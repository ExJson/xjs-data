package xjs.data.serialization.writer;

import xjs.data.comments.CommentData;
import xjs.data.comments.CommentStyle;
import xjs.data.comments.CommentType;
import xjs.data.JsonValue;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public abstract class CommentedElementWriter extends ElementWriter {
    protected final boolean outputComments;
    protected CommentStyle forcedStyle;

    protected CommentedElementWriter(
            final File file, final boolean format) throws IOException {
        super(file, format);
        this.outputComments = format;
    }

    protected CommentedElementWriter(
            final Writer writer, final boolean format) {
        super(writer, format);
        this.outputComments = format;
    }

    protected CommentedElementWriter(
            final File file, final JsonWriterOptions options) throws IOException {
        super(file, options);
        this.outputComments = options.isOutputComments();
    }

    protected CommentedElementWriter(
            final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
        this.outputComments = options.isOutputComments();
    }

    protected boolean hasComment(final CommentType type) {
        return this.outputComments
            && this.current != null
            && this.current().hasComment(type);
    }

    protected CommentData getComment(final CommentType type) {
        return this.current().getComments().getData(type);
    }

    @Override
    protected void writeAbove() throws IOException {
        if (this.format) {
            this.writeLines(this.getActualLinesAbove());
            if (this.hasComment(CommentType.HEADER)) {
                final CommentData data = this.getComment(CommentType.HEADER);
                this.writeComment(data);
                this.nl();
                if (this.level == -1
                        && !data.endsWithNewline()
                        && this.getLinesAbove(this.getFirst(this.current())) <= 0) {
                    this.nl();
                }
            }
        }
    }

    @Override
    protected void writeBetween() throws IOException {
        if (this.format) {
            final int lines =
                this.getActualLinesBetween();
            if (lines > 0) {
                this.writeLines(lines, this.level + 1);
            } else {
                this.tw.write(this.separator);
            }
            if (!this.hasComment(CommentType.VALUE)) {
                return;
            }
            CommentData data =
                this.getComment(CommentType.VALUE);

            CommentStyle style = this.forcedStyle;
            if (lines == 0 && !data.endsWithNewline()) {
                style = CommentStyle.BLOCK;
            }
            this.writeComment(data, style, this.level + 1, false);

            if (lines <= 0 && !data.endsWithNewline()) {
                this.tw.write(this.separator);
            } else if (!data.endsWithNewline()) {
                // coerce value onto the next line
                this.nl(this.level + 1);
            }
        }
    }

    @Override
    protected void writeAfter() throws IOException {
        if (!this.hasComment(CommentType.EOL)) {
            return;
        }
        final CommentData data =
            this.getComment(CommentType.EOL);
        final boolean separatorWritten =
            this.getLinesAbove(this.peek()) == 0;
        if (!separatorWritten) {
            this.tw.write(this.separator);
        }
        this.writeComment(data);
        if (separatorWritten) {
            this.tw.write(this.separator);
        }
    }

    @Override
    protected boolean writeTrailing() throws IOException {
        if (!this.format) {
            return false;
        }

        // todo: simplify

        final CommentData data =
            this.parent().getComments().getData(CommentType.INTERIOR);

        final boolean empty = this.parent().isEmpty();

        int lines = this.getActualLinesTrailing();
        if (empty) {
            lines = Math.min(1, lines);
        }
        if (data.getLines() > 0) {
            lines = Math.max(1, lines);
        }

        final int level = !data.isEmpty()
            ? this.level : this.level - 1;
        this.writeLines(lines, level);

        if (data.isEmpty()) {
            return lines > 0;
        }

        final boolean trim = data.endsWithNewline();

        if (lines <= 0) {
            this.tw.write(this.separator);
        }
        this.writeComment(data, this.forcedStyle, this.level, true);
        if (empty && lines <= 0) {
            this.tw.write(this.separator);
        }

        if (!trim && lines > 0) {
            this.nl(this.level - 1);
        }

        return lines > 0;
    }

    protected void writeFooter() throws IOException {
        final JsonValue source = this.parent != null
             ? this.parent() : this.previous();
        if (source == null || !source.hasComment(CommentType.FOOTER)) {
            return;
        }
        final CommentData data =
            source.getComments().getData(CommentType.FOOTER);
        this.nl(0);
        this.writeComment(data);
    }

    protected void writeComment(final CommentData data) throws IOException {
        this.writeComment(data, this.forcedStyle, this.level, false);
    }

    protected void writeComment(
            final CommentData data, final CommentStyle style,
            final int level, final boolean dedentLast) throws IOException {
        data.writeTo(this.tw, style, this.indent, level, this.eol, dedentLast);
    }

    @Override
    protected boolean requiresSmartSpace(final JsonValue value) {
        if (value == null) return false;
        return value.isContainer() || value.hasComment(CommentType.HEADER);
    }
}
