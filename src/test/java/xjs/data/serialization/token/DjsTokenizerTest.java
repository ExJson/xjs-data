package xjs.data.serialization.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xjs.data.comments.CommentStyle;
import xjs.data.StringType;
import xjs.data.exception.SyntaxException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DjsTokenizerTest {

    @Test
    public void single_parsesLineComment() {
        final String reference = "// Hello, world!";
        assertEquals(
            comment(reference, CommentStyle.LINE, "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_parsesHashComment() {
        final String reference = "# Hello, world!";
        assertEquals(
            comment(reference, CommentStyle.HASH, "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_parseBlockComment() {
        final String reference = "/*\nHello\nworld!\n*/";
        assertEquals(
            comment(reference, CommentStyle.BLOCK, "Hello\nworld!"),
            single(reference));
    }

    @Test
    public void single_parsesDoubleQuote() {
        final String reference = "\"Hello, world!\"";
        assertEquals(
            string(reference, StringType.DOUBLE, "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_parsesSingleQuote() {
        final String reference = "'Hello, world!'";
        assertEquals(
            string(reference, StringType.SINGLE, "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_parsesTripleQuote() {
        final String reference = "'''\nHello\nworld!\n'''";
        assertEquals(
            string(reference, StringType.MULTI, "Hello\nworld!"),
            single(reference));
    }

    @Test
    public void single_parsesInteger() {
        final String reference = "1234";
        assertEquals(
            number(reference, 1234),
            single(reference));
    }

    @Test
    public void single_parsesDecimal() {
        final String reference = "1234.5";
        assertEquals(
            number(reference, 1234.5),
            single(reference));
    }

    @Test
    public void single_parsesNegativeInteger() {
        final String reference = "-1234";
        assertEquals(
            number(reference, -1234),
            single(reference));
    }

    @Test
    public void single_parsesNegativeDecimal() {
        final String reference = "-1234.5";
        assertEquals(
            number(reference, -1234.5),
            single(reference));
    }

    @Test
    public void single_parsesMinus_withoutFollowingNumber_asSymbol() {
        final String reference = "-.1";
        assertEquals(
            symbol("-", '-'),
            single(reference));
    }

    @Test
    public void single_parsesScientificNumber() {
        final String reference = "1234.5E6";
        assertEquals(
            number(reference, 1234.5E6),
            single(reference));
    }

    @Test
    public void single_parsesScientificNumber_withExplicitSign() {
        final String reference = "1234.5e+6";
        assertEquals(
            number(reference, 1234.5E+6),
            single(reference));
    }

    @Test // cannot support splitting tokens without peek or side effects
    public void single_parsesSignAfterNumber_asWord() {
        final String reference = "1234e+";
        assertEquals(
            token(TokenType.WORD, 0, 6),
            single(reference));
    }

    @Test
    public void single_parsesLeadingZero_asWord() {
        final String reference = "01234";
        assertEquals(
            token(TokenType.WORD, 0, 5),
            single(reference));
    }

    @Test
    public void single_parsesLeadingZero_withDecimal_asNumber() {
        final String reference = "0.1234";
        assertEquals(
            number(reference, 0.1234),
            single(reference));
    }

    @Test
    public void single_parsesSingleZero_asNumber() {
        final String reference = "0";
        assertEquals(
            number(reference, 0),
            single(reference));
    }

    @Test
    public void single_parsesSingleZero_withDecimal_asNumber() {
        final String reference = "0.";
        assertEquals(
            number(reference, 0),
            single(reference));
    }

    @Test
    public void single_parsesBreak() {
        final String reference = "\n";
        assertEquals(
            token(reference, TokenType.BREAK),
            single(reference));
    }

    @ValueSource(strings = {"+", "-", "<", ">", "=", ":", "{", "}", "[", "]", "(", ")"})
    @ParameterizedTest
    public void single_parsesSymbol(final String reference) {
        assertEquals(
            symbol(reference, reference.charAt(0)),
            single(reference));
    }

    @Test
    public void single_parsesWord() {
        final String reference = "word";
        assertEquals(
            token(TokenType.WORD, 0, 4),
            single(reference));
    }

    @Test
    public void single_skipsWhitespace() {
        final String reference = " \t \t \t 'Hello, world!'";
        assertEquals(
            string(StringType.SINGLE, 7, reference.length(), "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_readsContainerElements_asSymbols() {
        final String reference = " {hello}";
        assertEquals(
            symbol('{', 1, 2),
            single(reference));
    }
    
    @ValueSource(strings = {"'", "\"", "'''"})
    @ParameterizedTest
    public void single_doesNotTolerate_UnclosedQuote(final String quote) {
        final String reference = quote + "hello, world!";
        assertThrows(SyntaxException.class, () ->
            single(reference));
    }

    @Test
    public void single_doesNotTolerate_UnclosedMultiLineComment() {
        final String reference = "/*hello, world!";
        assertThrows(SyntaxException.class, () ->
            single(reference));
    }

    @Test
    public void containerize_readsSingleContainer() {
        final String reference = "{hello,world}";
        assertEquals(
            container(
                TokenType.OPEN,
                0,
                13,
                container(
                    TokenType.BRACES,
                    0,
                    13,
                    token(TokenType.WORD, 1, 6),
                    symbol(',', 6, 7),
                    token(TokenType.WORD, 7, 12))),
            DjsTokenizer.containerize(reference).readToEnd());
    }

    @Test
    public void containerize_readsNestedContainer() {
        final String reference = "{hello,[world]}";
        assertEquals(
            container(TokenType.OPEN, 0, 15,
                container(
                    TokenType.BRACES,
                    0,
                    15,
                    token(TokenType.WORD, 1, 6),
                    symbol(',', 6, 7),
                    container(
                        TokenType.BRACKETS,
                        7,
                        14,
                        token(TokenType.WORD, 8, 13)))),
            DjsTokenizer.containerize(reference).readToEnd());
    }

    @Test
    public void containerize_toleratesTrailingWhitespace() {
        final String reference = "{hello,world} \t";
        assertEquals(
            container(
                TokenType.OPEN,
                0,
                13,
                container(
                    TokenType.BRACES,
                    0,
                    13,
                    token(TokenType.WORD, 1, 6),
                    symbol(',', 6, 7),
                    token(TokenType.WORD, 7, 12))),
            DjsTokenizer.containerize(reference).readToEnd());
    }

    @Test
    public void containerize_doesNotTolerate_UnclosedContainer() {
        final String reference = "{[}";
        final SyntaxException e =
            assertThrows(SyntaxException.class, () ->
                DjsTokenizer.containerize(reference).readToEnd());
        assertTrue(e.getMessage().contains("Expected ']'"));
    }

    @Test
    public void stream_returnsLazilyEvaluatedTokens() {
        final TokenStream stream = DjsTokenizer.stream("1234");
        stream.iterator().next();
        assertEquals(1, stream.tokens.size());
    }
    
    private static Token single(final String reference) {
        try {
            return new DjsTokenizer(reference, false).next();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Token token(final String reference, final TokenType type) {
        return token(type, 0, reference.length());
    }

    private static Token comment(final String reference, final CommentStyle type, final String parsed) {
        return new CommentToken(0, reference.length(), 0, lines(reference), 0, type, parsed);
    }

    private static Token string(final String reference, final StringType type, final String parsed) {
        return new StringToken(0, reference.length(), 0, lines(reference), 0, type, parsed);
    }

    private static Token string(final StringType type, final int s, final int e, final String parsed) {
        return new StringToken(s, e, 0, s, type, parsed);
    }

    private static Token token(final TokenType type, final int s, final int e) {
        return new Token(s, e, 0, s, type);
    }

    private static Token number(final String reference, final double number) {
        return new NumberToken(0, reference.length(), 0, 0, number);
    }

    private static Token symbol(final String reference, final char symbol) {
        return symbol(symbol, 0, reference.length());
    }

    private static Token symbol(final char symbol, final int s, final int e) {
        return new SymbolToken(s, e, 0, s, symbol);
    }

    private static Token container(
            final TokenType type, final int s, final int e, final Token... tokens) {
        return new TokenStream(s, e, 0, 0, s, type, List.of(tokens));
    }

    private static int lines(final String reference) {
        return (int) reference.lines().count() - 1;
    }
}
