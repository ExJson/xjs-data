package xjs.data.comments;

import org.jetbrains.annotations.Nullable;
import xjs.data.serialization.JsonContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommentData {
    private final List<Object> comments;
    private int lines;

    public CommentData() {
        this(new ArrayList<>(), 0);
    }

    private CommentData(final List<Object> comments, final int lines) {
        this.comments = comments;
        this.lines = lines;
    }

    public static CommentData immutable() {
        return new CommentData(Collections.emptyList(), 0);
    }

    public void append(final Comment comment) {
        this.comments.add(comment);
        this.appendLines(comment);
    }

    public void append(final int lines) {
        if (this.comments.isEmpty()) {
            this.comments.add(lines);
        } else {
            final int end = this.comments.size() - 1;
            final Object last = this.comments.get(end);
            if (last instanceof Integer) {
                this.comments.set(end, ((Integer) last) + lines);
            } else {
                this.comments.add(lines);
            }
        }
        this.lines += lines;
    }

    public void append(final CommentData data) {
        this.comments.addAll(data.comments);
        this.lines += data.lines;
    }

    public void prepend(final Comment comment) {
        this.comments.add(0, comment);
    }

    public void prepend(final int lines) {
        if (this.comments.isEmpty()) {
            this.comments.add(lines);
        } else {
            final Object first = this.comments.get(0);
            if (first instanceof Integer) {
                this.comments.set(0, ((Integer) first) + lines);
            } else {
                this.comments.add(0, lines);
            }
        }
        this.lines += lines;
    }

    private void appendLines(final Comment comment) {
        final String text = comment.text();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                this.lines++;
            }
        }
    }

    public void setLinesAfter(final int lines) {
        if (this.comments.isEmpty()) {
            this.comments.add(lines);
            return;
        }
        final int last = this.comments.size() - 1;
        if (this.comments.get(last) instanceof Integer) {
            this.comments.set(last, lines);
            return;
        }
        this.comments.add(lines);
    }

    public boolean isEmpty() {
        return this.comments.isEmpty();
    }

    public int getLines() {
        return this.lines;
    }

    public boolean endsWithNewline() {
        return !this.comments.isEmpty()
            && this.comments.get(this.comments.size() - 1) instanceof Integer;
    }

    public void trimLastNewline() {
        if (!this.comments.isEmpty()) {
            final int end = this.comments.size() - 1;
            final Object last = this.comments.get(end);
            if (last instanceof Integer) {
                this.comments.set(end, ((Integer) last) - 1);
            }
        }
    }

    public @Nullable CommentData takeOpenHeader() {
        if (this.isEmpty()) {
            return null;
        }
        final int end = this.getLastGap();
        if (end < 2) {
            return null;
        }
        return this.takeBefore(end + 1);
    }

    private int getLastGap() {
        for (int i = this.comments.size() - 2; i >= 0; i--) {
            final Object o = this.comments.get(i);
            if (o instanceof Integer && ((Integer) o) > 1) {
                return i;
            }
        }
        return -1;
    }

    private CommentData takeBefore(final int index) {
        final CommentData taken = new CommentData();
        for (int i = 0; i < index; i++) {
            final Object removed = this.comments.remove(0);
            if (removed instanceof Comment) {
                taken.append((Comment) removed);
            } else {
                taken.append((Integer) removed);
            }
        }
        return taken;
    }

    public int takeLastLinesSkipped() {
        return (Integer) this.comments.remove(this.comments.size() - 1);
    }

    public void writeTo(final Appendable out) throws IOException {
        this.writeTo(out, null, "", 0, JsonContext.getEol(), false);
    }

    public void writeTo(final Appendable out, final CommentStyle style) throws IOException {
        this.writeTo(out, style, "", 0, JsonContext.getEol(), false);
    }

    public void writeTo(
            final Appendable out, final String indent, final int level, final String eol) throws IOException {
        this.writeTo(out, null, indent, level, eol, false);
    }

    public void writeTo(
            final Appendable out, @Nullable final CommentStyle style, final String indent,
            int level, final String eol, final boolean dedentLast) throws IOException {
        for (int i = 0; i < this.comments.size(); i++) {
            final Object o = this.comments.get(i);
            if (o instanceof final Comment comment) {
                final CommentStyle styleOut = style != null ? style : comment.style();
                this.append(out, comment.text(), styleOut, indent, level, eol);
            } else if ((Integer) o > 0) {
                for (int j = 0; j < (Integer) o - 1; j++) {
                    out.append(eol);
                }
                if (dedentLast && i == this.comments.size() -1) {
                    level--;
                }
                this.nl(out, indent, level, eol);
            }
        }
    }

    @Override
    public String toString() {
        if (this.comments.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Object o : this.comments) {
            if (o instanceof Comment) {
                sb.append(((Comment) o).text());
            } else {
                sb.append("\n".repeat(Math.max(0, (Integer) o)));
            }
        }
        return sb.toString();
    }

    public CommentData copy() {
        return new CommentData(new ArrayList<>(this.comments), this.lines);
    }

    @Override
    public int hashCode() {
        return this.lines + 31 * this.comments.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof final CommentData d) {
            return this.lines == d.lines
                && this.comments.equals(d.comments);
        }
        return false;
    }

    private void append(
            final Appendable out, final String text, final CommentStyle style,
            final String separator, final int level, final String eol) throws IOException {
        if (style.isMultiline()) {
            this.appendMultiline(out, text, style, separator, level, eol);
            return;
        }
        int s = 0;
        for (int i = 0; i < text.length() + 1; i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                final int e = i > 0 && text.charAt(i - 1) == '\r' ? i - 1 : i;
                out.append(style.getPrefix());
                out.append(' ');
                out.append(text, s, e);
                if (i != text.length()) {
                    this.nl(out, separator, level, eol);
                }
                s = i + 1;
            }
        }
    }

    private void appendMultiline(
            final Appendable out, final String text, final CommentStyle style,
            final String separator, final int level, final String eol) throws IOException {

        if (!text.contains("\n")) {
            out.append(style.getPrefix());
            out.append(' ');
            out.append(text);
            out.append(" */");
            return;
        }
        out.append(style.getPrefix());
        this.nl(out, separator, level, eol);
        int s = 0;
        for (int i = 0; i < text.length() + 1; i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                final int e = i > 0 && text.charAt(i - 1) == '\r' ? i - 1 : i;
                if (s != 0) {
                    this.nl(out, separator, level, eol);
                }
                out.append(" * ");
                out.append(text, s, e);
                s = i + 1;
            }
        }
        this.nl(out, separator, level, eol);
        out.append(" */");
    }

    private void nl(
            final Appendable out, final String separator,
            final int level, final String eol) throws IOException {
        out.append(eol);
        for (int i = 0; i < level; i++) {
            out.append(separator);
        }
    }

}
