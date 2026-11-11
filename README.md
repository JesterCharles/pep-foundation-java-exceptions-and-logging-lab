# Exceptions and Logging lab: a resilient stock importer

## Goal

Make a stock-file importer survive real-world data, in five linked tasks in the package `importer`.
The shop receives files with lines like `tea,3` - and real files arrive broken: counts that are not numbers, lines without a comma.
Your importer imports every good line, reports every bad one with its line number, closes the file on every path, tells different failures apart, and logs what it did on the right levels.
You practise custom exceptions, wrapping causes, try-with-resources, catch ordering and logging on one coherent failure story.
You build and test it with the real JDK and Maven on your own computer.

## Files

**Edit exactly two files.** Every `TODO` comment marks its task; the test failure lines carry the same task numbers:

- `starter/src/main/java/importer/ImportException.java` - Task 1: the importer's own checked exception
- `starter/src/main/java/importer/StockImporter.java` - Tasks 2-5: parsing, resilience, failure classification and logging

**Do not change anything else.** In particular:

- `fixtures/stock.csv` - the broken sample file the importer must survive, exactly as shipped
- `tests/importer/StockImporterTest.java` - the public tests your importer must pass
- `starter/pom.xml` - the build is prewired
- `expected/test-summary.txt` - the finished-state reference

## Prerequisites

You need JDK 25 and Apache Maven 3.9 or later, the same setup as the earlier Java labs.
Check both in a new terminal:

```
java -version
mvn -v
```

- `java -version` must report version 25.
- `mvn -v` must report Apache Maven 3.9 or later, and its `Java version:` line must also say 25.

This lab works the same on macOS, Linux and Windows.
On Windows, use PowerShell or Command Prompt; the commands are the same.
No Docker or other container tool is needed.

## Setup (once, needs the internet)

Open a terminal in the lab's root folder - the one that holds this README, `starter/` and `tests/` - and change into `starter` with `cd starter`; every command in this lab runs there. Then run:

```
mvn -q dependency:go-offline
```

It prints nothing when it succeeds.
If you finished an earlier Java lab on this machine, it finds everything already downloaded.
After this step, everything else in the lab runs offline.

## Tasks

Open the lab's root folder in your IDE or editor; it holds one Maven project, `starter/pom.xml`, and if your editor asks, open or import that file as a Maven project, then open `starter/src/main/java/importer/ImportException.java` and `StockImporter.java`. Whatever your editor shows, the test command below, run in a terminal in `starter`, is the result that counts.
The broken sample file the importer must survive is `fixtures/stock.csv` - read it first; lines 3 and 5 are the troublemakers.
The tests feed the importer good and broken data and check only the visible results.

After each task, run the one test command from the `starter` folder:

```
mvn -q -o test
```

Every failure line starts with its task number, so you can work through the tasks in order.

### Task 1: the importer's own exception

1. Open `ImportException.java`. It extends `Exception`, so it is checked: a broken stock file is an expected failure every caller must plan for - the compiler enforces that plan. An unchecked `RuntimeException` would be the choice for programming errors instead.
2. Complete both constructors: `super(message)` in the first, `super(message, cause)` in the second. The second keeps the low-level exception alive for every later stack trace.
3. While it is wrong, you see lines such as `Task 1: the message constructor must hand the message to super(...)`.
4. Done when no line starting with `Task 1` appears.

### Task 2: parse one line, wrap what breaks

1. In `StockImporter.java`, find `parseCount(String line, int lineNumber)`.
2. A line without exactly one comma, or with nothing before it, fails with `new ImportException("line " + lineNumber + ": expected sku,count")`.
3. Parse the count with `Integer.parseInt`. Catch the `NumberFormatException` it throws for `"abc"` and wrap it: `throw new ImportException("line " + lineNumber + ": count is not a number", cause)`.
   Never swallow the cause - when this import fails at 3 a.m., the stack trace's `Caused by:` line is what finds the bad file.
4. Done when no line starting with `Task 2` appears.

### Task 3: import everything that can be imported

1. Implement `importLines(List<String> lines, List<String> errors)`: one loop, and the try/catch INSIDE the loop - that placement is the whole resilience story. A good line adds its count to the sku's total (`getOrDefault` from the maps module helps); a bad line adds its exception message to `errors` and the loop keeps going.
2. Implement `importFile(Path file, List<String> errors)` with try-with-resources:
   `try (BufferedReader reader = Files.newBufferedReader(file)) { ... }`.
   Read every line with `readLine()` until it returns null, collect the lines, delegate to `importLines`. The reader now closes itself on every path, normal or exceptional - that is what the parentheses buy, with no `finally` to forget.
3. Let `IOException` stay declared: a missing file is the caller's decision, not something the importer can repair. The tests check that a missing file still throws it.
4. Done when no line starting with `Task 3` appears.

