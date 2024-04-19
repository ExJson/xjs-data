package xjs.performance.experimental.token;

import org.junit.jupiter.api.Test;
import xjs.data.comments.CommentStyle;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.token.CommentToken;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ExperimentalCommentTokenParserTest {

    @Test
    public void readBlockComment_readsExpandedBlock() throws IOException {
        final String text = """
            /*
             * expanded block
             */""";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        final CommentToken comment = parser.readBlockComment();
        assertEquals(CommentStyle.BLOCK, comment.commentStyle());
        assertEquals("expanded block", comment.parsed());
    }

    @Test
    public void readBlockComment_readsExpandedDocumentation() throws IOException {
        final String text = """
            /**
             * expanded documentation
             */""";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        final CommentToken comment = parser.readBlockComment();
        assertEquals(CommentStyle.MULTILINE_DOC, comment.commentStyle());
        assertEquals("expanded documentation", comment.parsed());
    }

    @Test
    public void readBlockComment_readsMultipleLines() throws IOException {
        final String text = "/**\n * line 1\n * line 2\n*/";
        final String expected = "line 1\nline 2";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        assertEquals(expected, parser.readBlockComment().parsed());
    }

    @Test
    public void readBlockComment_ignoresCarriageReturns() throws IOException {
        final String text = "/**\r\n * 1\r\n * 2\r\n*/";
        final String expected = "1\n2";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        assertEquals(expected, parser.readBlockComment().parsed());
    }

    @Test
    public void readBlockComment_preservesIndentation_afterAsterisk() throws IOException {
        final String text = """
            /**
             * 0
             *  1
             *   2
             */""";
        final String expected = """
            0
             1
              2""";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        assertEquals(expected, parser.readBlockComment().parsed());
    }

    @Test
    public void readBlockComment_preservesIndentation_beforeAsterisk() throws IOException {
        final String text = """
            /*
             * 0
              * 1
               * 2
             */""";
        final String expected = """
            0
             1
              2""";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        assertEquals(expected, parser.readBlockComment().parsed());
    }

    @Test
    public void readBlockComment_preservesEmptyLines() throws IOException {
        final String text = """
            /*
             * line 1
             
             *
             
             * line 2
             */""";
        final String expected = """
            line 1
            
            
            
            line 2""";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        assertEquals(expected, parser.readBlockComment().parsed());
    }

    @Test
    public void readBlockComment_readsCollapsedBlock() throws IOException {
        final String text = "/* collapsed block */";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        final CommentToken comment = parser.readBlockComment();
        assertEquals(CommentStyle.BLOCK, comment.commentStyle());
        assertEquals("collapsed block", comment.parsed());
    }

    @Test
    public void readBlockComment_readsCollapsedDocumentation() throws IOException {
        final String text = "/** collapsed block */";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        final CommentToken comment = parser.readBlockComment();
        assertEquals(CommentStyle.MULTILINE_DOC, comment.commentStyle());
        assertEquals("collapsed block", comment.parsed());
    }

    @Test
    public void readBlockComment_withoutCloser_throwsException() throws IOException {
        final String text = "/* collapsed block\n1\n2 * /";
        final PositionTrackingReader reader = PositionTrackingReader.fromString(text);
        final ExperimentalCommentTokenParser parser = new ExperimentalCommentTokenParser(reader);
        reader.expect('/');

        assertThrows(SyntaxException.class, parser::readBlockComment);
    }
}
