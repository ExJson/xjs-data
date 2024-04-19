package xjs.data.serialization.writer;

import org.jetbrains.annotations.ApiStatus;
import xjs.data.JsonValue;
import xjs.data.serialization.JsonContext;

import java.util.Arrays;

/**
 * A series of options which may be used to stylize the output of any
 * {@link ElementWriter}.
 *
 * <p>Note that much of the formatting will additionally be configured
 * directly in the {@link JsonValue} metadata. It is possible that, in
 * the future, additional utilities will be provided to format these
 * data directly.
 */
public class JsonWriterOptions {

    private boolean allowCondense = true;
    private boolean bracesSameLine = true;
    private boolean nestedSameLine = false;
    private boolean omitRootBraces = true;
    private boolean outputComments = true;
    private boolean omitQuotes = false;
    private String eol = JsonContext.getEol();
    private String indent = "  ";
    private String separator = " ";
    private int minSpacing = 0;
    private int maxSpacing = Integer.MAX_VALUE;
    private int defaultSpacing = 1;
    private boolean smartSpacing = false;
    private boolean nextLineMulti = true;

    /**
     * Construct a new instance with default settings.
     */
    public JsonWriterOptions() {}

    /**
     * Construct a new instance copying settings from <code>source</code>.
     *
     * @param source The source being copied out of.
     */
    public JsonWriterOptions(final JsonWriterOptions source) {
        this.allowCondense = source.allowCondense;
        this.bracesSameLine = source.bracesSameLine;
        this.nestedSameLine = source.nestedSameLine;
        this.omitRootBraces = source.omitRootBraces;
        this.outputComments = source.outputComments;
        this.omitQuotes = source.omitQuotes;
        this.eol = source.eol;
        this.indent = source.indent;
        this.separator = source.separator;
        this.minSpacing = source.minSpacing;
        this.maxSpacing = source.maxSpacing;
        this.defaultSpacing = source.defaultSpacing;
        this.smartSpacing = source.smartSpacing;
        this.nextLineMulti = source.nextLineMulti;
    }

    /**
     * Indicates whether to allow condensed containers in the output.
     *
     * <p>For example, when this value is set to <code>false</code>, the
     * following JSON:
     *
     * <pre>{@code
     *   [ 1, 2, 3 ]
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   [
     *     1,
     *     2,
     *     3
     *   ]
     * }</pre>
     *
     * @return <code>true</code>, if condensing is allowed.
     */
    public boolean isAllowCondense() {
        return this.allowCondense;
    }

    /**
     * Sets whether to allow condensed containers in the output.
     *
     * @param allowCondense Whether to allow condensed containers.
     * @return <code>this</code>, for method chaining.
     * @see #isAllowCondense()
     */
    public JsonWriterOptions setAllowCondense(final boolean allowCondense) {
        this.allowCondense = allowCondense;
        return this;
    }

    /**
     * Indicates whether braces and brackets should be output on the same
     * line as their key.
     *
     * <p>For example, when this value is set to <code>false</code>, the
     * following JSON:
     *
     * <pre>{@code
     *   {
     *     "inner" : {
     *        "k": "v"
     *     }
     *   }
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   {
     *     "inner":
     *     {
     *       "k": "v"
     *     }
     *   }
     * }</pre>
     *
     * <p><b>Note</b>: callers should be aware that this option may only
     * affect the <em>default</em> style. In other words, it may be ignored
     * if the {@link JsonValue} metadata is configured otherwise. However,
     * the exact implementation does depend on the writer.
     *
     * @return Whether to output braces on the same line.
     */
    public boolean isBracesSameLine() {
        return this.bracesSameLine;
    }

    /**
     * Sets whether to output braces and brackets on the same line as their
     * keys.
     *
     * @param bracesSameLine Whether to output braces on the same line.
     * @return <code>this</code>, for method chaining.
     * @see #isBracesSameLine()
     */
    public JsonWriterOptions setBracesSameLine(final boolean bracesSameLine) {
        this.bracesSameLine = bracesSameLine;
        return this;
    }

