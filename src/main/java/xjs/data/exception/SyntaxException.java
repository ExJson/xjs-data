package xjs.data.exception;

/**
 * Represents any miscellaneous syntax exception which can occur when parsing JSON, DJS,
 * or some other data format.
 */
public class SyntaxException extends RuntimeException {

    private final int line;
    private final int column;

    /**
     * Constructs a new syntax exception indicating the line and column numbers, as well
     * as a message to display which describes the error.
     *
     * @param msg    A description of the error.
     * @param line   The line number, <b>starting at line 1</b>.
     * @param column The column number, <b>starting at column 1</b>.
     */
    public SyntaxException(final String msg, final int line, final int column) {
        super(msg + " at " + line + ":" + column);
        this.line = line;
        this.column = column;
    }

    /**
     * Generates a syntax exception indicating that the given token is illegal in the
     * current file format.
     *
     * @param text   The text of the token found.
     * @param line   The line number, <b>starting at line 1</b>.
     * @param column The column number, <b>starting at column 1</b>.
     * @return A new syntax exception reporting this error.
     */
    public static SyntaxException illegalToken(final String text, final int line, final int column) {
        return new SyntaxException("Illegal token: '" + text + "'", line, column);
    }

    /**
     * Generates a syntax exception indicating that a specific character was expected,
     * but not found.
     *
     * @param expected The missing character.
     * @param line     The line number, <b>starting at line 1</b>.
     * @param column   The column number, <b>starting at column 1</b>.
     * @return A new syntax exception reporting this error.
     */
    public static SyntaxException expected(final char expected, final int line, final int column) {
        return new SyntaxException("Expected '" + expected + "'", line, column);
    }

    /**
     * Generates a syntax exception indicating that some stream of characters or type
     * of token was expected, but not found.
     *
     * @param expected A description of the missing tokens.
     * @param line     The line number, <b>starting at line 1</b>.
     * @param column   The column number, <b>starting at column 1</b>.
     * @return A new syntax exception reporting this error.
     */
    public static SyntaxException expected(final String expected, final int line, final int column) {
        return new SyntaxException("Expected " + expected, line, column);
    }

    /**
     * Generates a syntax exception indicating that a specific character was found, but
     * not expected.
     *
     * @param unexpected The unexpected character.
     * @param line       The line number, <b>starting at line 1</b>.
     * @param column     The column number, <b>starting at column 1</b>.
     * @return A new syntax exception reporting this error.
     */
    public static SyntaxException unexpected(final char unexpected, final int line, final int column) {
        return new SyntaxException("Unexpected '" + unexpected + "'", line, column);
    }

    /**
     * Generates a syntax exception indicating that some stream of characters or type of
     * token was found, but not expected.
     *
     * @param unexpected A description of the unexpected tokens.
     * @param line       The line number, <b>starting at line 1</b>.
     * @param column     The column number, <b>starting at column 1</b>.
     * @return A new syntax exception reporting this error.
     */
    public static SyntaxException unexpected(final String unexpected, final int line, final int column) {
        return new SyntaxException("Unexpected " + unexpected, line, column);
    }

    /**
     * Indicates the line number at which the error occurred, starting at index 1.
     *
     * @return The line number.
     */
    public int getLine() {
        return this.line;
    }

    /**
     * Indicates the column number at which the error occurred, starting at index 1.
     *
     * @return The column number.
     */
    public int getColumn() {
        return this.column;
    }
}
