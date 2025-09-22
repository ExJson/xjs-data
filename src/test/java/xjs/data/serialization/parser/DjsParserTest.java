package xjs.data.serialization.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import xjs.data.comments.CommentType;
import xjs.data.JsonArray;
import xjs.data.JsonObject;
import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.util.PositionTrackingReader;
import xjs.data.serialization.writer.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DjsParserTest extends CommonParserTest {

    @Test
    public void parse_ignoresTrailingCommas() {
        assertTrue(new JsonArray().add(1).add(2).add(3)
            .matches(this.parse("[1,2,3,]")));
    }

    @Test
    public void parse_readsUnquotedKeys() {
        assertEquals("key", this.parse("{key:'value'}").asObject().keys().get(0));
    }

    @Test
    public void parse_readsMultipleUnquotedKeys() {
        assertTrue(new JsonObject().add("k1", "v1").add("k2", "v2")
            .matches(this.parse("{k1:'v1',k2:'v2'}")));
    }

    @Test
    public void parse_readsOpenRoot() {
        assertTrue(new JsonObject().add("a", 1).add("b", 2)
            .matches(this.parse("a:1,b:2")));
    }

    @Test
    public void parseOpenRoot_whenFirstValueIsAtTheTop_setsLinesAboveToAuto() {
        final JsonValue a = this.parse("a:1").asObject().get("a");
        assertEquals(-1, Objects.requireNonNull(a).getLinesAbove());
    }

    @Test
    public void parse_doesNotTolerate_nonDelimitedContainers() {
        assertThrows(SyntaxException.class,
            () -> this.parse("[[][]]"));
    }

    @Test
    public void parse_doesNotTolerate_leadingDelimiter() {
        assertThrows(SyntaxException.class, () -> this.parse("[,]"));
    }

    @Test
    public void parse_doesNotTolerate_missingValue() {
        assertThrows(SyntaxException.class, () -> this.parse("k:,"));
    }

    @Test
    public void parse_doesNotTolerate_missingKey() {
        assertThrows(SyntaxException.class, () -> this.parse(":"));
    }

    @Test
    public void emptyFile_isImplicitlyAnObject() {
        assertTrue(this.parse("").isObject());
    }

    @Test
    public void parseValue_readsUntilEndOfLine() {
        assertTrue(new JsonObject().add("k", "v").add("r", "t")
            .matches(this.parse("k:'v'\nr:'t'")));
    }

    @Test
    public void parse_doesNotTolerate_newlinesInKey() {
        assertThrows(SyntaxException.class, () -> this.parse("k\n1:v"));
    }

    @Test
    public void parse_readsSingleQuotedString() {
        assertEquals("", this.parse("''").asString());
    }

    @Test
    public void parse_readsMultilineString() {
        assertEquals("test", this.parse("'''test'''").asString());
    }

    @Test
    public void parse_toleratesEmptyMultilineString() {
        assertEquals("", this.parse("''''''").asString());
    }

    @Test
    public void multilineString_ignoresLeadingWhitespace() {
        assertEquals("test", this.parse("'''  test'''").asString());
    }

    @Test
    public void multilineString_ignoresTrailingNewline() {
        assertEquals("test", this.parse("'''test\n'''").asString());
    }

    @Test
    public void multilineString_preservesIndentation_bySubsequentLines() {
        final String text = """
            multi:
              '''
              0
               1
                2
              '''
            """;
        assertEquals("0\n 1\n  2", this.parse(text).asObject().getAsserted("multi").asString());
    }

    @Test
    public void parse_readsLeadingDecimal() {
        assertEquals(0.1234, this.parse(".1234").asDouble());
    }

    @Test
    public void parse_doesNotTolerate_leadingDecimal_withoutFollowingNumber() {
        assertThrows(SyntaxException.class, () -> this.parse(".+1234"));
    }

    @Test
    public void parse_ignoresLeadingPlus() {
        assertEquals(1234, this.parse("+1234").asDouble());
    }

    @Test
    public void parse_doesNotTolerate_leadingPlus_withoutFollowingNumber() {
        assertThrows(SyntaxException.class, () -> this.parse("+.1234"));
    }

    @ParameterizedTest
    @CsvSource({"/*header*/", "#header", "//header"})
    public void parse_preservesHeaderComment_atTopOfFile(final String comment) {
        assertEquals("header",
            this.parse(comment + "\n{}").getComment(CommentType.HEADER));
    }

    @ParameterizedTest
    @CsvSource({"/*footer*/", "#footer", "//footer"})
    public void parse_preservesFooterComment_atBottomOfFile(final String comment) {
        assertEquals("footer",
            this.parse("{}\n" + comment).getComment(CommentType.FOOTER));
    }

    @ParameterizedTest
    @CsvSource({"/*eol*/", "#eol", "//eol"})
    public void parse_preservesEolComment_afterClosingRootBrace(final String comment) {
        assertEquals("eol",
            this.parse("{}" +  comment).getComment(CommentType.EOL));
    }

    @ParameterizedTest
    @CsvSource({"/*header*/", "#header", "//header"})
    public void parse_preservesHeader_aboveValue(final String comment) {
        assertEquals("header",
            this.parse(comment + "\nk:'v'").asObject().get(0).getComment(CommentType.HEADER));
    }

    @ParameterizedTest
    @CsvSource({"/*value*/", "#value", "//value"})
    public void parse_preservesValueComment_betweenKeyValue(final String comment) {
        assertEquals("value\n",
            this.parse("k:\n" + comment + "\n'v'")
                .asObject().get(0).getComment(CommentType.VALUE));
    }

    @ParameterizedTest
    @CsvSource({"/*eol*/", "#eol", "//eol"})
    public void parse_preservesEolComment_afterValue(final String comment) {
        assertEquals("eol",
            this.parse("k:'v'" + comment).asObject().get(0).getComment(CommentType.EOL));
    }

    @ParameterizedTest
    @CsvSource({"/*interior*/", "#interior", "//interior"})
    public void parse_preservesInteriorComment_inContainer(final String comment) {
        assertEquals("interior\n",
            this.parse("{\n" + comment + "\n}").getComment(CommentType.INTERIOR));
    }

    @ParameterizedTest
    @CsvSource({"/*comment*/", "#comment", "//comment"})
    public void parse_preservesNewlines_afterComments(final String comment) {
        assertEquals("comment\n",
            this.parse("k1:'v1'\n" + comment + "\n\nk:'v'")
                .asObject().get(1).getComment(CommentType.HEADER));
    }

    @Test
    public void parse_readsUntilLastEmptyLine_asHeader() {
        final String header = """
            // header part 1
            // header part 2
            
            // header part 3""";
        final String json = """

            // comment of "key"
            key: 'value'""";
        final String expected = """
            header part 1
            header part 2
            
            header part 3""";

        final JsonValue parsed = this.parse(header + "\n" + json);
        assertEquals(expected, parsed.getComment(CommentType.HEADER));
    }

    @Test
    public void parse_preservesEmptyLines_ignoringComments() throws IOException {
        final String json = """

             key:
               'value'

             another:

               # comment
               'value'

             k3: 'v3', k4: 'v4'


             # and
             finally: 'value'
             """;
        final String expected = """
             {
               "key":
                 "value",

               "another":

                 "value",

               "k3": "v3", "k4": "v4",


               "finally": "value"
             }""";
        final var actual = this.parse(json).toString("json");
        assertEquals(expected.replace("\r", ""), actual.replace("\r", ""));
    }

    @Override
    protected JsonValue parse(final String json) {
        return new DjsParser(PositionTrackingReader.fromString(json)).parse();
    }
}