    /**
     * Indicates whether output nested containers within arrays on the same
     * line as their parent.
     *
     * <p>For example, when this value is set to <code>true</code>, the
     * following JSON:
     *
     * <pre>{@code
     *   {
     *     "array": [
     *       {
     *         "k1": "v1"
     *       },
     *       {
     *         "k2": "v2"
     *       }
     *     ]
     *   }
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   {
     *     "array": [ {
     *       "k1": "v1"
     *     }, {
     *       "k2": "v2"
     *     } ]
     *   }
     * }</pre>
     *
     * <p><b>Note</b>: Callers should be aware that this method is currently
     * ignored by the provided serializers due to its complexity. It will
     * either be implemented at a later time or will simply be removed.
     *
     * @return Whether to open nested containers on the same line.
     * @apiNote Experimental - this setting is currently ignored by the
     *          provided serializers due to its complexity.
     */
    @ApiStatus.Experimental
    public boolean isNestedSameLine() {
        return this.nestedSameLine;
    }

    /**
     * Sets whether to open nested containers on the same line.
     *
     * @param nestedSameLine Whether to open nested containers on the same
     *                       line.
     * @return <code>this</code>, for method chaining.
     * @apiNote Experimental - this setting is currently ignored by the
     *          provided serializers due to its complexity.
     * @see #setNestedSameLine(boolean)
     */
    @ApiStatus.Experimental
    public JsonWriterOptions setNestedSameLine(final boolean nestedSameLine) {
        this.nestedSameLine = nestedSameLine;
        return this;
    }

    /**
     * Indicates whether to forcibly omit root braces <em>from any format which
     * supports it</em>.
     *
     * <p>For example, when this value is set to <code>true</code>, the following
     * XJS data:
     *
     * <pre>{@code
     *   {
     *     key: value
     *   }
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   key: value
     * }</pre>
     *
     * @return Whether to forcibly omit root braces.
     */
    public boolean isOmitRootBraces() {
        return this.omitRootBraces;
    }

    /**
     * Sets whether to forcibly omit root braces <em>from any format which
     * supports it</em>.
     *
     * @param omitRootBraces Whether to forcibly omit root braces.
     * @return <code>this</code>, for method chaining.
     * @see #isOmitRootBraces()
     */
    public JsonWriterOptions setOmitRootBraces(final boolean omitRootBraces) {
        this.omitRootBraces = omitRootBraces;
        return this;
    }

    /**
     * Indicates whether to output comments <em>for any format which supports
     * it</em>.
     *
     * <p>For example, when this value is set to <code>false</code>, the
     * following DJS:
     *
     * <pre>{@code
     *   # Comment
     *   key: value
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   key: value
     * }</pre>
     *
     * @return Whether to output comments.
     */
    public boolean isOutputComments() {
        return this.outputComments;
    }

    /**
     * Sets whether to allow comments <em>for any format which supports
     * it</em>.
     *
     * @param outputComments Whether to output comments.
     * @return <code>this</code>, for method chaining.
     * @see #isOutputComments()
     */
    public JsonWriterOptions setOutputComments(final boolean outputComments) {
        this.outputComments = outputComments;
        return this;
    }

    /**
     * Indicates whether to automatically remove quotes from strings
     * <em>for any format which supports it</em>.
     *
     * <p>For example, when this value is set to <code>true</code>,
     * the following Hjson:
     *
     * <pre>{@code
     *   key: "value"
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   key: value
     * }</pre>
     *
     * @return Whether to automatically omit quotes.
     */
    public boolean isOmitQuotes() {
        return this.omitQuotes;
    }

    /**
     * Sets whether to automatically remove quotes from strings
     * <em>for any format which supports it</em>.
     *
     * @param omitQuotes Whether to automatically omit quotes.
     * @return <code>this</code>, for method chaining.
     * @see #isOmitQuotes()
     */
    public JsonWriterOptions setOmitQuotes(final boolean omitQuotes) {
        this.omitQuotes = omitQuotes;
        return this;
    }

