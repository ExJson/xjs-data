package xjs.data.serialization;

import xjs.data.comments.CommentStyle;
import xjs.data.Json;
import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.parser.JsonParser;
import xjs.data.serialization.parser.ParsingFunction;
import xjs.data.serialization.parser.DjsParser;
import xjs.data.serialization.token.DjsTokenizer;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenizingFunction;
import xjs.data.serialization.util.PositionTrackingReader;
import xjs.data.serialization.writer.JsonWriter;
import xjs.data.serialization.writer.JsonWriterOptions;
import xjs.data.serialization.writer.WritingFunction;
import xjs.data.serialization.writer.DjsWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A collection of settings and serializers for automatic parsing and writing.
 *
 * <p>This context may be used to configure the behavior of this library. For
 * example, to configure the default comment style or newline character used
 * by all provided serializers:
 *
 * <pre>{@code
 *   JsonContext.setDefaultCommentStyle(CommentStyle.HASH);
 *   JsonContext.setEol("\n");
 * }</pre>
 *
 * <p>Or the formatting options used by all default serializers:
 *
 * <pre>{@code
 *   JsonContext.setDefaultFormatting(
 *     new JsonWriterOptions()
 *       .setIndent("    ")
 *       .setMaxSpacing(3))
 * }</pre>
 *
 * <p>In addition, the context is provided as a way to configure the automatic
 * format selection of {@link Json} and {@link JsonValue}:
 *
 * <pre>{@code
 *   JsonContext.addParser("yaml", file -> new MyYamlParser(file).parse());
 *   JsonContext.addWriter("yaml" (w, v, o) -> new MyYamlWriter(w, o).write(v));
 * }</pre>
 */
public class JsonContext {

    private static final Map<String, ParsingFunction> PARSERS = new ConcurrentHashMap<>();
    private static final Map<String, WritingFunction> WRITERS = new ConcurrentHashMap<>();
    private static final Map<String, TokenizingFunction> TOKENIZERS = new ConcurrentHashMap<>();
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();
    private static final ParsingFunction DEFAULT_PARSER = ParsingFunction.fromParser(DjsParser::new);
    private static final WritingFunction DEFAULT_WRITER = WritingFunction.fromWriter(DjsWriter::new);
    private static final TokenizingFunction DEFAULT_TOKENIZER = TokenizingFunction.fromTokenizer(DjsTokenizer::new);
    private static volatile String eol = System.lineSeparator();
    private static volatile CommentStyle defaultCommentStyle = CommentStyle.LINE;
    private static volatile JsonWriterOptions defaultFormatting = new JsonWriterOptions();

    /**
     * Indicates whether the xjs-compat module is provided, enabling support for
     * Hjson, JSONC, YAML, and other foreign serializers.
     */
    public static final boolean COMPAT_AVAILABLE;

    /**
     * Indicates whether the xjs-transform module is provided, enabling support for
     * data transforms and formatters.
     */
    public static final boolean TRANSFORM_AVAILABLE;

    /**
     * Adds or replaces a parser for the given format.
     *
     * <p>Note that parsing functions would ideally not get reused for multiple formats.
     * For this purpose, use {@link #registerAlias}.
     *
     * @param format The file extension corresponding to this parser.
     * @param parser A function of {@link PositionTrackingReader} -> {@link JsonValue} throwing IOException
     */
    public static void addParser(final String format, final ParsingFunction parser) {
        PARSERS.put(format.toLowerCase(), parser);
    }

    /**
     * Adds or replaces a writer for the given format.
     *
     * <p>Note that writing functions would ideally not get reused for multiple formats.
     * For this purpose, use {@link #registerAlias}.
     *
     * @param format The file extension corresponding to the parser.
     * @param writer A consumer of ({@link Writer}, {@link JsonValue}, {@link JsonWriterOptions})
     */
    public static void addWriter(final String format, final WritingFunction writer) {
        WRITERS.put(format.toLowerCase(), writer);
    }

    /**
     * Adds or replaces a tokenizer for the given format.
     *
     * <p>Note that tokenizing functions would ideally not get reused for multiple formats.
     * For this purpose, use {@link #registerAlias}.
     *
     * @param format The file extension corresponding to the parser.
     * @param tokenizer A function of ({@link PositionTrackingReader}, boolean -> {@link TokenStream})
     */
    public static void addTokenizer(final String format, final TokenizingFunction tokenizer) {
        TOKENIZERS.put(format.toLowerCase(), tokenizer);
    }

    /**
     * Registers an alias for some other format to the context.
     *
     * <p>For example, to register <code>yml</code> as an alias of <code>yaml</code>:
     *
     * <pre>{@code
     *   JsonContext.registerAlias("yml", "yaml");
     * }</pre>
     *
     * @param alias  An alias for the expected format.
     * @param format The expected format being configured.
     */
    public static void registerAlias(final String alias, final String format) {
        ALIASES.put(alias.toLowerCase(), format.toLowerCase());
    }

    /**
     * Gets the <em>default</em> newline character configured the provided serializers.
     *
     * @return The default newline character, usually {@link System#lineSeparator()}
     */
    public static synchronized String getEol() {
        return eol;
    }

