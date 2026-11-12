# Exceptions and Logging lab: a resilient stock importer

A shop receives stock files with lines like `tea,3`, and real files arrive broken: counts that are not numbers, and lines without a comma.
You make the stock importer survive them, in five linked tasks in the package `importer`. When you finish, it:
- imports every good line;
- reports every bad line with its line number;
- closes the file on every path;
- tells different failures apart;
- logs what it did at the right levels.

You practise custom exceptions, wrapping causes, try-with-resources, catch ordering and logging.

## Quick start

The lab needs JDK 25 and Apache Maven 3.9 or later in the terminal. Troubleshooting shows how to check.

1. Open the lab project you were given in VS Code, then open its integrated terminal: **Terminal > New Terminal**.
2. Go into the project folder. Every command in this lab runs there:

   ```
   cd starter
   ```

3. Before the first test run, download the lab's libraries once. This needs the internet, and prints nothing when it works:

   ```
   mvn -q dependency:go-offline
   ```

4. Run the tests before you change anything:

   ```
   mvn -q -o test
   ```

   **All 5 tests fail at first. That is expected: the TODOs are not done yet.**
   The output ends with `Tests run: 5, Failures: 5`, and every failure line starts with its task number, such as `Task 1:`.
5. Work through the checklist below in order. After each task, run `mvn -q -o test` again and read the remaining `Task N:` lines.
6. **You are finished when `mvn -q -o test` prints nothing at all.**

```
OPEN project -> cd starter -> setup (once) -> RUN tests: 5 fail (expected)
  -> READ the "Task N:" lines -> EDIT the next TODO -> RUN tests again
  -> repeat for Tasks 1-5 -> STOP when the test run prints nothing
```

In words: open the project, run the tests, read which tasks fail, complete one task's TODO, run the tests again, and stop when they print nothing.

## Checklist

The files are in `starter/src/main/java/importer/`, and each task is marked with a `TODO` comment:

- [ ] Task 1: `ImportException.java`, both constructors (a checked exception of your own)
- [ ] Task 2: `StockImporter.java`, `parseCount` (throwing and wrapping exceptions)
- [ ] Task 3: `StockImporter.java`, `importLines` and `importFile` (try/catch in a loop, try-with-resources)
- [ ] Task 4: `StockImporter.java`, `outcome` (catch order)
- [ ] Task 5: `StockImporter.java`, `logSummary` and `logCrash` (logging levels)
- [ ] Optional, after the tests pass: read one stack trace on purpose

A task is done when no failure line starts with its number.
Edit only `ImportException.java` and `StockImporter.java`.
- `fixtures/stock.csv` is the broken sample file the importer must survive, exactly as shipped.
- The tests in `tests/importer/`, the build file `starter/pom.xml` and `expected/test-summary.txt` stay as they are.

## Task steps

Read `fixtures/stock.csv` first: lines 3 and 5 are the broken ones.
The tests feed the importer good and broken data, and check only the visible results.

### Task 1: the importer's own exception

1. Open `ImportException.java`. It extends `Exception`, so it is a checked exception.
   A broken stock file is an expected failure, and the compiler makes every caller plan for it. An unchecked `RuntimeException` is the choice for programming errors instead.
2. Complete both constructors: `super(message)` in the first, `super(message, cause)` in the second.
   The second keeps the low-level exception, so it appears in every later stack trace.
3. While it is wrong, you see lines such as `Task 1: the message constructor must hand the message to super(...)`.

### Task 2: parse one line, wrap what breaks

1. In `StockImporter.java`, find `parseCount(String line, int lineNumber)`.
2. A line without exactly one comma, or with nothing before the comma, fails with `new ImportException("line " + lineNumber + ": expected sku,count")`.
3. Parse the count with `Integer.parseInt`. Catch the `NumberFormatException` it throws for `"abc"`, and wrap it: `throw new ImportException("line " + lineNumber + ": count is not a number", cause)`.
   Never swallow the cause: when an import fails at 3 a.m., the stack trace's `Caused by:` line is what finds the bad file.

<details><summary>Hint</summary>

If the compiler reports `unreported exception ImportException; must be caught or declared to be thrown`, the checked exception is doing its job. Either catch it (Task 3) or declare `throws ImportException` (Task 2).

</details>

### Task 3: import everything that can be imported