    /**
     * Gets the newline character to output at the end of each line.
     *
     * @return The newline character(s) in the JSON output.
     */
    public String getEol() {
        return eol;
    }

    /**
     * Sets the newline character to output at the end of each line.
     *
     * @param eol The newline character(s) in the JSON output.
     * @return <code>this</code>, for method chaining.
     */
    public JsonWriterOptions setEol(final String eol) {
        this.eol = eol;
        return this;
    }

    /**
     * Sets the indentation used in the JSON output.
     *
     * <p>for example, when this value is set to <pre>"    "</pre>,
     * the following JSON:
     *
     * <pre>{@code
     *   {
     *     "object": {
     *       "hello": "world"
     *     }
     *   }
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   {
     *       "object": {
     *           "hello": "world"
     *       }
     *   }
     * }</pre>
     *
     * @return The exact indentation to be used by the writer.
     */
    public String getIndent() {
        return this.indent;
    }

    /**
     * Sets the indentation used in the JSON output.
     *
     * @param indent The exact indentation used by the writer.
     * @return <code>this</code>, for method chaining.
     * @see #getIndent()
     */
    public JsonWriterOptions setIndent(final String indent) {
        this.indent = indent;
        return this;
    }

    /**
     * Convenience method to be used instead of {@link #setIndent}.
     * This implementation accepts a number of spaces to use for each
     * indentation.
     *
     * @param num The number of spaces to use for indentation.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if <code>num</code> is negative.
     * @see #getIndent()
     */
    public JsonWriterOptions setTabSize(final int num) {
        final char[] spaces = new char[num];
        Arrays.fill(spaces, ' ');
        this.indent = new String(spaces);
        return this;
    }

    /**
     * Gets the separator to be output at a variety of locations in the
     * JSON output.
     *
     * <p>For example, when this value is set to <pre>"  "</pre>, the
     * following JSON:
     *
     * <pre>{@code
     *   {
     *     "array": [ 1, 2, 3 ]
     *   }
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   {
     *     "array":  [  1,  2,  3  ]
     *   }
     * }</pre>
     *
     * @return The exact separator to be used by the writer.
     */
    public String getSeparator() {
        return this.separator;
    }

    /**
     * Sets the separator to be output at a variety of locations in the
     * JSON output.
     *
     * @param separator The exact separator to be used by the writer.
     * @return <code>this</code>, for method chaining.
     * @see #getSeparator()
     */
    public JsonWriterOptions setSeparator(final String separator) {
        this.separator = separator;
        return this;
    }

    /**
     * Sets the <em>minimum</em> number of lines between values.
     *
     * <p>For example, when this value is set to <code>2</code>, the
     * following JSON:
     *
     * <pre>{@code
     *   {
     *     "k1": "v1",
     *     "k2": "v2"
     *   }
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   {
     *     "k1": "v1",
     *
     *     "k2": "v2"
     *   }
     * }</pre>
     *
     * <p>Note that this value will be ignored by condensed containers.
     *
     * @return The minimum number of newline characters between values.
     */
    public int getMinSpacing() {
        return this.minSpacing;
    }

    /**
     * Sets the minimum number of newline characters between values.
     *
     * @param minSpacing the minimum number of newline characters.
     * @return <code>this</code>, for method chaining.
     * @see #getMinSpacing()
     */
    public JsonWriterOptions setMinSpacing(final int minSpacing) {
        this.minSpacing = minSpacing;
        return this;
    }

    /**
     * Sets the maximum number of newline characters between values.
     *
     * <p>For example, when this value is set to <code>1</code>, the
     * following JSON:
     *
     * <pre>{@code
     *   {
     *     "k1": "v1",
     *
     *     "k2": "v2"
     *   }
     * }</pre>
     *
     * <p>Will be output as:
     *
     * <pre>{@code
     *   {
     *     "k1": "v1",
     *     "k2": "v2"
     *   }
     * }</pre>
     *
     * @return The maximum number of newline characters.
     */
    public int getMaxSpacing() {
        return this.maxSpacing;
    }

