package importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Exceptions and Logging lab: a stock-file importer that survives bad data.
 * The shop receives stock files with lines like "tea,3". Real files arrive
 * broken - a count that is not a number, a line without a comma - and the
 * importer must import every good line, report every bad one, close the file
 * whatever happens, and log what it did. Complete the tasks marked TODO.
 * The public tests feed it good and broken data and check only the results.
 * The README lists the steps for every task. main is complete: it imports
 * fixtures/stock.csv and prints what happened.
 */
public class StockImporter {

    /** An import step that may fail with any exception; used by Task 4. */
    public interface ImportAction {
        void run() throws Exception;
    }

    // Task 2 (wrap, do not lose): parse one "sku,count" line into its count.
    // A line without exactly one comma, or with an empty sku, fails with
    //   new ImportException("line " + lineNumber + ": expected sku,count")
    // A count that is not a number fails with
    //   new ImportException("line " + lineNumber + ": count is not a number", cause)
    // where cause is the NumberFormatException you caught - wrap it, never
    // swallow it: whoever reads the stack trace later needs the root cause.
    static int parseCount(String line, int lineNumber) throws ImportException {
        return -1; // TODO: split at the comma, parse, wrap the NumberFormatException
    }

    // Task 3a (resilience): import every line. Good lines add their count to
    // the sku's total in the map. A line that parseCount rejects must not stop
    // the import: catch the ImportException and add its message to errors.
    static HashMap<String, Integer> importLines(List<String> lines, List<String> errors) {
        return null; // TODO: one loop, try/catch inside the loop, keep going
    }

    // Task 3b (try-with-resources): open the file with
    // Files.newBufferedReader(file) INSIDE try-with-resources parentheses,
    // read every line, and delegate to importLines. The reader then closes
    // itself on every path - normal return or exception - before the method
    // ends. IOException stays checked and declared: a missing file is the
    // caller's problem to handle.
    static HashMap<String, Integer> importFile(Path file, List<String> errors) throws IOException {
        return null; // TODO: try (BufferedReader reader = ...) { collect lines, delegate }
    }

    // Task 4 (catch order): run the action and report what happened:
    //   it finishes            -> "ok"
    //   ImportException        -> "import error: " + its message
    //   NumberFormatException  -> "bad number: " + its message
    //   any other Exception    -> "unexpected: " + its message
    // Order the catch blocks from most to least specific - the compiler
    // rejects a chain where a broader catch shadows a narrower one below it.
    static String outcome(ImportAction action) {
        return "?"; // TODO: try/catch chain, most specific first
    }

    // Task 5a (logging): log the import summary on the right levels:
    // one INFO line exactly "imported <okCount> skus from <fileName>",
    // then one WARNING per entry in errors, with the entry as the message.
    static void logSummary(Logger logger, String fileName, int okCount, List<String> errors) {
        // TODO: logger.info(...), then logger.warning(...) per error
    }

    // Task 5b (logging a crash): log "import failed" on SEVERE together with
    // the exception itself, so the log keeps the whole stack trace:
    // logger.log(Level.SEVERE, message, thrown). Log the exception object,
    // not its toString - and never log secrets or whole customer records.
    static void logCrash(Logger logger, Exception crash) {
        // TODO: one call that carries both the message and the exception
    }

    public static void main(String[] args) throws IOException {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();
        HashMap<String, Integer> stock = importFile(Path.of("..", "fixtures", "stock.csv"), errors);
        if (stock == null) {
            System.out.println("importFile is not implemented yet");
            return;
        }
        System.out.println("tea=" + stock.get("tea") + " mug=" + stock.get("mug") + " pen=" + stock.get("pen"));
        System.out.println("errors=" + errors.size());
        for (String error : errors) {
            System.out.println("  " + error);
        }
        Logger logger = Logger.getLogger("importer.demo");
        logSummary(logger, "stock.csv", stock.size(), errors);
        System.out.println("outcome ok: " + outcome(() -> parseCount("tea,3", 1)));
        System.out.println("outcome bad: " + outcome(() -> parseCount("tea,x", 1)));
    }
}
