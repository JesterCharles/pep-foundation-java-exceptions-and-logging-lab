# Exceptions and Logging lab: a resilient stock importer

A shop receives stock files with lines like `tea,3`, and real files arrive broken: counts that are not numbers, and lines without a comma.
You make the stock importer survive them, in five linked tasks in the package `importer`. When you finish, it:
- imports every good line;
- reports every bad line with its line number;
- closes the file on every path;
- tells different failures apart;
- logs what it did at the right levels.

You practise custom exceptions, wrapping causes, try-with-resources, catch ordering and logging.

The full learner instructions are in [`.evolv/instructions.md`](.evolv/instructions.md), which the lab platform shows as the lab's instructions.

## Layout

This repository follows the Exa coding-lab scaffold for Java.

| Path | What it is |
|---|---|
| `src/main/java/importer/ImportException.java` | Lab source |
| `src/main/java/importer/StockImporter.java` | Lab source |
| `src/test/java/importer/StockImporterTest.java` | Visible tests |
| `fixtures/stock.csv` | Sample data the lab reads |
| `.evolv/instructions.md` | Learner instructions |
| `.evolv/grade.yml` | Grading manifest, used by Run Tests and by the grader |
| `pom.xml` | Maven build: Java 17 and JUnit 5.11.3, matching the scaffold |
| `Dockerfile` | Image for local Run Tests; warms the Maven cache |
| `expected/test-summary.txt` | The result of a finished lab |

There are no held-out tests in this lab: every test is visible.

## Run the tests

On the platform, select **Run Tests**. From a terminal with JDK 17 or later and Apache Maven 3.9:

```
mvn -q test
```

All 5 tests fail until the `TODO`s are done. That is expected.