1. Implement `importLines(List<String> lines, List<String> errors)` with one loop, and put the try/catch INSIDE the loop. That placement is the whole resilience story.
   - A good line adds its count to that sku's total. `getOrDefault`, from the maps module, helps here.
   - A bad line adds its exception message to `errors`, and the loop keeps going.
2. Implement `importFile(Path file, List<String> errors)` with try-with-resources: `try (BufferedReader reader = Files.newBufferedReader(file)) { ... }`.
   Read every line with `readLine()` until it returns null, collect the lines, and hand them to `importLines`.
   The reader now closes itself on every path, normal or exceptional. That is what the parentheses buy, with no `finally` block to forget.
3. Let `IOException` stay declared: a missing file is the caller's decision, not something the importer can repair. The tests check that a missing file still throws it.

### Task 4: tell the failures apart

1. Implement `outcome(ImportAction action)` with one try and three catch blocks, most specific first: `ImportException`, then `NumberFormatException`, then `Exception`. Return the exact texts the comment shows.
2. Try reversing the order. The compiler rejects a chain with the broad `Exception` first, because the narrower catches below it could never run. That compiler error is the ordering rule being enforced.

<details><summary>Hint</summary>

If you see `exception ImportException is never thrown in body of corresponding try statement`, the compiler is tracking checked exceptions. That catch block can only exist where some call in the try declares `throws ImportException`.

</details>

### Task 5: log it right

1. Implement `logSummary`:
   - one `logger.info("imported <okCount> skus from <fileName>")`, because routine success is INFO;
   - then one `logger.warning(...)` per collected error, because a bad line the importer survived is a warning, not a crash.
2. Implement `logCrash`: `logger.log(Level.SEVERE, "import failed", crash)`.
   This one call carries the message AND the exception object, so the log keeps the whole stack trace. Logging `crash.toString()` would throw the trace away.
3. Logs are for operators. Never log passwords, tokens or whole customer records; here the summary carries only counts, file names and line numbers.

### Optional: read one stack trace

Break your own importer once, on purpose.
1. From `starter`, run the program with the two commands under **Run the program** below, and watch it import `stock.csv`.
2. In `main`, change the fixture path to `stock-typo.csv`, and run the same two commands again.
3. Read the trace from the top down:
   - the exception type, `NoSuchFileException`;
   - its message, the path;
   - the first line that names YOUR code, `StockImporter.importFile`, where your code opened the file;
   - the line below it, `StockImporter.main`, which passed in the wrong path.
4. That first-own-line habit is how you will find every later failure in this course. Undo the change afterwards.

### About the test results

A passing run shows the importer behaves correctly.
It does not check which Java constructs your code uses, so compare your code with each task's steps yourself.

## Using an AI assistant

If you use an AI assistant, ask it to explain, not to answer.
"Explain this Caused by: chain" teaches you stack traces; "write the importer for me" does not.
Check any explanation with a test run before you trust it.

## Run the program

The importer runs on the broken fixture file. From `starter`:

```
mvn -q -o compile
java -cp target/classes importer.StockImporter
```

`expected/test-summary.txt` shows the finished result, including what the program prints for the fixture file.

## Troubleshooting

- **`there is no POM in this directory`.** The terminal is not in `starter`. From the lab folder, run `cd starter` and try again.
- **`NoSuchFileException` naming `stock.csv` (when you did not change the path).** The program and tests read `../fixtures/stock.csv` relative to `starter`, so run them from `starter`. Keep the `Path.of("..", "fixtures", ...)` form. If a run started from your editor cannot find the file, use the terminal command instead.
- **`COMPILATION ERROR`.** Your code does not compile yet. Maven names the file, then `[line,column]`, then the problem. Fix that line and run the tests again.
- **`mvn: command not found`, or `'mvn' is not recognized`.** Maven's `bin` folder is not on your `PATH`. Add it, then open a new terminal.
- **`mvn -v` shows a Java version other than 25, or you see `release version 25 not supported`.** Maven uses the JDK named by `JAVA_HOME` when it is set, otherwise the `java` on your `PATH`. Set `JAVA_HOME` to your JDK 25 folder, or unset it, then open a new terminal. Check with `java -version` and `mvn -v`.
- **`Cannot access central in offline mode`, or a missing artifact.** The setup step has not finished. Run `mvn -q dependency:go-offline` once while online.

## Cleanup (optional)

Maven writes only the `starter/target` folder, and Git already ignores it.
To delete it, run `rm -rf target` from `starter`. In Windows PowerShell, run `Remove-Item -Recurse -Force target`.
