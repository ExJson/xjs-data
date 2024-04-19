package xjs.data.serialization.token;

import xjs.data.comments.CommentStyle;

/**
 * Represents a single comment token: either a line, hash,
 * block comment, or other formatted comment variety.
 */
public class CommentToken extends ParsedToken {
    private final CommentStyle commentStyle;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start  The inclusive start index of this token.
     * @param end    The exclusive end index of this token.
     * @param line   The inclusive line number of this token.
     * @param offset The column of the start index.
     * @param type   The style of comment represented by this token.
     * @param text   The parsed text of the token.
     */
    public CommentToken(
            final int start, final int end, final int line, final int offset,
            final CommentStyle type, final String text) {
        super(start, end, line, offset, TokenType.COMMENT, text);
        this.commentStyle = type;
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param lastLine The inclusive line number at the end of this token.
     * @param offset   The column of the start index.
     * @param type     The style of comment represented by this token.
     * @param text   The parsed text of the token.
     */
    public CommentToken(
            final int start, final int end, final int line, final int lastLine,
            final int offset, final CommentStyle type, final String text) {
        super(start, end, line, lastLine, offset, TokenType.COMMENT, text);
        this.commentStyle = type;
    }

    /**
     * Constructs a new Comment Token with effectively no scope.
     *
     * @param text The text of the comment.
     */
    public CommentToken(final String text) {
        super(TokenType.COMMENT, text);
        this.commentStyle = CommentStyle.LINE;
    }

    @Override
    public CommentStyle commentStyle() {
        return this.commentStyle;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof CommentToken) {
            final CommentToken ct = (CommentToken) other;
            return this.commentStyle == ct.commentStyle
                && this.parsed.equals(ct.parsed)
                && this.spanEquals(ct);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.type + "(start:" + this.start + ",end:" + this.end + ",line:"
            + this.line + ",lastLine:" + lastLine + ",offset:" + this.offset
            + ",commentStyle:" + this.commentStyle + ",parsed:'" + this.parsed + "')";
    }
}
