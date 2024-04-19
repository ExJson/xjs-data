package xjs.data.serialization.token;

import xjs.data.comments.CommentStyle;
import xjs.data.StringType;
import xjs.data.serialization.Span;

/**
 * Represents a single token: either a character, group of characters,
 * or any other sequence of tokens.
 */
public class Token extends Span<TokenType> {

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public Token(final int start, final int end, final int line, final int offset, final TokenType type) {
        super(start, end, line, offset, type);
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param lastLine The inclusive end line number of this token.
     * @param offset   The column of the start index.
     * @param type     The type of token.
     */
    public Token(
            final int start, final int end, final int line, final int lastLine, final int offset, final TokenType type) {
        super(start, end, line, lastLine, offset, type);
    }

    /**
     * Constructs a new superficial token with effectively no scope.
     */
    protected Token(final TokenType type) {
        super(type);
    }

    /**
     * Retrieves the parsed text of this token, if applicable. Else,
     * returns a subsequence of the given reference.
     *
     * @param reference A reference to the original parent text body.
     * @return A string representing the parsed text of this token.
     */
    public String parsed(final CharSequence reference) {
        return this.textOf(reference);
    }

    /**
     * Retrieves the parsed text of this token, if applicable. Else,
     * throws an exception.
     *
     * @return A string representing the parsed text of this token.
     * @throws UnsupportedOperationException If the token is not parsed.
     */
    public String parsed() {
        throw new UnsupportedOperationException("not parsed");
    }

    /**
     * Builds a new token with identical scope and type, inserting an up-front
     * text representation.
     *
     * @param reference A reference to the original text body.
     * @return A new pre-parsed version of this token.
     */
    public ParsedToken intoParsed(final CharSequence reference) {
        if (this instanceof ParsedToken) {
            return (ParsedToken) this;
        }
        return new ParsedToken(
            this.start, this.end, this.line, this.lastLine,
            this.offset, this.type, this.parsed(reference));
    }

    /**
     * Determines whether this token represents the given symbol.
     *
     * @param symbol The expected symbol.
     * @return true, if this token matches the symbol.
     */
    public boolean isSymbol(final char symbol) {
        return false;
    }

    /**
     * Indicates whether this token <em>represents</em> the given text.
     *
     * @param reference A reference to the original parent text body.
     * @param text      The text being compared against.
     * @return true, if this token matches the text.
     */
    public boolean isText(final CharSequence reference, final String text) {
        return reference.toString().regionMatches(this.start, text, 0, this.length());
    }

    /**
     * Indicates whether this token <em>represents</em> the given text.
     *
     * @return true, if this token matches the text.
     * @throws UnsupportedOperationException if the text is not parsed.
     */
    public boolean isText(final String text) {
        throw new UnsupportedOperationException("not parsed");
    }

    /**
     * Retrieves the type of string represented by this token, if applicable.
     * Else, returns {@link StringType#NONE}.
     *
     * @return The type of string represented by this token.
     */
    public StringType stringType() {
        return StringType.NONE;
    }

    /**
     * Retrieves the type of comment represented by this token, if applicable.
     * Else, throws an exception.
     *
     * @return The type of comment represented by this token.
     * @throws UnsupportedOperationException If this is not a comment token.
     */
    public CommentStyle commentStyle() {
        throw new UnsupportedOperationException("not a comment");
    }

    /**
     * Indicates whether this token is either a line break or comment.
     *
     * @return <code>true</code>, if this token is
     */
    public boolean isMetadata() {
        return this.type == TokenType.BREAK || this.type == TokenType.COMMENT;
    }

    protected void setStart(final int start) {
        this.start = start;
    }

    protected void setEnd(final int end) {
        this.end = end;
    }

    protected void setOffset(final int offset) {
        this.offset = offset;
    }

    protected void setLine(final int line) {
        this.line = line;
    }

    protected void setLastLine(final int lastLine) {
        this.lastLine = lastLine;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Token) {
            return this.spanEquals((Token) other);
        }
        return false;
    }

}