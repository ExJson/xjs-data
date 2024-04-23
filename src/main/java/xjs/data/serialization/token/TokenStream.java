package xjs.data.serialization.token;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents any sequence of other tokens.
 *
 * <p>For example, the following <em>token</em> is a single
 * sequence of tokens and is thus eligible to be represented
 * by this object:
 *
 * <pre>
 *   { k : v }
 * </pre>
 *
 * <p>When given a {@link Tokenizer} which is configured to
 * containerize its output, callers should be aware that the
 * spectrum of tokens returned includes the following types:
 * {@link TokenType#BRACES}, {@link TokenType#BRACKETS}, and
 * {@link TokenType#PARENTHESES}.
 *
 * <p>For example, the following tokens:
 *
 * <pre>{@code
 *   (a[b]c)
 * }</pre>
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
 *
 * <p>However, if the given {@link Tokenizer} is <em>not</em>
 * configured to containerize its output, the previous tokens
 * would be represented as follows:
 *
 * <pre>{@code
 *   OPEN([
 *     SYMBOL('('),
 *     WORD('a'),
 *     SYMBOL('['),
 *     WORD('b'),
 *     SYMBOL(']'),
 *     WORD('c'),
 *     SYMBOL(')')
 *   ])
 * }</pre>
 */
public class TokenStream extends Token implements Iterable<Token>, Closeable {
    protected final List<Token> tokens;
    protected volatile @Nullable Tokenizer tokenizer;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start     The inclusive start index of this token.
     * @param end       The exclusive end index of this token.
     * @param line      The inclusive line number of this token.
     * @param lastLine  The inclusive end line number of this token.
     * @param offset    The column of the start index.
     * @param type      The type of token.
     * @param tokens    A list of any known tokens, in order.
     */
    public TokenStream(final int start, final int end, final int line, final int lastLine,
                       final int offset, final TokenType type, final List<Token> tokens) {
        super(start, end, line, lastLine, offset, type);
        this.tokens = new ArrayList<>(tokens);
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param tokenizer A tokenizer for generating tokens OTF.
     * @param type      The type of token.
     */
    public TokenStream(final @NotNull Tokenizer tokenizer, final TokenType type) {
        super(tokenizer.reader.index, -1, tokenizer.reader.line, -1, tokenizer.reader.index, type);
        this.tokens = new ArrayList<>();
        this.tokenizer = tokenizer;
    }

    /**
     * Constructs a new Token object when streaming an existing token.
     *
     * @param tokenizer A tokenizer for generating tokens OTF.
     * @param from      The token source of this stream.
     * @param type      The type of token.
     */
    public TokenStream(final @NotNull Tokenizer tokenizer, final Token from, final TokenType type) {
        super(from.start(), -1, from.line(), -1, from.offset(), type);
        this.tokens = new ArrayList<>();
        this.tokenizer = tokenizer;
    }

    /**
     * Generates a String representation of the underlying tokens,
     * evaluating any un-parsed tokens, as needed.
     *
     * <p>For example, the following tokens:
     *
     * <pre>
     *   { k : v }
     * </pre>
     *
     * <p>Will be printed as follows:
     *
     * <pre>
     *   BRACES([
     *     WORD('k')
     *     SYMBOL(':')
     *     WORD('v')
     *   ])
     * </pre>
     */
    @ApiStatus.Experimental
    public String stringify() {
        return this.stringify(1, true);
    }

    protected String stringify(final int level, final boolean readToEnd) {
        if (readToEnd) {
            this.readToEnd();
        }
        final StringBuilder sb = new StringBuilder("[");
        final List<Token> copy = new ArrayList<>(this.tokens);
        for (final Token token : copy) {
            this.stringifySingle(sb, token, level, readToEnd);
        }
        if (this.tokenizer != null || this.tokens.size() != copy.size()) {
            this.writeNewLine(sb, level);
            sb.append("<reading...>");
        }
        this.writeNewLine(sb, level - 1);
        return sb.append("]").toString();
    }

    @VisibleForTesting
    public TokenStream readToEnd() {
        final Tokenizer tokenizer = this.tokenizer;
        if (tokenizer != null) {
            synchronized (this) {
                this.forEach(token -> {});
            }
        }
        return this;
    }

    private void stringifySingle(
            final StringBuilder sb, final Token token, final int level, final boolean readToEnd) {
        this.writeNewLine(sb, level);
        sb.append(token.type()).append('(');
        if (token instanceof NumberToken number) {
            sb.append(number.number);
        } else if (token instanceof TokenStream stream) {
            sb.append(stream.stringify(level + 1, readToEnd));
        } else if (token instanceof SymbolToken symbol) {
            sb.append('\'').append(symbol.symbol).append('\'');
        } else if (token.hasText()) {
            final String text = token.parsed()
                .replace("\n", "\\n").replace("\t", "\\t");
            sb.append('\'').append(text).append('\'');
        }
        sb.append(')');
    }

    private void writeNewLine(final StringBuilder sb, final int level) {
        sb.append('\n');
        sb.append(" ".repeat(Math.max(0, level)));
    }

    public List<Token> viewTokens() {
        return Collections.unmodifiableList(this.tokens);
    }

    public @Nullable Lookup lookup(final char symbol, final boolean exact) {
        return this.lookup(symbol, 0, exact);
    }

    public @Nullable Lookup lookup(final char symbol, final int fromIndex, final boolean exact) {
        final Itr itr = this.iterator();
        itr.skipTo(fromIndex);
        while (itr.hasNext()) {
            final Token token = itr.next();
            if (token.isSymbol(symbol)) {
                final Lookup result = new Lookup(token, itr.getIndex());
                if (exact && (result.followsOtherSymbol() || result.precedesOtherSymbol())) {
                    return this.lookup(symbol, itr.getIndex(), true);
                }
                return result;
            }
        }
        return null;
    }

    public @Nullable Lookup lookup(final String symbol, final boolean exact) {
        return this.lookup(symbol, 0, exact);
    }

    public @Nullable Lookup lookup(final String symbol, final int fromIndex, final boolean exact) {
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

    @Override
    public Itr iterator() {
        final char closer = switch (this.type) {
            case PARENTHESES -> ')';
            case BRACES -> '}';
            case BRACKETS -> ']';
            default -> '\u0000';
        };
        return new Itr(closer);
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TokenStream) {
            return super.equals(o)
                && this.lastLine == ((TokenStream) o).lastLine
                && this.tokens.equals(((TokenStream) o).tokens);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.stringify(1, false);
    }

    @Override
    public void close() throws IOException {
        final Tokenizer tokenizer;
        synchronized (this) {
            tokenizer = this.tokenizer;
        }
        if (tokenizer != null) {
            tokenizer.close();
        }
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

    public class Itr implements Iterator<Token> {
        protected final char closer;
        protected Token next;
        protected boolean ready;
        protected int elementIndex;

        protected Itr(final char closer) {
            this.closer = closer;
            this.elementIndex = -1;
            this.ready = true;
        }

        @Override
        public boolean hasNext() {
            if (this.ready) {
                this.read();
                this.ready = false;
            }
            return this.next != null;
        }

        @Override
        public Token next() {
            if (this.ready) {
                this.read();
            }
            final Token current = this.next;
            this.elementIndex++;
            this.ready = true;
            return current;
        }

        protected void read() {
            this.next = this.peek(1);
        }

        public void skipTo(final int index) {
            this.elementIndex = index - 1;
            this.ready = true;
        }

        public void skip(final int amount) {
            this.elementIndex = this.elementIndex + amount;
            this.ready = true;
        }

        public int getIndex() {
            return this.elementIndex;
        }

        public TokenStream getParent() {
            return TokenStream.this;
        }

        public @Nullable Token peek() {
            return this.peek(1);
        }

        public Token peek(final int amount, final Token defaultValue) {
            final Token peek = this.peek(amount);
            return peek != null ? peek : defaultValue;
        }

        public @Nullable Token peek(final int amount) {
            final int peekIndex = this.elementIndex + amount;
            if (peekIndex >= 0 && peekIndex < tokens.size()) {
                return tokens.get(peekIndex);
            }
            final Tokenizer tokenizer = TokenStream.this.tokenizer;
            if (tokenizer == null) {
                return null;
            }
            Token next = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
            while (tokens.size() < this.elementIndex + amount + 1) {
                if (next instanceof TokenStream stream) {
                    stream.readToEnd();
                    this.expandToFit(stream);
                }
                try {
                    next = tokenizer.next();
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (next == null) {
                    if (this.closer != '\u0000') {
                        final PositionTrackingReader reader = tokenizer.getReader();
                        throw SyntaxException.expected(
                            this.closer, reader.line, reader.column);
                    }
                    tryClose(tokenizer);
                    TokenStream.this.tokenizer = null;
                    return null;
                }
                this.expandToFit(next);
                if (next.isSymbol(this.closer)) {
                    TokenStream.this.tokenizer = null;
                    return null;
                }
                tokens.add(next);
            }
            return next;
        }

        protected void expandToFit(final Token t) {
            if (t.end() > TokenStream.this.end) {
                TokenStream.this.end = t.end();
            }
            if (t.lastLine() > TokenStream.this.lastLine) {
                TokenStream.this.lastLine = t.lastLine();
            }
        }

        protected static void tryClose(final Tokenizer tokenizer) {
            if (tokenizer != null) {
                try {
                    tokenizer.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
