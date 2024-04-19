package xjs.data.comments;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.data.serialization.JsonContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CommentDataTest {

    @BeforeAll
    static void setup() {
        JsonContext.setEol("\n");
    }

    @AfterAll
    static void teardown() {
        JsonContext.setEol(System.getProperty("line.separator"));
    }

    @Test
    public void format_generatesHashComment() {
        final String comment = "here's a comment";
        final String expected = "# here's a comment";

        Assertions.assertEquals(expected, format(CommentStyle.HASH, comment));
    }

    @Test
    public void format_generatesLineComment() {
        final String comment = "here's another comment";
        final String expected = "// here's another comment";

        assertEquals(expected, format(CommentStyle.LINE, comment));
    }

    @Test
    public void format_generatesBlockComment() {
        final String comment = "here's a block comment";
        final String expected = "/* here's a block comment */";

        assertEquals(expected, format(CommentStyle.BLOCK, comment));
    }

    @Test
    public void format_generatesMultilineHash() {
        final String comment = "here's a comment\nwith multiple lines";
        final String expected = "# here's a comment\n# with multiple lines";

        assertEquals(expected, format(CommentStyle.HASH, comment));
    }

    @Test
    public void format_generatesMultilineLine() {
        final String comment = "here's a comment\nwith multiple lines";
        final String expected = "// here's a comment\n// with multiple lines";

        assertEquals(expected, format(CommentStyle.LINE, comment));
    }

    @Test
    public void format_generatesMultilineBlock() {
        final String comment = "here's a block comment\nwith multiple lines";
        final String expected = "/*\n * here's a block comment\n * with multiple lines\n */";

        assertEquals(expected, format(CommentStyle.BLOCK, comment));
    }

    @Test
    public void formatLine_handlesCarriageReturn() {
        final String comment = "here's a comment\r\nwith multiple lines";
        final String expected = "// here's a comment\n// with multiple lines";

        assertEquals(expected, format(CommentStyle.LINE, comment));
    }

    @Test
    public void formatBlock_handlesCarriageReturn() {
        final String comment = "here's a comment\r\nwith multiple lines";
        final String expected = "/*\n * here's a comment\n * with multiple lines\n */";

        assertEquals(expected, format(CommentStyle.BLOCK, comment));
    }

    @Test
    public void formatIndented_doesNotIndent_singleLine_lineComment() {
        final String comment = "here's a comment";
        final String expected = "# here's a comment";

        Assertions.assertEquals(expected, format(CommentStyle.HASH, comment, " ", 2));
    }

    @Test
    public void formatIndented_doesNotIndent_singleLine_blockComment() {
        final String comment = "here's a block comment";
        final String expected = "/* here's a block comment */";

        assertEquals(expected, format(CommentStyle.BLOCK, comment, " ", 2));
    }

    @Test
    public void formatIndented_doesIndent_multiline_lineComment() {
        final String comment = "here's a comment\nwith multiple lines";
        final String expected = "# here's a comment\n  # with multiple lines";

        assertEquals(expected, format(CommentStyle.HASH, comment, " ", 2));
    }

    @Test
    public void format_doesIndent_multiline_blockComment() {
        final String comment = "here's a block comment\nwith multiple lines";
        final String expected = "/*\n   * here's a block comment\n   * with multiple lines\n   */";

        assertEquals(expected, format(CommentStyle.BLOCK, comment, " ", 2));
    }

    private static String format(final CommentStyle style, final String text) {
        return format(style, text, "", 0);
    }
    
    private static String format(
            final CommentStyle style, final String text, final String separator, final int level) {
        final CommentData data = new CommentData();
        data.append(new Comment(style, text));
        
        final StringBuilder sb = new StringBuilder();
        try {
            data.writeTo(sb, separator, level, "\n");
        } catch (final Exception ignored) {}
        return sb.toString();
    }
}
