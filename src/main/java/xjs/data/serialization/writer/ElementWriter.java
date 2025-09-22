package xjs.data.serialization.writer;

import org.jetbrains.annotations.Nullable;
import xjs.data.JsonArray.Element;
import xjs.data.JsonContainer;
import xjs.data.JsonObject;
import xjs.data.JsonObject.Member;
import xjs.data.JsonReference;
import xjs.data.JsonString;
import xjs.data.JsonValue;
import xjs.data.StringType;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.util.BufferedStack;
import xjs.data.serialization.util.WritingBuffer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;

/**
 * A basic writer type providing a writer and some formatting options.
 */
public abstract class ElementWriter implements ValueWriter {

    protected static final Iterator<? extends Element> EMPTY_ITERATOR =
        Collections.emptyIterator();

    protected final boolean format;
    protected final Writer tw;
    protected final boolean allowCondense;
    protected final boolean bracesSameLine;
    protected final boolean nestedSameLine;
    protected final boolean omitRootBraces;
    protected final boolean omitQuotes;
    protected final String eol;
    protected final String indent;
    protected final int minSpacing;
    protected final int maxSpacing;
    protected final int defaultSpacing;
    protected final boolean smartSpacing;
    protected final boolean nextLineMulti;
    protected final String separator;

    protected final BufferedStack.OfTwo<
        Element, Iterator<? extends Element>> stack;
    protected Iterator<? extends Element> iterator;

    protected Element parent;
    protected Element previous;
    protected Element current;
    protected Element peek;
    protected int level;

    protected ElementWriter(final File file, final boolean format) throws IOException {
        this(new FileWriter(file), format);
    }

    protected ElementWriter(final Writer writer, final boolean format) {
        this.format = format;
        this.tw = new WritingBuffer(writer);
        this.eol = JsonContext.getEol();
        this.allowCondense = true;
        this.bracesSameLine = true;
        this.nestedSameLine = false;
        this.omitRootBraces = true;
        this.omitQuotes = true;
        this.indent = "  ";
        this.minSpacing = 0;
        this.maxSpacing = Integer.MAX_VALUE;
        this.defaultSpacing = format ? 1 : 0;
        this.smartSpacing = false;
        this.nextLineMulti = true;
        this.separator = format ? " " : "";
        this.stack = BufferedStack.ofTwo();
        this.level = 0;
    }

    protected ElementWriter(final File file, final JsonWriterOptions options) throws IOException {
        this(new FileWriter(file), options);
    }

    protected ElementWriter(final Writer writer, final JsonWriterOptions options) {
        this.format = true;
        this.tw = new WritingBuffer(writer);
        this.eol = options.getEol();
        this.allowCondense = options.isAllowCondense();
        this.bracesSameLine = options.isBracesSameLine();
        this.nestedSameLine = options.isNestedSameLine();
        this.omitRootBraces = options.isOmitRootBraces();
        this.omitQuotes = options.isOmitQuotes();
        this.indent = options.getIndent();
        this.minSpacing = options.getMinSpacing();
        this.maxSpacing = options.getMaxSpacing();
        this.defaultSpacing = options.getDefaultSpacing();
        this.smartSpacing = options.isSmartSpacing();
        this.nextLineMulti = options.isNextLineMulti();
        this.separator = options.getSeparator();
        this.stack = BufferedStack.ofTwo();
        this.level = 0;
    }

    /**
     * Appends a {@link JsonValue} of <em>any kind</em> into the writer being
     * wrapped by this object.
     *
     * @param value The value being serialized.
     * @throws IOException If the underlying writer throws an {@link IOException}.
     */
    @Override
    public void write(final JsonValue value) throws IOException {
        this.current = new Element(0, new JsonReference(value));
        this.write();
        this.tw.flush();
    }

    protected abstract void write() throws IOException;

    protected void next() {
        final Iterator<? extends Element> iterator =
            this.iterator;
        if (iterator == EMPTY_ITERATOR) {
            this.current = null;
            return;
        }
        final Element next =
            iterator.hasNext() ? iterator.next() : null;
        this.cycle(next);
    }

    protected void cycle(final Element peek) {
        this.previous = this.current;
        this.current = this.peek;
        this.peek = peek;
    }

