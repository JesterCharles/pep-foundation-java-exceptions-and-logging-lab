# Exceptions and Logging lab: a resilient stock importer

A shop receives stock files with lines like `tea,3`, and real files arrive broken: counts that are not numbers, and lines without a comma.
You make the stock importer survive them, in five linked tasks in the package `exercise`. When you finish, it:
- imports every good line;
- reports every bad line with its line number;
- closes the file on every path;
- tells different failures apart;
- logs what it did at the right levels.

You practise custom exceptions, wrapping causes, try-with-resources, catch ordering and logging.

## Quick start

1. Open the lab project. The files you edit are in `src/main/java/exercise/`; the checklist below names them.
2. Select **Run Tests** before you change anything.

   **All 5 visible tests fail at first. That is expected: the TODOs are not done yet.**
   Every failure message starts with its task number, such as `Task 1:`.
3. Work through the checklist below in order. After each task, select **Run Tests** again and read the remaining `Task N:` messages.
4. **You are finished when all 5 visible tests pass.** Then select **Submit**.
   Submitting also runs held-out tests you can't see.

```
OPEN project -> RUN TESTS: 5 visible tests fail (expected)
  -> READ the "Task N:" messages -> EDIT the next TODO -> RUN TESTS again
  -> repeat for Tasks 1-5 -> SUBMIT when all 5 visible tests pass
```

In words: open the project, run the tests, read which tasks fail, complete one task's TODO, run the tests again, and submit when they all pass.

## Checklist

The files are in `src/main/java/exercise/`, and each task is marked with a `TODO` comment:

- [ ] Task 1: `ImportException.java`, both constructors (a checked exception of your own)
- [ ] Task 2: `StockImporter.java`, `parseCount` (throwing and wrapping exceptions)
- [ ] Task 3: `StockImporter.java`, `importLines` and `importFile` (try/catch in a loop, try-with-resources)
- [ ] Task 4: `StockImporter.java`, `outcome` (catch order)
- [ ] Task 5: `StockImporter.java`, `logSummary` and `logCrash` (logging levels)
- [ ] Optional, after the tests pass: read one stack trace on purpose

A task is done when no failure message starts with its number.
Edit only `ImportException.java` and `StockImporter.java`.
- `fixtures/stock.csv` is the broken sample file the importer must survive, exactly as shipped.
- The tests in `src/test/java/exercise/` are restored to their original state before your work is graded, so changes to them do not count. Leave the build file `pom.xml` as it is.

## Task steps

Read `fixtures/stock.csv` first: lines 3 and 5 are the broken ones.
The tests feed the importer good and broken data, and check only the visible results.
Run Tests runs the visible tests. Submit also runs held-out tests you can't see: they try more inputs and edge cases, so make each method do everything its comment describes, not only what the visible tests try.

### Task 1: the importer's own exception

1. Open `ImportException.java`. It extends `Exception`, so it is a checked exception.
   A broken stock file is an expected failure, and the compiler makes every caller plan for it. An unchecked `RuntimeException` is the choice for programming errors instead.
2. Complete both constructors: `super(message)` in the first, `super(message, cause)` in the second.
   The second keeps the low-level exception, so it appears in every later stack trace.
3. While it is wrong, you see messages such as `Task 1: the message constructor must hand the message to super(...)`.

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
1. Run the program with the two commands under **Run the program** below, and watch it import `stock.csv`.
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

## Run the program (optional)

The importer runs on the broken fixture file. In a terminal, from the project folder:

```
mvn -q compile
java -cp target/classes exercise.StockImporter
```

When every task is done, it prints:

```
tea=5 mug=9 pen=14
errors=2
  line 3: count is not a number
  line 5: expected sku,count
```

then the three log records from `logSummary`. Each record is two lines: a timestamp line naming `exercise.StockImporter logSummary`, then the record itself (your timestamps will differ):

```
INFO: imported 3 skus from stock.csv
WARNING: line 3: count is not a number
WARNING: line 5: expected sku,count
```

and finally:

```
outcome ok: ok
outcome bad: import error: line 1: count is not a number
```

## Troubleshooting

- **`NoSuchFileException` naming `stock.csv` (when you did not change the path).** The program and tests read `fixtures/stock.csv` relative to the project folder, so run them from there. Keep the `Path.of("fixtures", ...)` form. If a run started from your editor cannot find the file, use **Run Tests** or the terminal command instead.
- **`COMPILATION ERROR`.** Your code does not compile yet. Maven names the file, then `[line,column]`, then the problem. Fix that line and run the tests again.
