package importer;

/**
 * Task 1: the importer's own exception.
 * It extends Exception, so it is CHECKED: a method that throws it must say so
 * with `throws ImportException`, and every caller must handle it or pass it
 * on. That is the right choice here - a broken stock file is an expected
 * failure the caller must plan for, not a programming error.
 */
public class ImportException extends Exception {

    // Task 1: make both constructors hand their arguments to the superclass.
    // super(message) stores the message that getMessage() returns;
    // super(message, cause) also keeps the low-level exception, so Task 2 can
    // wrap a NumberFormatException without losing it.

    public ImportException(String message) {
        // TODO Task 1: call super(message)
    }

    public ImportException(String message, Throwable cause) {
        // TODO Task 1: call super(message, cause)
    }
}