### Task 4: tell the failures apart

1. Implement `outcome(ImportAction action)` with one try and three catch blocks, most specific first: `ImportException`, then `NumberFormatException`, then `Exception`, returning the exact texts the comment shows.
2. Try reversing the order: the compiler rejects a chain where the broad `Exception` comes first, because the narrower catches below it could never run. That compiler error is the ordering rule enforced.
3. Done when no line starting with `Task 4` appears.

### Task 5: log it right

1. Implement `logSummary`: one `logger.info("imported <okCount> skus from <fileName>")` - routine success is INFO - then one `logger.warning(...)` per collected error: a survived bad line is a warning, not a crash.
2. Implement `logCrash`: `logger.log(Level.SEVERE, "import failed", crash)` - one call that carries the message AND the exception object, so the log keeps the whole stack trace. Logging `crash.toString()` would throw the trace away.
3. Logs are for operators: never log passwords, tokens or whole customer records - here the summary carries only counts, file names and line numbers.
4. Done when no line starting with `Task 5` appears and the whole run prints nothing.

### Read one stack trace

Break your own importer once on purpose. First, from the `starter` folder, run the program with the two commands at the end of the Test section and see it import `stock.csv`; then, in `main`, change the fixture path to `stock-typo.csv` and run the same two commands again.
Read the trace top-down: the exception type (`NoSuchFileException`), its message (the path), and the first line that names YOUR file (`StockImporter.importFile`, where your code opened the file), then the line below it (`StockImporter.main`, which passed in the wrong path).
That first-own-line habit is how every later failure in this course gets found. Undo the change afterwards.

## Using an AI assistant

If you use an AI assistant in this lab, ask it to explain, not to answer: "explain this Caused by: chain" teaches you stack traces; "write the importer for me" does not.
Verify any explanation against a test run before you trust it.

## Test

The one test command, run from the `starter` folder, is:

```
mvn -q -o test
```

- Before you start, all five tests fail, and Maven ends with `Tests run: 5, Failures: 5`.
- Each test lists what behavior was wrong. A passing run shows the importer behaves correctly; it does not check which Java constructs your code uses, so compare your code with each task's steps yourself.
- If your code does not compile, Maven prints `COMPILATION ERROR` with the file, then `[line,column]`, then the compiler's message. Fix that line and run the test again.

You can also run the importer itself on the broken fixture file:

```
mvn -q -o compile
java -cp target/classes importer.StockImporter
```

## Expected output

When every task is complete, `mvn -q -o test` prints nothing and exits with status 0.
`expected/test-summary.txt` shows the full expected result, including what the program prints for the fixture file.

## Offline note

Only the setup step uses the network.
The test command uses `-o` (offline), so it never downloads anything.
If it reports that an artifact is missing from the local repository, run the setup step once more while online.

## Cleanup

The lab writes only the `starter/target` folder (compiled classes and test reports). Delete it from the `starter` folder:

- macOS and Linux: `rm -rf target`
- Windows PowerShell: `Remove-Item -Recurse -Force target`

The JUnit and plugin downloads stay in your local Maven repository (`~/.m2/repository` by default) so later Java labs can reuse them.

`starter/.gitignore` already keeps Maven's `starter/target` folder out of Git. Your editor and operating system may also create local files in the lab's root folder, such as `.idea/`, `.vscode/` or `.DS_Store`; they are not part of the lab, so if `git status` lists any of them, add them to a `.gitignore` file in the lab's root folder instead of committing them.

## Troubleshooting

- **`mvn: command not found`, or `'mvn' is not recognized`.** Maven's `bin` folder is not on your `PATH`. Add it, then open a new terminal.
- **`mvn -v` shows a Java version other than 25.** Maven uses the JDK named by `JAVA_HOME` when it is set, otherwise the `java` on your `PATH`. Set `JAVA_HOME` to your JDK 25 folder, or unset it, then open a new terminal.
- **`release version 25 not supported`.** Maven is running on an older JDK. Fix it as in the previous item.
- **`Cannot access central in offline mode`, or a missing artifact.** The setup step has not completed. Run `mvn -q dependency:go-offline` once while online.
- **`exception ImportException is never thrown in body of corresponding try statement`.** The compiler tracks checked exceptions: that catch block can only exist where some call in the try actually declares `throws ImportException`.
- **`unreported exception ImportException; must be caught or declared to be thrown`.** That is the checked contract working: either catch it (Task 3) or add `throws ImportException` (Task 2).
- **`NoSuchFileException` naming `stock.csv`, or Task 3 reads the wrong file.** The tests run from `starter/`, and the fixture path is `../fixtures/stock.csv` relative to it; keep the `Path.of("..", "fixtures", ...)` form. If a run started from your editor cannot find the fixture, its working directory is not `starter`: set that run's working directory to `starter`, or use the terminal command.