    /**
     * Sets the <em>default</em> newline character configured for the provided serializers.
     *
     * @param eol The default newline character, e.g. <code>\n</code>
     */
    public static synchronized void setEol(final String eol) {
        JsonContext.eol = eol;
    }

    /**
     * Gets the <em>default</em> newline character used by {@link JsonValue#setComment(String)}.
     *
     * @return The configured {@link CommentStyle}.
     */
    public static synchronized CommentStyle getDefaultCommentStyle() {
        return defaultCommentStyle;
    }

    /**
     * Sets the <em>default</em> newline character used by {@link JsonValue#setComment(String)}.
     *
     * @param style The configured {@link CommentStyle}.
     */
    public static synchronized void setDefaultCommentStyle(final CommentStyle style) {
        defaultCommentStyle = style;
    }

    /**
     * Gets the <em>default</em> formatting options used by the provided serializers.
     *
     * @return The default {@link JsonWriterOptions}.
     */
    public static synchronized JsonWriterOptions getDefaultFormatting() {
        return defaultFormatting;
    }

    /**
     * Sets the <em>default</em> formatting options used by the provided serializers.
     *
     * @param options The default {@link JsonWriterOptions}.
     */
    public static synchronized void setDefaultFormatting(final JsonWriterOptions options) {
        defaultFormatting = options;
    }

    /**
     * Indicates whether the given file is extended with a known format or alias.
     *
     * @param file The file being tested.
     * @return <code>true</code>, if the extension is recognized by the context.
     */
    public static boolean isKnownFormat(final File file) {
        final String ext = getExtension(file);
        return PARSERS.containsKey(ext) || ALIASES.containsKey(ext);
    }

    /**
     * Parses the given file automatically based on its extension.
     *
     * <p>This method is the delegate of {@link Json#parse(File)}.
     *
     * @param file The file being parsed as some kind of JSON file or superset.
     * @return The {@link JsonValue} represented by the file.
     * @throws IOException If the underlying {@link FileReader} throws an exception.
     * @throws SyntaxException If the contents of the file are syntactically invalid.
     */
    public static JsonValue autoParse(final File file) throws IOException {
        return getParser(getExtension(file)).parse(file);
    }

    /**
     * Writes the given file automatically based on its extension.
     *
     * <p>This method is the delegate of {@link JsonValue#write(File)}.
     *
     * @param file  The file being written as some kind of JSON file or superset.
     * @param value The {@link JsonValue} to be represented by the file.
     * @throws IOException If the underlying {@link FileWriter} throws an exception.
     */
    public static void autoWrite(final File file, final JsonValue value) throws IOException {
        getWriter(getExtension(file)).write(file, value, defaultFormatting);
    }

    /**
     * Gets a parsing function for the given format. This method provides
     * a utility which allows the caller to parse from a variety of sources.
     *
     * @param ext The data type, with support for aliases.
     * @return An interface used for parsing JSON data from various sources.
     */
    public static ParsingFunction getParser(final String ext) {
        return PARSERS.getOrDefault(getFormat(ext), DEFAULT_PARSER);
    }

    /**
     * Gets a writing function for the given format. This method provides
     * a utility which allows the caller to output to a variety sources.
     *
     * @param ext The data type, with support for aliases.
     * @return An interface used for writing JSON data to various sources.
     */
    public static WritingFunction getWriter(final String ext) {
        return WRITERS.getOrDefault(getFormat(ext), DEFAULT_WRITER);
    }

    /**
     * Gets a writing function for the given format. This method provides
     * a utility which allows the caller to output to a variety sources.
     *
     * @param ext The data type, with support for aliases.
     * @return An interface used for parsing {@link Token tokens} from various sources.
     */
    public static TokenizingFunction getTokenizer(final String ext) {
        return TOKENIZERS.getOrDefault(getFormat(ext), DEFAULT_TOKENIZER);
    }

    private static String getFormat(final File file) {;
        return getFormat(getExtension(file));
    }

    private static String getFormat(final String ext) {
        return ALIASES.getOrDefault(ext, ext);
    }

    private static String getExtension(final File file) {
        final int index = file.getName().lastIndexOf('.');
        return index < 0 ? "djs" : file.getName().substring(index + 1).toLowerCase();
    }

    static {
        PARSERS.put("json", ParsingFunction.fromParser(JsonParser::new));
        PARSERS.put("djs", DEFAULT_PARSER);
        WRITERS.put("json", WritingFunction.fromWriter(JsonWriter::new));
        WRITERS.put("djs", DEFAULT_WRITER);
        ALIASES.put("xjs", "djs"); // temporary to provide compat until the new format

        COMPAT_AVAILABLE = isClassAvailable("xjs.compat.serialization.XjsCompat");
        TRANSFORM_AVAILABLE = isClassAvailable("xjs.transform.JsonTransformer");
    }

    private static boolean isClassAvailable(final String name) {
        try {
            Class.forName(name);
            return true;
        } catch (final ClassNotFoundException ignored) {}
        return false;
    }
}
