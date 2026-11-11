package importer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Public tests for the Exceptions and Logging lab. Each test feeds the
 * importer good and broken data and checks only the visible results:
 * return values, thrown exceptions, collected errors and log records.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class StockImporterTest {

    @Test
    void task1Exception() {
        ImportException plain = new ImportException("line 3: expected sku,count");
        NumberFormatException cause = new NumberFormatException("For input string: \"abc\"");
        ImportException wrapped = new ImportException("line 3: count is not a number", cause);
        assertAll(
                () -> assertEquals("line 3: expected sku,count", plain.getMessage(),
                        "Task 1: the message constructor must hand the message to super(...)"),
                () -> assertNull(plain.getCause(), "Task 1: the message constructor has no cause"),
                () -> assertEquals("line 3: count is not a number", wrapped.getMessage(),
                        "Task 1: the cause constructor must keep the message"),
                () -> assertEquals(cause, wrapped.getCause(),
                        "Task 1: the cause constructor must keep the cause for the stack trace"));
    }

    @Test
    void task2ParseCount() throws Exception {
        assertEquals(3, StockImporter.parseCount("tea,3", 1), "Task 2: parseCount(\"tea,3\", 1)");
        assertEquals(14, StockImporter.parseCount("pen,14", 4), "Task 2: parseCount(\"pen,14\", 4)");
        ImportException noComma = assertThrows(ImportException.class,
                () -> StockImporter.parseCount("candle", 5),
                "Task 2: a line without a comma must fail with ImportException");
        assertEquals("line 5: expected sku,count", noComma.getMessage(),
                "Task 2: the message names the line number");
        ImportException notANumber = assertThrows(ImportException.class,
                () -> StockImporter.parseCount("notebook,abc", 3),
                "Task 2: a count that is not a number must fail with ImportException");
        assertAll(
                () -> assertEquals("line 3: count is not a number", notANumber.getMessage(),
                        "Task 2: the message names the line number"),
                () -> assertInstanceOf(NumberFormatException.class, notANumber.getCause(),
                        "Task 2: wrap the NumberFormatException as the cause; never lose it"));
    }

    @Test
    void task3Resilience() throws Exception {
        List<String> lines = List.of("tea,3", "mug,9", "notebook,abc", "pen,14", "candle", "tea,2");
        ArrayList<String> errors = new ArrayList<>();
        HashMap<String, Integer> stock = StockImporter.importLines(lines, errors);
        assertNotNull(stock, "Task 3: importLines must return a map, not null");
        assertAll(
                () -> assertEquals(5, stock.get("tea"), "Task 3: two tea lines add up to 5"),
                () -> assertEquals(9, stock.get("mug"), "Task 3: mug is imported"),
                () -> assertEquals(14, stock.get("pen"),
                        "Task 3: a bad line must not stop the lines after it"),
                () -> assertEquals(3, stock.size(), "Task 3: only good skus end up in the map"),
                () -> assertEquals(List.of("line 3: count is not a number", "line 5: expected sku,count"),
                        errors, "Task 3: every bad line is reported with its message"));
        ArrayList<String> fileErrors = new ArrayList<>();
        HashMap<String, Integer> fromFile =
                StockImporter.importFile(Path.of("..", "fixtures", "stock.csv"), fileErrors);
        assertNotNull(fromFile, "Task 3: importFile must return a map, not null");
        assertAll(
                () -> assertEquals(stock, fromFile,
                        "Task 3: importing the fixture file gives the same stock"),
                () -> assertEquals(2, fileErrors.size(),
                        "Task 3: the fixture file contains exactly two bad lines"));
        assertThrows(java.io.IOException.class,
                () -> StockImporter.importFile(Path.of("..", "fixtures", "no-such-file.csv"), new ArrayList<>()),
                "Task 3: a missing file stays a checked IOException for the caller");
    }

    @Test
    void task4Outcome() {
        assertAll(
                () -> assertEquals("ok", StockImporter.outcome(() -> { }),
                        "Task 4: an action that finishes reports ok"),
                () -> assertEquals("import error: line 7: expected sku,count",
                        StockImporter.outcome(() -> {
                            throw new ImportException("line 7: expected sku,count");
                        }),
                        "Task 4: ImportException is the most specific catch"),
                () -> assertEquals("bad number: For input string: \"x\"",
                        StockImporter.outcome(() -> Integer.parseInt("x")),
                        "Task 4: NumberFormatException has its own catch"),
                () -> assertEquals("unexpected: boom",
                        StockImporter.outcome(() -> {
                            throw new IllegalStateException("boom");
                        }),
                        "Task 4: anything else lands in the broadest catch, last"));
    }

    @Test
    void task5Logging() {
        ArrayList<LogRecord> records = new ArrayList<>();
        Logger logger = Logger.getLogger("importer.test." + System.nanoTime());
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        StockImporter.logSummary(logger, "stock.csv", 3, List.of("line 3: count is not a number"));
        assertEquals(2, records.size(), "Task 5: one INFO summary plus one WARNING per error");
        assertAll(
                () -> assertEquals(Level.INFO, records.get(0).getLevel(),
                        "Task 5: the summary is routine, so it is INFO"),
                () -> assertEquals("imported 3 skus from stock.csv", records.get(0).getMessage(),
                        "Task 5: the summary message"),
                () -> assertEquals(Level.WARNING, records.get(1).getLevel(),
                        "Task 5: a bad line is a WARNING, not an error that stopped the import"),
                () -> assertEquals("line 3: count is not a number", records.get(1).getMessage(),
                        "Task 5: the warning carries the error text"));
        records.clear();
        IllegalStateException crash = new IllegalStateException("disk gone");
        StockImporter.logCrash(logger, crash);
        assertEquals(1, records.size(), "Task 5: logCrash logs exactly one record");
        assertAll(
                () -> assertEquals(Level.SEVERE, records.get(0).getLevel(),
                        "Task 5: a crash is SEVERE"),
                () -> assertEquals("import failed", records.get(0).getMessage(),
                        "Task 5: the crash message"),
                () -> assertEquals(crash, records.get(0).getThrown(),
                        "Task 5: log the exception object itself, so the stack trace survives"));
    }
}
