package xjs.performance.experimental.token;

import xjs.data.comments.CommentStyle;
import xjs.data.serialization.token.CommentToken;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.IOException;

public record ExperimentalCommentTokenParser(PositionTrackingReader reader) {

    public CommentToken readLineComment() throws IOException {
        final int s = this.reader.index - 1;
        final int o = this.reader.column - 1;
        this.reader.expect('/');
        if (this.reader.readIf('/')) {
            return this.readSingleComment(CommentStyle.LINE_DOC, s, o);
        }
        return this.readSingleComment(CommentStyle.LINE, s, o);
    }

    public CommentToken readHashComment() throws IOException {
        this.reader.expect('#');
        return this.readSingleComment(
            CommentStyle.HASH, this.reader.index, this.reader.column);
    }

    private CommentToken readSingleComment(
            final CommentStyle type, final int s, final int o) throws IOException {
        if (this.reader.isLineWhitespace()) {
            this.reader.read();
        }
        this.reader.startCapture();
        final int e = this.reader.skipToNL();
        return new CommentToken(s, e, this.reader.line, o, type, this.reader.endCapture());
    }

    public CommentToken readBlockComment() throws IOException {
        final int s = this.reader.index - 1;
        final int o = this.reader.column - 1;
        this.reader.expect('*');
        if (this.reader.readIf('*')) {
            return this.readMultiComment(CommentStyle.MULTILINE_DOC, s, o);
        }
        return this.readMultiComment(CommentStyle.BLOCK, s, o);
    }

    private CommentToken readMultiComment(
            final CommentStyle type, final int s, final int o) throws IOException {
        this.reader.skipLineWhitespace();

        final StringBuilder output = new StringBuilder();
        final int commentOffset = o + 1;
        final int line = this.reader.line;

        if (this.reader.readIf('\n')
                && this.skipToCommentOffset(output, commentOffset)) {
            return new CommentToken(
                s, this.reader.index, line, this.reader.line, o, type, output.toString());
        }
        char c = 0;
        int reset = 0;
        while (c != '*' || this.reader.current != '/') {
            if (this.reader.current == -1) {
                throw this.reader.expected("end of comment (*/)");
            }
            c = (char) this.reader.current;
            if (c != '*' && !this.reader.isWhitespace()) {
                reset = output.length();
            }
            if (c == '\n') {
                this.trimLine(output, reset);
                if (this.skipEmptyLines(output, commentOffset)) {
                    break;
                }
            } else {
                output.append(c);
                this.reader.read();
            }
        }
        this.trimLine(output, reset);
        return new CommentToken(
            s, this.reader.index, line, this.reader.line, o, type, output.toString());
    }

    private boolean skipEmptyLines(
            final StringBuilder output, final int commentOffset) throws IOException {
        while (this.reader.current == '\n') {
            output.append('\n');
            this.reader.read();
            if (this.skipToCommentOffset(output, commentOffset)) {
                return true;
            }
            if (this.reader.current == '\r') {
                this.reader.read();
            }
        }
        return false;
    }

    private void trimLine(final StringBuilder output, final int reset) {
        if (output.length() > 0) {
            output.setLength(reset + 1);
        }
    }

    private boolean skipToCommentOffset(
            final StringBuilder output, final int o) throws IOException {
        while (this.reader.column < o && this.reader.isLineWhitespace()) {
            this.reader.read();
        }
        while (this.reader.isLineWhitespace()) {
            output.append((char) this.reader.current);
            this.reader.read();
        }
        if (this.reader.current == '*') {
            this.reader.read();
            if (this.reader.isLineWhitespace()) {
                this.reader.read();
                return false;
            }
            return this.reader.current == '/';
        }
        return false;
    }
}
