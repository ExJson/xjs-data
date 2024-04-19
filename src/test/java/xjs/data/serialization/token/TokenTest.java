package xjs.data.serialization.token;

import org.junit.jupiter.api.Test;
import xjs.data.StringType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TokenTest {

    @Test
    public void isSymbol_withMatchingSymbol_returnsTrue() {
        assertTrue(symbol(',', 0, 0).isSymbol(','));
    }

    @Test
    public void isSymbol_withMismatchingSymbol_returnsFalse() {
        assertFalse(symbol('x', 0, 0).isSymbol(','));
    }

    @Test
    public void isSymbol_withNonSymbol_returnsFalse() {
        assertFalse(number("0", 0).isSymbol('0'));
    }

    @Test
    public void isText_withMatchingText_returnsTrue() {
        final String reference = "test";
        assertTrue(token(reference, TokenType.WORD).isText(reference, "test"));
    }

    @Test
    public void isText_withMismatchingText_returnsFalse() {
        final String reference = "test";
        assertFalse(token(reference, TokenType.WORD).isText(reference, "false"));
    }

    @Test
    public void isText_withParsedToken_andMatchingText_returnsTrue() {
        assertTrue(parsed("test", TokenType.WORD).isText("test"));
    }

    @Test
    public void isText_withParsedToken_andMismatchingText_returnsFalse() {
        assertFalse(parsed("test", TokenType.WORD).isText("false"));
    }

    @Test
    public void isText_withoutReference_onNonParsedToken_throwsException() {
        assertThrows(UnsupportedOperationException.class,
            () -> token("test", TokenType.WORD).isText("test"));
    }

    @Test
    public void parsed_withReference_onNonParsedToken_returnsData() {
        final String reference = "test";
        assertEquals("test", token(reference, TokenType.WORD).parsed(reference));
    }

    @Test
    public void parsed_withoutReference_onNonParsedToken_throwsException() {
        assertThrows(UnsupportedOperationException.class,
            () -> token("test", TokenType.WORD).parsed());
    }

    @Test
    public void parsed_withoutReference_onParsedToken_returnsData() {
        assertEquals("test", parsed("test", TokenType.WORD).parsed());
    }

    @Test
    public void stringType_onNonStringToken_returnsNone() {
        assertEquals(StringType.NONE, token("test", TokenType.WORD).stringType());
    }

    @Test
    public void stringType_onStringToken_returnsStringType() {
        assertEquals(StringType.SINGLE,
            string("'test'", "test", StringType.SINGLE).stringType());
    }

    private static Token token(final String reference, final TokenType type) {
        return token(type, 0, reference.length());
    }

    private static Token parsed(final String parsed, final TokenType type) {
        return new ParsedToken(0, parsed.length(), 0, 0, type, parsed);
    }

    private static Token token(final TokenType type, final int s, final int e) {
        return new Token(s, e, 0, s, type);
    }

    private static Token number(final String reference, final double number) {
        return new NumberToken(0, reference.length(), 0, 0, number);
    }

    private static Token string(final String reference, final String text, final StringType type) {
        return new StringToken(0, reference.length(), 0, 0, type, text);
    }

    private static Token symbol(final char symbol, final int s, final int e) {
        return new SymbolToken(s, e, 0, s, symbol);
    }

    private static Token container(
            final String reference, final TokenType type, final int s, final int e, final Token... tokens) {
        return new ContainerToken(reference, s, e, 0, 0, s, type, List.of(tokens));
    }
}
