package exercise.hidden;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import exercise.ImportException;
import exercise.StockImporter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Held-out tests: graded on submit, never shown to the learner. Edge cases for Tasks 2-5. */
@TestMethodOrder(MethodOrderer.MethodName.class)
class HeldOutTests {

    @Test
    void task2ParseCountEdgeCases() throws Exception {
        assertEquals(14, StockImporter.parseCount("pen,14", 4), "Task 2: parseCount(\"pen,14\", 4)");
        ImportException notANumber = assertThrows(ImportException.class,
                () -> StockImporter.parseCount("notebook,abc", 3),
                "Task 2: a count that is not a number must fail with ImportException");
        assertEquals("line 3: count is not a number", notANumber.getMessage(),
                "Task 2: the message names the line number");
    }

    @Test
    void task3ResilienceEdgeCases() throws Exception {
        List<String> lines = List.of("tea,3", "mug,9", "notebook,abc", "pen,14", "candle", "tea,2");
        HashMap<String, Integer> stock = StockImporter.importLines(lines, new ArrayList<>());
        assertEquals(3, stock.size(), "Task 3: only good skus end up in the map");
        ArrayList<String> fileErrors = new ArrayList<>();
        StockImporter.importFile(Path.of("fixtures", "stock.csv"), fileErrors);
        assertEquals(2, fileErrors.size(), "Task 3: the fixture file contains exactly two bad lines");
        assertThrows(java.io.IOException.class,
                () -> StockImporter.importFile(Path.of("fixtures", "no-such-file.csv"), new ArrayList<>()),
                "Task 3: a missing file stays a checked IOException for the caller");
    }

    @Test
    void task4BroadestCatchLast() {
        assertEquals("unexpected: boom",
                StockImporter.outcome(() -> {
                    throw new IllegalStateException("boom");
                }),
                "Task 4: anything else lands in the broadest catch, last");
    }

    @Test
    void task5CrashKeepsTheException() {
        ArrayList<LogRecord> records = new ArrayList<>();
        Logger logger = Logger.getLogger("importer.hidden." + System.nanoTime());
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
        IllegalStateException crash = new IllegalStateException("disk gone");
        StockImporter.logCrash(logger, crash);
        assertEquals(1, records.size(), "Task 5: logCrash logs exactly one record");
        assertAll(
                () -> assertEquals("import failed", records.get(0).getMessage(),
                        "Task 5: the crash message"),
                () -> assertEquals(crash, records.get(0).getThrown(),
                        "Task 5: log the exception object itself, so the stack trace survives"));
    }
}
