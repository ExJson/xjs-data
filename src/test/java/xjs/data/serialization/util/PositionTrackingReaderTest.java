package xjs.data.serialization.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xjs.data.comments.CommentStyle;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.token.CommentToken;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PositionTrackingReaderTest {

    private static final int MEDIUM_STRING = 1024;
    private static final int LARGE_STRING = 2048;
    private static final int GIANT_STRING = 16_384;

    private static final int[] STRING_SIZES = {
        MEDIUM_STRING, LARGE_STRING, GIANT_STRING
    };

    private static final int MINIMUM_BUFFER = 8;
    private static final int SMALL_BUFFER = 128;
    private static final int NORMAL_BUFFER = 1024;
    private static final int LARGE_BUFFER = 2048;

    private static final int[] BUFFER_SIZES = {
        MINIMUM_BUFFER, SMALL_BUFFER, NORMAL_BUFFER, LARGE_BUFFER
    };

    private static final String[] SPECIAL_CHARS = {
        "!", "@", "#", "$", "%", "^", "&", "*", "(", ")",
        "{", "}", "[", "]", ",", "\"", "'", ":", "<", ">",
        "\uD83D\uDE00", "\uD83E\uDD17", "\uD83E\uDD28"
    };

    @RepeatedTest(25)
    public void reader_readsExactString() {
        for (final Sample sample : TestData.generateMixedSet()) {
            for (PositionTrackingReader reader : sample.getAllReaders()) {
                assertEquals(sample.text, parseFullText(reader),
                    reader.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void reader_capturesFullText() throws IOException {
        for (final Sample sample : TestData.generateMixedSet()) {
            for (PositionTrackingReader reader : sample.getAllReaders()) {
                assertEquals(sample.text, reader.readToEnd(),
                    reader.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void reader_tracksIndexExactly() throws IOException {
        final Sample sample = Sample.generate(GIANT_STRING, NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            int index = 0;
            do {
                assertEquals(index++, reader.index,
                    reader.getClass().getSimpleName());
                reader.read();
            } while (!reader.isEndOfText());
        }
    }

    @Test
    public void reader_tracksColumnExactly() throws IOException {
        final String columns = "0123\n012345\n01234\n".repeat(100);
        final Sample sample = new Sample(columns, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            do {
                if (reader.current != '\n') {
                    final int expectedColumn =
                        Integer.parseInt("" + (char) reader.current);
                    assertEquals(expectedColumn, reader.column,
                        reader.getClass().getSimpleName());
                }
                reader.read();
            } while (!reader.isEndOfText());
        }
    }

    @Test
    public void reader_tracksLinesExactly() throws IOException {
        final String columns = "0123\n012345\n01234\n".repeat(100);
        final Sample sample = new Sample(columns, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            while (!reader.isEndOfText()) {
                reader.read();
            }
            assertEquals(299, reader.line,
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readIf_doesNotAdvanceOnMismatch() throws IOException {
        final Sample sample = new Sample("abc", NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertFalse(reader.readIf('x'));
            assertTrue(reader.readIf('a'));
            assertFalse(reader.readIf('y'));
            assertTrue(reader.readIf('b'));
            assertFalse(reader.readIf('z'));
            assertTrue(reader.readIf('c'));
            assertTrue(reader.isEndOfText());
        }
    }

    @Test
    public void expect_throwsOnMismatch() throws IOException {
        final Sample sample = new Sample("abc", NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertFalse(reader.readIf('x'));
            assertTrue(reader.readIf('a'));
            assertThrows(SyntaxException.class, () ->
                reader.expect('y'));
        }
    }

    @Test
    public void capture_capturesExactly() throws IOException {
        final Sample sample = new Sample("1234567891011", MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertTrue(reader.readIf('1'));
            assertTrue(reader.readIf('2'));
            assertTrue(reader.readIf('3'));
            reader.startCapture();
            while (!reader.isEndOfText()) {
                reader.read();
            }
            assertEquals("4567891011", reader.endCapture(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void pauseCapture_doesNotMangleCapture() throws IOException {
        final Sample sample = new Sample("1234567891011", MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertTrue(reader.readIf('1'));

            reader.startCapture();
            assertTrue(reader.readIf('2'));
            assertTrue(reader.readIf('3'));

            reader.pauseCapture();
            assertTrue(reader.readIf('4'));
            assertTrue(reader.readIf('5'));

            reader.startCapture();
            while (!reader.isEndOfText()) {
                reader.read();
            }
            assertEquals("2367891011", reader.endCapture(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void endCaptureEarly_doesNotMangleCapture() throws IOException {
        final Sample sample = new Sample("1234567891011", MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertTrue(reader.readIf('1'));

            reader.startCapture();
            assertTrue(reader.readIf('2'));
            assertTrue(reader.readIf('3'));
            assertTrue(reader.readIf('4'));
            assertTrue(reader.readIf('5'));
            assertTrue(reader.readIf('6'));

            assertEquals("23456", reader.endCapture(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void skipToNL_returnsIndexOfNl() throws IOException {
        final Sample sample = new Sample("abc\n123", NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertEquals(3, reader.skipToNL());
        }
    }

    @Test
    public void skipToNL_returnsIndexOfEof() throws IOException {
        final Sample sample = new Sample("abc", NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertEquals(3, reader.skipToNL());
        }
    }

    @Test
    public void skipToNL_returnsEndOfNonWhitespace() throws IOException {
        final Sample sample = new Sample("abc \t\n123", NORMAL_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertEquals(3, reader.skipToNL());
        }
    }

    @Test
    public void readNumber_readsFullNumber() throws IOException {
        final Sample sample = new Sample("123456789", MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertEquals(123456789, reader.readNumber(),
                reader.getClass().getSimpleName());
        }
    }

    @ParameterizedTest
    @ValueSource(chars = {'"', '\"'})
    public void readQuoted_readsFullQuote(final char quote) throws IOException {
        final String text = "'Hello, world!'";
        final Sample sample = new Sample(quote + text + quote, MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertEquals(text, reader.readQuoted(quote),
                reader.getClass().getSimpleName());
        }
    }

    @ParameterizedTest
    @ValueSource(chars = {'"', '\''})
    public void readQuoted_readsEscapeChars(final char quote) throws IOException {
        final String text = "\\t\\n\\u1234";
        final Sample sample = new Sample(quote + text + quote, MINIMUM_BUFFER);
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            assertEquals("\t\n\u1234", reader.readQuoted(quote),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readBlockComment_readsExpandedBlock() throws IOException {
        final String text = """
            /*
             * expanded block
             */""";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            final CommentToken comment = reader.readBlockComment();
            assertEquals(CommentStyle.BLOCK, comment.commentStyle(),
                reader.getClass().getSimpleName());
            assertEquals("expanded block", comment.parsed(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readBlockComment_readsExpandedDocumentation() throws IOException {
        final String text = """
            /**
             * expanded documentation
             */""";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            final CommentToken comment = reader.readBlockComment();
            assertEquals(CommentStyle.MULTILINE_DOC, comment.commentStyle(),
                reader.getClass().getSimpleName());
            assertEquals("expanded documentation", comment.parsed(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readBlockComment_readsMultipleLines() throws IOException {
        final String text = "/**\n * line 1\n * line 2\n*/";
        final String expected = "line 1\nline 2";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            assertEquals(expected, reader.readBlockComment().parsed(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readBlockComment_ignoresCarriageReturns() throws IOException {
        final String text = "/**\r\n * 1\r\n * 2\r\n*/";
        final String expected = "1\n2";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            assertEquals(expected, reader.readBlockComment().parsed(),
                reader.getClass().getSimpleName());
        }
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
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            assertEquals(expected, reader.readBlockComment().parsed(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readBlockComment_ignoresIndentation_beforeAsterisk() throws IOException {
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
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            assertEquals(expected, reader.readBlockComment().parsed(),
                reader.getClass().getSimpleName());
        }
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
        final Sample sample = new Sample(text, NORMAL_BUFFER);
        
        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            assertEquals(expected, reader.readBlockComment().parsed(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readBlockComment_readsCollapsedBlock() throws IOException {
        final String text = "/* collapsed block */";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            final CommentToken comment = reader.readBlockComment();
            assertEquals(CommentStyle.BLOCK, comment.commentStyle(),
                reader.getClass().getSimpleName());
            assertEquals("collapsed block", comment.parsed(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readBlockComment_readsCollapsedDocumentation() throws IOException {
        final String text = "/** collapsed block */";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            final CommentToken comment = reader.readBlockComment();
            assertEquals(CommentStyle.MULTILINE_DOC, comment.commentStyle(),
                reader.getClass().getSimpleName());
            assertEquals("collapsed block", comment.parsed(),
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readBlockComment_withoutCloser_throwsException() throws IOException {
        final String text = "/* collapsed block\n1\n2 * /";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            assertThrows(SyntaxException.class, reader::readBlockComment,
                reader.getClass().getSimpleName());
        }
    }

    @Test
    public void readLineComment_capturesCommentText() throws IOException {
        final String text = "//comment";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            final CommentToken comment = reader.readLineComment();
            assertEquals(CommentStyle.LINE, comment.commentStyle());
            assertEquals("comment", comment.parsed());
        }
    }

    @Test
    public void readLineComment_ignoresFirstWhitespace() throws IOException {
        final String text = "// comment";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            final CommentToken comment = reader.readLineComment();
            assertEquals(CommentStyle.LINE, comment.commentStyle());
            assertEquals("comment", comment.parsed());
        }
    }

    @Test
    public void readLineComment_doesCaptureAdditionalLeadingWhitespace() throws IOException {
        final String text = "//  comment";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            final CommentToken comment = reader.readLineComment();
            assertEquals(CommentStyle.LINE, comment.commentStyle());
            assertEquals(" comment", comment.parsed());
        }
    }

    @Test
    public void readLineComment_ignoresTrailingWhitespace() throws IOException {
        final String text = "//comment  ";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            reader.expect('/');

            final CommentToken comment = reader.readLineComment();
            assertEquals(CommentStyle.LINE, comment.commentStyle());
            assertEquals("comment", comment.parsed());
        }
    }

    @Test
    public void readHashComment_capturesCommentText() throws IOException {
        final String text = "#comment";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            final CommentToken comment = reader.readHashComment();
            assertEquals(CommentStyle.HASH, comment.commentStyle());
            assertEquals("comment", comment.parsed());
        }
    }

    @Test
    public void readHashComment_ignoresFirstWhitespace() throws IOException {
        final String text = "# comment";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            final CommentToken comment = reader.readHashComment();
            assertEquals(CommentStyle.HASH, comment.commentStyle());
            assertEquals("comment", comment.parsed());
        }
    }

    @Test
    public void readHashComment_doesCaptureAdditionalLeadingWhitespace() throws IOException {
        final String text = "#  comment";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            final CommentToken comment = reader.readHashComment();
            assertEquals(CommentStyle.HASH, comment.commentStyle());
            assertEquals(" comment", comment.parsed());
        }
    }

    @Test
    public void readHashComment_ignoresTrailingWhitespace() throws IOException {
        final String text = "#comment  ";
        final Sample sample = new Sample(text, NORMAL_BUFFER);

        for (final PositionTrackingReader reader : sample.getAllReaders()) {
            final CommentToken comment = reader.readHashComment();
            assertEquals(CommentStyle.HASH, comment.commentStyle());
            assertEquals("comment", comment.parsed());
        }
    }

    private static String parseFullText(final PositionTrackingReader reader) {
        final StringBuilder sb = new StringBuilder();
        try {
            while (reader.current != -1) {
                sb.append((char) reader.current);
                reader.read();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private record TestData(List<Sample> samples) implements Iterable<Sample> {
        static TestData generateMixedSet() {
            final List<Sample> samples = new ArrayList<>();
            for (final int stringSize : STRING_SIZES) {
                for (final int bufferSize : BUFFER_SIZES) {
                    samples.add(Sample.generate(stringSize, bufferSize));
                }
            }
            return new TestData(samples);
        }

        @NotNull
        @Override
        public Iterator<Sample> iterator() {
            return this.samples.iterator();
        }
    }

    private record Sample(String text, int bufferSize) {
        static Sample generate(final int stringSize, final int bufferSize) {
            return new Sample(generateString(stringSize), bufferSize);
        }

        static String generateString(final int stringSize) {
            return generateString(stringSize, RandomUtils.nextInt(1, stringSize));
        }

        static String generateString(final int stringSize, final int specialChars) {
            final StringBuilder sb = new StringBuilder();
            final int numGroups = specialChars + 1;
            final int groupSize = (stringSize / numGroups) - 1;
            for (int i = 0; i < numGroups; i++) {
                sb.append(RandomStringUtils.random(groupSize, true, true));
                sb.append(SPECIAL_CHARS[RandomUtils.nextInt(0, SPECIAL_CHARS.length)]);
            }
            return sb.toString();
        }

        List<PositionTrackingReader> getAllReaders() {
            return List.of(this.getStringReader(), this.getCharBufferedReader());
        }

        PositionTrackingReader getStringReader() {
            return PositionTrackingReader.fromString(this.text);
        }

        PositionTrackingReader getCharBufferedReader() {
            try {
                return PositionTrackingReader.fromIs(this.getInputStream(), this.bufferSize, true);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        InputStream getInputStream() {
            return new ByteArrayInputStream(this.text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