    /**
     * Sets the maximum number of newline characters between values.
     *
     * @param maxSpacing The maximum number of newline characters.
     * @return <code>this</code>, for method chaining.
     * @see #getMaxSpacing()
     */
    public JsonWriterOptions setMaxSpacing(final int maxSpacing) {
        this.maxSpacing = maxSpacing;
        return this;
    }

    /**
     * Gets the <em>default</em> number of newline characters to
     * output between values. This method is mostly intended for
     * unformatted or <em>generated</em> JSON data.
     *
     * @return The default number of newline characters to output.
     * @see #getMinSpacing()
     */
    public int getDefaultSpacing() {
        return this.defaultSpacing;
    }

    /**
     * Sets the <em>default</em> number of newline characters to
     * output between values.
     *
     * @param defaultSpacing The default number of newline characters.
     * @return <code>this</code>, for method chaining.
     * @see #getMinSpacing()
     */
    public JsonWriterOptions setDefaultSpacing(final int defaultSpacing) {
        this.defaultSpacing = defaultSpacing;
        return this;
    }

    /**
     * Determines whether to use "smart spacing" in the output.
     *
     * <p><b>Smart spacing</b> essentially means that any container or
     * value with a header comment will get an extra space above and
     * below it. This should make it easier to identify keys in large
     * objects when syntax highlighting is unavailable.
     *
     * <p><b>This effect only applies to unformatted values</b> in
     * object containers and <b>does not apply to values in arrays</b>.
     *
     * <p>For example, when smart spacing is enabled, the following
     * JSON object:
     *
     * <pre>{@code
     *   {
     *     "a": 1,
     *     "b": 2,
     *     "c": {}
     *     "d": []
     *     "e": 5,
     *     "f": 6
     *   }
     * }</pre>
     *
     * <p>Will be output as follows:
     *
     * <pre>{@code
     *   {
     *     "a": 1,
     *     "b": 2,
     *
     *     "c": {}
     *
     *     "d": []
     *
     *     "e": 5,
     *     "f": 6
     *   }
     * }</pre>
     *
     * @return Whether to use smart spacing in the output.
     */
    public boolean isSmartSpacing() {
        return this.smartSpacing;
    }

    /**
     * Sets whether to use smart spacing in the output.
     *
     * @param smartSpacing Whether to use smart spacing in the output.
     * @return <code>this</code>, for method chaining.
     * @see #isSmartSpacing()
     */
    public JsonWriterOptions setSmartSpacing(final boolean smartSpacing) {
        this.smartSpacing = smartSpacing;
        return this;
    }

    /**
     * Indicates whether the formatter should move multiline strings onto the
     * next line by default. Supporting formats only.
     *
     * <p>For example, when this is enabled, an unformatted multiline string
     * will be moved onto the following line, as follows:
     *
     * <pre>{@code
     *
     * string:
     *   '''
     *   line 1
     *   line 2
     *   '''
     *
     * }</pre>
     *
     * <p>Otherwise, it will be formatted on the same line, as follows:
     *
     * <pre>{@code
     *
     *   string: '''
     *     line 1
     *     line 2
     *     '''
     *
     * }</pre>
     *
     * @return
     */
    public boolean isNextLineMulti() {
        return this.nextLineMulti;
    }

    /**
     * Sets whether to move unformatted multiline strings onto the next line
     * by default.
     *
     * @param nextLineMulti Whether to enable this formatting feature by default.
     * @return <code>this</code>, for method chaining.
     * @see #isNextLineMulti()
     */
    public JsonWriterOptions setNextLineMulti(final boolean nextLineMulti) {
        this.nextLineMulti = nextLineMulti;
        return this;
    }
}
