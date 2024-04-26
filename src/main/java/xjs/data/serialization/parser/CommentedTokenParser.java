package xjs.data.serialization.parser;

import xjs.data.comments.Comment;
import xjs.data.comments.CommentData;
import xjs.data.comments.CommentType;
import xjs.data.JsonObject;
import xjs.data.serialization.token.CommentToken;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenType;

/**
 * Specialized implementation of {@link TokenParser} designed to tolerate
 * commented JSON formats. This class provides utilities for storing and
 * applying comments to the output, as well as overrides to neatly mesh
 * this logic into the existing interface where possible.
 *
 * @see TokenParser
 */
public abstract class CommentedTokenParser extends TokenParser {
    protected CommentData commentBuffer;

    protected CommentedTokenParser(final TokenStream root) {
        super(root);
        this.commentBuffer = new CommentData();
    }

    @Override
    protected boolean consumeWhitespace(
            final Token t, final boolean nl) {
        if (!t.isMetadata()) {
            return false;
        }
        if (t.type() == TokenType.BREAK) {
            if (!nl) {
                return false;
            }
            this.flagLineAsSkipped();
        } else {
            this.appendComment((CommentToken) t);
        }
        return true;
    }

    @Override
    protected void flagLineAsSkipped() {
        if (!this.commentBuffer.isEmpty()) {
            this.commentBuffer.append(1);
        } else {
            this.linesSkipped++;
        }
    }

    @Override
    protected void setAbove() {
        this.setComment(CommentType.HEADER);
        super.setAbove();
    }

    @Override
    protected void setBetween() {
        this.setComment(CommentType.VALUE);
        super.setBetween();
    }

    @Override
    protected void setTrailing() {
        this.setComment(CommentType.INTERIOR);
        super.setTrailing();
    }

    /**
     * Splits any comments above an open root object (supported formats
     * only) into a root header and a first value header.
     *
     * @param root The root {@link JsonObject} where this data will be
     *             stored.
     */
    protected void readAboveOpenRoot(final JsonObject root) {
        this.readWhitespace(false);
        this.splitOpenHeader(root);
    }

    @Override
    protected void readAfter() {
        this.readLineWhitespace();
        this.setComment(CommentType.EOL);
    }

    @Override
    protected void readBottom() {
        this.readWhitespace(false);

        this.prependLinesSkippedToComment();
        this.setComment(CommentType.FOOTER);
        this.expectEndOfText();
    }

    /**
     * Appends the given token to the comment buffer.
     *
     * @param t Any token representing a comment.
     */
    protected void appendComment(final CommentToken t) {
        this.commentBuffer.append(new Comment(t));
    }

    /**
     * For each line skipped, appends a newline character to the
     * <em>beginning</em> of the comment buffer, resetting the
     * counter to 0.
     */
    protected void prependLinesSkippedToComment() {
        if (this.linesSkipped > 1) {
            this.commentBuffer.prepend(this.linesSkipped);
            this.linesSkipped = 0;
        }
    }

    /**
     * Applies the contents of the comment buffer to the formatting
     * output as the given <em>type</em> of comment.
     *
     * @param type The type of comment being set.
     */
    protected void setComment(final CommentType type) {
        final CommentData data = this.takeComment(type);
        if (!data.isEmpty()) {
            this.formatting.getComments().setData(type, data);
        }
    }

    /**
     * Returns the full text of the comment buffer, cropping it
     * according to the rules defined in {@link CommentType}.
     *
     * @param type The type of comment currently in the buffer.
     * @return The text of the comment buffer.
     */
    protected CommentData takeComment(final CommentType type) {
        if (this.commentBuffer.isEmpty()) {
            return new CommentData();
        }
        // line comments _must_ have newlines,
        // so they will be added later.
        if (this.commentBuffer.endsWithNewline()) {
            if (this.shouldTakeNl(type)) {
                this.trimComment();
                this.linesSkipped++;
            } else if (this.shouldTrimNl(type)) {
                this.trimComment();
            }
        }
        final CommentData taken = this.commentBuffer;
        this.commentBuffer = new CommentData();
        return taken;
    }

    // header comments always include a newline
    protected final boolean shouldTrimNl(final CommentType type) {
        return type == CommentType.HEADER;
    }

    // eol comments go to eol, but do not include the newline
    protected final boolean shouldTakeNl(final CommentType type) {
        return type == CommentType.EOL;
    }

    /**
     * Removes the last character (usually a newline character) from the
     * comment buffer.
     */
    protected final void trimComment() {
        this.commentBuffer.trimLastNewline();
    }

    /**
     * Splits the contents of the comment buffer into a header of the
     * root JSON object and one of the first member in the object.
     *
     * <p>For example, assume the following comments:
     *
     * <pre>
     *     // first
     *     // second
     *
     *     // third
     *
     *     // fourth
     *     key: value
     * </pre>
     *
     * <p>This method pairs the first, second, and third comment with
     * the root object, and the fourth comment with the key-value pair.
     *
     * @param root The root {@link JsonObject} where the data will be
     *             stored.
     */
    protected void splitOpenHeader(final JsonObject root) {
        final CommentData header = this.commentBuffer.takeOpenHeader();
        if (header != null) {
            root.getComments().setData(CommentType.HEADER, header);
            root.setLinesAbove(this.linesSkipped);
            this.linesSkipped = header.takeLastLinesSkipped() - 1;
        }
    }
}