    protected boolean push() {
        if (this.parent == this.current) {
            return false;
        }
        if (this.current.getOnly() instanceof JsonContainer) {
            if (this.parent != null) {
                this.stack.push(this.parent, this.iterator);
            }
            this.parent = this.current;
            this.iterator = this.getNextIterator();
            this.clear();
            if (this.iterator.hasNext()) {
                this.current = this.iterator.next();
                if (this.iterator.hasNext()) {
                    this.peek = this.iterator.next();
                }
            }
            this.level++;
            return true;
        }
        return false;
    }

    protected Iterator<? extends Element> getNextIterator() {
        if (this.current.getOnly().isObject()) {
            return this.current.getOnly().asObject().iterator();
        }
        return this.current.getOnly().asArray().elements().iterator();
    }

    protected boolean pop() {
        if (this.stack.isEmpty()) {
            this.iterator = EMPTY_ITERATOR;
            return false;
        }
        final int index = this.parent.getIndex();
        this.stack.pop();
        this.parent = this.stack.getFirst();
        this.iterator = this.stack.getSecond();
        this.reconstruct(index);
        this.level--;
        return true;
    }

    protected void reconstruct(final int index) {
        this.clear();
        if (index > 0) {
            this.previous = this.getElement(index - 1);
        }
        this.current = this.getElement(index);
        if (index < this.parent().size() - 1) {
            this.peek = this.getElement(index + 1);
        }
    }

    protected Element getElement(final int index) {
        final JsonContainer c = this.parent();
        if (c.isObject()) {
            final JsonObject o = c.asObject();
            return new Member(index, o.getKey(index), o.getReference(index));
        }
        return new Element(index, this.parent().getReference(index));
    }

    protected void clear() {
        this.previous = null;
        this.current = null;
        this.peek = null;
    }

    protected JsonContainer parent() {
        return this.parent != null ? this.parent.getOnly().asContainer() : null;
    }

    protected JsonValue previous() {
        return this.previous != null ? this.previous.getOnly() : null;
    }

    protected JsonValue current() {
        return this.current.getOnly();
    }

    protected JsonValue peek() {
        return this.peek != null ? this.peek.getOnly() : null;
    }

    protected int index() {
        return this.current != null ? this.current.getIndex() : 0;
    }

    protected String key() {
        return ((Member) this.current).getKey();
    }

    protected void open(final char opener) throws IOException {
        this.push();
        if (opener != 0) {
            this.tw.write(opener);
        }
        if (this.shouldSeparateOpener()) {
            this.tw.write(this.separator);
        }
    }

    protected void close(final char closer) throws IOException {
        if (this.format) {
            if (!this.writeTrailing() && this.shouldSeparateCloser()) {
                this.tw.write(this.separator);
            }
        }
        if (closer != 0) {
            this.tw.write(closer);
        }
        this.pop();
    }

    protected boolean shouldSeparateOpener() {
        final JsonContainer c = this.parent();
        return this.format
            && this.allowCondense
            && this.level > 0
            && !c.isEmpty()
            && this.getLinesAbove(this.getFirst(c)) == 0;
    }

    protected boolean shouldSeparateCloser() {
        return this.isCondensed() && this.level > 0;
    }

    protected void writeAbove() throws IOException {
        if (this.format) {
            this.writeLines(this.getActualLinesAbove());
        }
    }

    protected void writeBetween() throws IOException {
        if (this.format) {
            final int lines = this.getActualLinesBetween();
            if (lines > 0) {
                this.writeLines(lines, this.level + 1);
            } else {
                this.tw.write(this.separator);
            }
        }
    }

    protected void writeAfter() throws IOException {}

    protected boolean writeTrailing() throws IOException {
        if (this.format) {
            final int lines = this.getActualLinesTrailing();
            if (lines > 0) {
                this.writeLines(lines, this.level - 1);
                return true;
            }
        }
        return false;
    }

    protected boolean isCondensed() {
        return this.isCondensed(this.parent());
    }

    protected boolean isCondensed(final @Nullable JsonContainer c) {
        if (c == null || !this.allowCondense) {
            return false;
        }
        if (c.isEmpty()) {
            return false;
        }
        if (c.size() == 1 && this.level == -1) {
            return false;
        }
        if (this.getLinesAbove(this.getFirst(c)) != 0) {
            return false;
        }
        return c.size() == 1
            || this.getLinesAbove(this.getLast(c)) == 0;
    }

    protected JsonValue getFirst(final JsonContainer c) {
        return c.getReference(0).getOnly();
    }

    protected @Nullable JsonValue getFirst(final JsonValue v) {
        return v instanceof JsonContainer ? this.getFirst(v.asContainer()) : null;
    }

