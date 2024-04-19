package xjs.data.serialization.token;

import xjs.data.StringType;

/**
 * Represents a single string token: either a single-quoted,
 * double-quoted, triple-quoted, or generated string,
 *
 * <p>For example, the following text:
 *
 * <pre>
 *   "123"
 * </pre>
 *
 * <p>Counts as the following token:
 *
 * <pre>
 *   [ double('123') ]
 * </pre>
 */
public class StringToken extends ParsedToken {
    private final StringType stringType;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start  The inclusive start index of this token.
     * @param end    The exclusive end index of this token.
     * @param line   The inclusive line number of this token.
     * @param offset The column of the start index.
     * @param type   The type of string represented by the token.
     * @param parsed The un-escaped, parsed text.
     */
    public StringToken(
            final int start, final int end, final int line, final int offset,
            final StringType type, final String parsed) {
        super(start, end, line, offset, TokenType.STRING, parsed);
        this.stringType = type;
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param lastLine The inclusive line number at the end of this token.
     * @param offset   The column of the start index.
     * @param type     The type of string represented by the token.
     * @param parsed   The un-escaped, parsed text.
     */
    public StringToken(
            final int start, final int end, final int line, final int lastLine,
            final int offset, final StringType type, final String parsed) {
        super(start, end, line, lastLine, offset, TokenType.STRING, parsed);
        this.stringType = type;
    }

    /**
     * Constructs a new String token with effectively no scope.
     *
     * @param type   The style of string written.
     * @param parsed The parsed text of the string.
     */
    public StringToken(final StringType type, final String parsed) {
        super(TokenType.STRING, parsed);
        this.stringType = type;
    }

    @Override
    public StringType stringType() {
        return this.stringType;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof StringToken) {
            final StringToken st = (StringToken) other;
            return this.stringType == st.stringType
                && this.parsed.equals(st.parsed)
                && this.spanEquals(st);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.type + "(start:" + this.start + ",end:" + this.end + ",line:"
            + this.line + ",lastLine:" + lastLine + ",offset:" + this.offset
            + ",stringType:'" + this.stringType + ",parsed:'" + this.parsed + "')";
    }
}
