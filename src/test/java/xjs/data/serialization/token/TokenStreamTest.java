package xjs.data.serialization.token;

import org.junit.jupiter.api.Test;
import xjs.data.exception.SyntaxException;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TokenStreamTest {

    @Test
    public void stringify_printsTokenType_andFullText() {
        TokenStream s = DjsTokenizer.stream("k:'v'//eol");
        s.lookup(':', false);
        TokenStream.Itr itr = s.iterator();
        itr.next();
        itr.next();
        itr.next();

        s = DjsTokenizer.containerize("{}\n//eol");
        s.lookup(':', false);
        itr = s.iterator();
        itr.next();
        itr.next();

        final TokenStream stream =
            DjsTokenizer.stream("word").preserveOutput();
        final String expected = """
            [
             WORD('word')
            ]""";
        assertEquals(expected, stream.stringify());
    }

    @Test
    public void stringify_escapesNewlines() {
        final TokenStream stream =
            DjsTokenizer.stream("'''1\n2'''");
        final String expected = """
            [
             STRING('1\\n2')
            ]""";
        assertEquals(expected, stream.stringify());
    }

    @Test
    public void stringify_printsAllTokens_inContainer() {
        final TokenStream stream =
            DjsTokenizer.stream("1 2 3");
        final String expected = """
            [
             NUMBER(1.0)
             NUMBER(2.0)
             NUMBER(3.0)
            ]""";
        assertEquals(expected, stream.stringify());
    }

    @Test
    public void stringify_recurses_intoOtherContainers() {
        final TokenStream stream =
            DjsTokenizer.containerize("1 [ 2.25 2.5 2.75 ] 3");
        final String expected = """
            [
             NUMBER(1.0)
             BRACKETS([
              NUMBER(2.25)
              NUMBER(2.5)
              NUMBER(2.75)
             ])
             NUMBER(3.0)
            ]""";
        assertEquals(expected, stream.stringify());
    }

    @Test
    public void toString_doesNotReadToEnd() {
        final TokenStream stream =
            DjsTokenizer.stream("1 2 3");
        stream.iterator();
        final String expected = """
            [
             <reading...>
            ]""";
        assertEquals(expected, stream.toString());
    }

    @Test
    public void viewTokens_isInternallyMutable() {
        final String reference = "1 2 3";
        final TokenStream stream = DjsTokenizer.stream(reference).preserveOutput();

        final List<Token> tokens = stream.viewTokens();
        assertTrue(tokens.isEmpty());

        stream.iterator().next();
        assertEquals(
            List.of(number(1, 0, 1)),
            tokens);
    }

    @Test
    public void viewTokens_isNotExternallyMutable() {
        final String reference = "1 2 3";
        final TokenStream stream = DjsTokenizer.stream(reference);

        assertThrows(UnsupportedOperationException.class,
            () -> stream.viewTokens().add(number(0, 0, 0)));
    }

    @Test
    public void next_lazilyEvaluatesTokens() {
        final String reference = "1 2 3";
        final TokenStream stream = DjsTokenizer.stream(reference);
        assertTrue(stream.source.isEmpty());

        final Iterator<Token> iterator = stream.iterator();
        assertTrue(stream.source.isEmpty());
        assertEquals(number(1, 0, 1), iterator.next());

        assertTrue(stream.source.isEmpty());
        assertEquals(number(2, 2, 3), iterator.next());

        assertTrue(stream.source.isEmpty());
        assertEquals(number(3, 4, 5), iterator.next());
    }

    @Test
    public void next_lazilyThrowsSyntaxException() {
        final String reference = "1 'hello";
        final TokenStream stream = DjsTokenizer.stream(reference);
        final Iterator<Token> iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertThrows(SyntaxException.class, iterator::next);
    }

    @Test
    public void peek_doesNotAdvanceIterator() {
        final String reference = "1 2 3 4";
        final TokenStream.Itr iterator =
            DjsTokenizer.stream(reference).iterator();

        assertEquals(number(1, 0, 1), iterator.next());

        assertEquals(number(2, 2, 3), iterator.peek());
        assertEquals(number(3, 4, 5), iterator.peek(2));
        assertEquals(number(4, 6, 7), iterator.peek(3));

        assertEquals(number(2, 2, 3), iterator.next());
        assertEquals(number(3, 4, 5), iterator.next());
        assertEquals(number(4, 6, 7), iterator.next());
    }

    @Test
    public void peek_toleratesReverseOrder() {
        final String reference = "1 2 3";
        final TokenStream.Itr iterator =
            DjsTokenizer.stream(reference).iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        assertEquals(number(3, 4, 5), iterator.next());

        assertEquals(number(3, 4, 5), iterator.peek(0));
        assertEquals(number(2, 2, 3), iterator.peek(-1));
        assertEquals(number(1, 0, 1), iterator.peek(-2));
    }

    @Test
    public void skipTo_advancesIterator() {
        final String reference = "1 2 3 4";
        final TokenStream stream = DjsTokenizer.stream(reference);
        final TokenStream.Itr iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        iterator.skipTo(2);

        assertEquals(number(3, 4, 5), iterator.next());
        assertEquals(number(4, 6, 7), iterator.next());
    }

    @Test
    public void skip_advancesIterator() {
        final String reference = "1 2 3 4";
        final TokenStream stream = DjsTokenizer.stream(reference);
        final TokenStream.Itr iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        iterator.skip(1);

        assertEquals(number(3, 4, 5), iterator.next());
        assertEquals(number(4, 6, 7), iterator.next());
    }

    @Test
    public void skipTo_toleratesReverseOrder() {
        final String reference = "1 2 3";
        final TokenStream stream = DjsTokenizer.stream(reference);
        final TokenStream.Itr iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        iterator.skipTo(0);

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        assertEquals(number(3, 4, 5), iterator.next());
    }

    @Test
    public void skip_toleratesReverseOrder() {
        final String reference = "1 2 3";
        final TokenStream stream = DjsTokenizer.stream(reference);
        final TokenStream.Itr iterator = stream.iterator();

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        iterator.skip(-2);

        assertEquals(number(1, 0, 1), iterator.next());
        assertEquals(number(2, 2, 3), iterator.next());
        assertEquals(number(3, 4, 5), iterator.next());
    }

    private static Token number(final double number, final int s, final int e) {
        return new NumberToken(s, e, 0, s, number);
    }

    private static Token token(final TokenType type, final int s, final int e) {
        return new Token(s, e, 0, s, type);
    }
}