    protected JsonValue getLast(final JsonContainer c) {
        return c.getReference(c.size() - 1).getOnly();
    }

    protected int getLinesAbove(final @Nullable JsonValue value) {
        return value != null ? value.getLinesAbove() : -1;
    }

    protected int getActualLinesAbove() {
        final int lines = this.getLinesAbove(this.current());
        if (lines < 0) {
            return this.getDefaultLinesAbove();
        } else if (!this.isTopOfFile() && !this.allowCondense) {
            return Math.max(1, lines);
        } else if (this.level == 0 && this.index() == 0) {
            return lines;
        }
        return this.limitLines(lines);
    }

    protected int getDefaultLinesAbove() {
        if (this.isTopOfFile()) {
            return 0;
        }
        final int spacing = this.getSpacing();
        if (this.index() == 0) {
            if (this.level > 0) {
                return Math.max(1, spacing - 1);
            }
            return spacing - 1;
        }
        return spacing;
    }

    protected int getSpacing() {
        if (this.shouldDoSmartSpace()) {
            return this.defaultSpacing + 1;
        }
        return this.defaultSpacing;
    }

    protected boolean shouldDoSmartSpace() {
        if (this.smartSpacing && this.parent != null && this.parent().isObject()) {
            return this.requiresSmartSpace(this.current())
                || this.requiresSmartSpace(this.previous());
        }
        return false;
    }

    protected int getLinesBetween(final @Nullable JsonValue value) {
        return value != null ? value.getLinesBetween() : -1;
    }

    protected int getActualLinesBetween() {
        final JsonValue value = this.current();
        int lines = this.getLinesBetween(value);
        if (value.isContainer() && !this.bracesSameLine) {
            lines = Math.max(1, lines);
        } else {
            lines = Math.max(0, lines);
        }
        // Ignore min spacing between k/v, check b same line
        return Math.min(lines, this.maxSpacing);
    }

    protected int getActualLinesTrailing() {
        final int lines = this.parent().getLinesTrailing();
        if (lines < 0 && this.parent().size() > 0) {
            return this.getDefaultLinesTrailing();
        }
        return this.limitLines(lines);
    }

    private int getDefaultLinesTrailing() {
        if (this.isCondensed()) {
            return 0;
        } if (this.level >= 0) {
            return Math.max(1, this.defaultSpacing - 1);
        }
        return this.defaultSpacing - 1;
    }

    protected int limitLines(final int lines) {
        if (this.isCondensed()) { // Condensed arrays are allowed to have 0 lines between values.
            return Math.min(lines, this.maxSpacing);
        } else if (this.isTopOrBottom()) { // The top and bottom of each container should be slightly smaller.
            return Math.max(Math.min(lines, this.maxSpacing - 1), this.minSpacing - 1);
        }
        return Math.max(Math.min(lines, this.maxSpacing), this.minSpacing);
    }

    protected boolean isTopOrBottom() {
        return this.current == null || this.index() == 0;
    }

    protected boolean isTopOfFile() {
        return this.level == 0 && this.index() == 0;
    }

    protected boolean isEndOfContainer() {
        return this.parent == null
            || this.index() == this.parent().size() - 1;
    }

    protected boolean requiresSmartSpace(final JsonValue value) {
        return value instanceof JsonContainer;
    }

    protected void nl() throws IOException {
        this.nl(this.level);
    }

    protected void nl(final int level) throws IOException {
        if (this.format) {
            this.tw.write(this.eol);
            for (int i = 0; i < level; i++) {
                this.tw.write(this.indent);
            }
        }
    }

    protected void writeLines(final int lines) throws IOException {
        this.writeLines(lines, this.level);
    }

    protected void writeLines(
            final int lines, final int level) throws IOException {
        if (lines <= 0 || level < 0) {
            return;
        }
        for (int i = 0; i < lines; i++) {
            this.tw.write(this.eol);
        }
        for (int i = 0; i < level; i++) {
            this.tw.write(this.indent);
        }
    }

    protected void delimit() throws IOException {
        if (this.peek != null) {
            this.tw.write(',');
            if (this.allowCondense && this.getLinesAbove(this.peek()) == 0) {
                this.tw.write(this.separator);
            }
        }
    }

    protected void writeNumber(final double decimal) throws IOException {
        final long integer = (long) decimal;
        if (integer == decimal) {
            this.tw.write(Long.toString(integer));
            return;
        }
        String res = BigDecimal.valueOf(decimal).toEngineeringString();
        if (res.endsWith(".0")) {
            res = res.substring(0, res.length() - 2);
        } else if (res.contains("E")) {
            res = Double.toString(decimal).replace("E", "e");
        }
        this.tw.write(res);
    }

