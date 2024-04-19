package xjs.data.serialization.token;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a series of tokens encapsulated recursively.
 *
 * <p>Unlike the regular {@link TokenStream stream variant},
 * the ContainerToken subclass includes all container types:
 * {@link TokenType#BRACES}, {@link TokenType#BRACKETS}, and
 * {@link TokenType#PARENTHESES}.
 *
 * <p>For example, the following tokens:
 *
 * <pre>{@code
 *   (a[b]c)
 * }</pre>
 *
 * <b>here's some bold text</b>
 *
 * <p>Would be represented as the following container token:
 *
 * <pre>{@code
 *   PARENTHESES([
 *     WORD('a'),
 *     BRACKETS([
 *       WORD('b')
 *     ]),
 *     WORD('c')
 *   ])
 * }</pre>
 */
public class ContainerToken extends TokenStream {

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param reference A reference to the original source of this token.
     * @param start     The inclusive start index of this token.
     * @param end       The exclusive end index of this token.
     * @param line      The inclusive line number of this token.
     * @param lastLine  The inclusive end line number of this token.
     * @param offset    The column of the start index.
     * @param type      The type of token.
     * @param tokens    A known set of tokens to be written into.
     */
    public ContainerToken(
            final String reference, final int start, final int end,
            final int line, final int lastLine, final int offset,
            final TokenType type, final List<Token> tokens) {
        super(reference, start, end, line, lastLine, offset, type, tokens);
    }

    /**
     * Constructs a new container token with effectively no scope.
     *
     * @param type   The type of token.
     * @param tokens A known set of tokens to be written into.
     */
    public ContainerToken(final TokenType type, final List<Token> tokens) {
        super("", 0, 0, 0, 0, 0, type, tokens);
    }

    public Token get(final int i) {
        return this.tokens.get(i);
    }

    public int size() {
        return this.tokens.size();
    }

    @ApiStatus.Experimental
    public @Nullable ContainerToken.Lookup lookup(final char symbol, final boolean exact) {
        return this.lookup(symbol, 0, exact);
    }

    @ApiStatus.Experimental
    public @Nullable ContainerToken.Lookup lookup(final char symbol, final int fromIndex, final boolean exact) {
        for (int i = fromIndex; i < this.tokens.size(); i++) {
            final Token token = this.tokens.get(i);
            if (token.isSymbol(symbol)) {
                final Lookup result = new Lookup(token, i);
                if (exact && (result.followsOtherSymbol() || result.precedesOtherSymbol())) {
                    return this.lookup(symbol, i, true);
                }
                return result;
            }
        }
        return null;
    }

    @ApiStatus.Experimental
    public @Nullable ContainerToken.Lookup lookup(final String symbol, final boolean exact) {
        return this.lookup(symbol, 0, exact);
    }

    @ApiStatus.Experimental
    public @Nullable ContainerToken.Lookup lookup(final String symbol, final int fromIndex, final boolean exact) {
        char c = symbol.charAt(0);
        final Lookup firstLookup = this.lookup(c, fromIndex, false);
        if (firstLookup == null) {
            return null;
        }
        if (exact && firstLookup.followsOtherSymbol()) {
            return this.lookup(symbol, fromIndex + 1, true);
        }
        Lookup previousLookup = firstLookup;
        Lookup nextLookup = firstLookup;
        for (int i = 1; i < symbol.length(); i++) {
            c = symbol.charAt(i);
            nextLookup = this.lookup(c, fromIndex + i, false);
            if (nextLookup == null) {
                return null;
            } else if (nextLookup.token.start() != previousLookup.token.end()
                    || nextLookup.index - previousLookup.index != 1) {
                return this.lookup(symbol, firstLookup.index + 1, exact);
            }
            previousLookup = nextLookup;
        }
        if (exact && nextLookup.precedesOtherSymbol()) {
            return this.lookup(symbol, nextLookup.index + 1, true);
        }
        return firstLookup;
    }

    @ApiStatus.Experimental
    public TokenStream slice(final int s, final int e) {
        if (s == 0 && e == this.tokens.size()) {
            return this;
        } else if (s < 0 || e > this.tokens.size()) {
            throw new IllegalArgumentException(
                String.format("slice: (%d, %d) exceeds markers: (0, %d)", s, e, tokens.size()));
        }
        final Token first = this.tokens.get(s);
        final Token last = this.tokens.get(e);
        final List<Token> slice = this.tokens.subList(s, e);
        return new ContainerToken(
            this.reference.toString(), first.start(), last.end(), first.line(), last.line(), first.offset(), TokenType.OPEN, slice);
    }

    public class Lookup {
        public final Token token;
        public final int index;

        protected Lookup(final Token token, final int index) {
            this.token = token;
            this.index = index;
        }

        public boolean followsOtherSymbol() {
            if (this.index > 0) {
                final Token previous = tokens.get(this.index - 1);
                return previous.type() == TokenType.SYMBOL && this.token.start() == previous.end();
            }
            return false;
        }

        public boolean precedesOtherSymbol() {
            if (this.index < tokens.size() - 1) {
                final Token following = tokens.get(this.index + 1);
                return following.type() == TokenType.SYMBOL && this.token.end() == following.start();
            }
            return false;
        }

        @ApiStatus.Experimental
        public boolean isFollowedBy(final char symbol) {
            if (tokens.size() > this.index + 1) {
                final Token following = tokens.get(index + 1);
                return this.token.end() == following.start()
                    && following.isSymbol(symbol);
            }
            return false;
        }
    }
}