    protected void writeQuoted(final String text, final char quote) throws IOException {
        this.tw.write(quote);
        this.tw.write(escapeQuoted(text, quote));
        this.tw.write(quote);
    }

    protected static String escapeQuoted(final String text, char quote) {
        if (text == null) return null;

        for (int i = 0; i < text.length(); i++) {
            if (getEscapedChar(text.charAt(i), quote) != null) {
                final StringBuilder sb = new StringBuilder();
                if (i > 0) sb.append(text, 0, i);
                return doEscapeString(sb, text, i, quote);
            }
        }
        return text;
    }

    protected static String doEscapeString(
            final StringBuilder sb, final String text, final int cur, final char quote) {
        int start = cur;
        for (int i = cur; i < text.length(); i++) {
            final String escaped = getEscapedChar(text.charAt(i), quote);
            if (escaped != null) {
                sb.append(text, start, i);
                sb.append(escaped);
                start = i + 1;
            }
        }
        sb.append(text, start, text.length());
        return sb.toString();
    }

    protected static @Nullable String getEscapedChar(
            final char c, final char quote) {
        if (c == quote) {
            return "\\" + quote;
        }
        switch (c) {
            case '\t': return "\\t";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\f': return "\\f";
            case '\b': return "\\b";
            case '\\': return "\\\\";
            default: return null;
        }
    }

    protected void writeMulti(final String value) throws IOException {
        final int level = this.current instanceof Member
            ? this.level + 1 : this.level;
        if (this.shouldNextLineMulti()) {
            this.nl(level);
        }
        this.tw.write("'''");
        this.nl(level);
        int lastLine = 0;
        int i = 0;
        int quotes = 0;
        while (i < value.length()) {
            final char c = value.charAt(i);
            if (c == '\r') {
                if (i < value.length() - 1
                        && value.charAt(i + 1) == '\n') {
                    this.writeLine(value, level, lastLine, i);
                    lastLine = i + 2;
                    i++;
                }
            } else if (c == '\n') {
                this.writeLine(value, level, lastLine, i);
                lastLine = i + 1;
            } else if (c == '\'') {
                quotes++;
                if (quotes == 3) {
                    this.writeLine(value, level, lastLine, i);
                    this.tw.write("\\'");
                    lastLine = i;
                }
            } else {
                quotes = 0;
            }
            i++;
        }
        this.tw.write(value, lastLine, i - lastLine);
        this.nl(level);
        this.tw.write("'''");
    }

    protected boolean shouldNextLineMulti() {
        if (!this.nextLineMulti || !(this.current instanceof Member)) {
            return false;
        }
        final JsonValue source = this.current.getOnly();
        final int between = source.getLinesBetween();
        if (source instanceof JsonString
                && ((JsonString) source).getStringType() == StringType.MULTI) {
            return between == -1;
        }
        return between < 1; // coerce if string was not originally multi
    }

    protected void writeIndented(final String data) throws IOException {
        this.writeIndented(data, this.level, false);
    }

    protected void writeIndented(
            final String data, final int level) throws IOException {
        this.writeIndented(data, level, false);
    }

    protected void writeIndented(
            final String data, final int level, final boolean trim) throws IOException {
        int lastLine = 0;
        int i = 0;
        while (i < (trim ? data.length() - 1 : data.length())) {
            final char c = data.charAt(i);
            if (c == '\r') {
                if (i < data.length() - 1
                        && data.charAt(i + 1) == '\n') {
                    this.writeLine(data, level, lastLine, i);
                    lastLine = i + 1;
                    i++;
                }
            } else if (c == '\n') {
                this.writeLine(data, level, lastLine, i);
                lastLine = i + 1;
            }
            i++;
        }
        this.tw.write(data, lastLine, i - lastLine);
    }

    protected void writeLine(
            final String value, int level, final int s, final int e) throws IOException {
        if (e < value.length() - 2) {
            final char peek = value.charAt(e + 2);
            if (peek == '\r' || peek == '\n') {
                level = 0; // optimize to trim unnecessary whitespace
            }
        }
        if (e - s != 0) {
            this.tw.write(value, s, e - s);
        }
        this.nl(level);
    }

    @Override
    public void close() throws IOException {
        this.tw.close();
    }
}
