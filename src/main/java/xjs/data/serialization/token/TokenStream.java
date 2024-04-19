package xjs.data.serialization.token;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * For example, the following <em>token</em> is a single
 * sequence of tokens and is thus eligible to be represented
 * by this object:
 *
 * <pre>
 *   { k : v }
 * </pre>
 */
public class TokenStream extends Token implements Iterable<Token>, Closeable {
    protected final List<Token> tokens;
    protected final List<Token> unmodifiableView;
    protected volatile @Nullable Tokenizer tokenizer;
    public final CharSequence reference;

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
     * @param tokens    A list of any known tokens, in order.
     */
    @Deprecated // todo: generate all container tokens lazily
    protected TokenStream(final CharSequence reference, final int start, final int end,
                          final int line, final int lastLine, final int offset,
                          final TokenType type, final List<Token> tokens) {
        super(start, end, line, lastLine, offset, type);
        this.reference = reference;
        this.tokens = new ArrayList<>(tokens);
        this.unmodifiableView = Collections.unmodifiableList(this.tokens);
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
        this.unmodifiableView = Collections.unmodifiableList(this.tokens);
        this.tokenizer = tokenizer;
        this.reference = tokenizer.reader.getFullText();
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

    protected void readToEnd() {
        final Tokenizer tokenizer = this.tokenizer;
        if (tokenizer != null) {
            synchronized (this) {
                this.forEach(token -> {});
            }
        }
    }

    private void stringifySingle(
            final StringBuilder sb, final Token token, final int level, final boolean readToEnd) {
        this.writeNewLine(sb, level);
        sb.append(token.type()).append('(');
        if (token.type() == TokenType.NUMBER) {
            sb.append(((NumberToken) token).number);
        } else if (token instanceof TokenStream) {
            sb.append(((TokenStream) token).stringify(level + 1, readToEnd));
        } else {
            final String text = token.textOf(this.reference)
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
        return this.unmodifiableView;
    }

    @Override
    public Itr iterator() {
        return new Itr();
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

    public class Itr implements Iterator<Token> {
        protected final Tokenizer tokenizer;
        protected Token next;
        protected int elementIndex;

        protected Itr() {
            this.tokenizer = TokenStream.this.tokenizer;
            this.elementIndex = -1;
            this.read();
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public Token next() {
            final Token current = this.next;
            this.read();
            return current;
        }

        public Token peekOrParent() {
            return this.next != null ? this.next : TokenStream.this;
        }

        protected void read() {
            this.elementIndex++;
            final Token next = this.peek(1);
            this.next = next;
            if (next != null) {
                if (next.end() > TokenStream.this.end) {
                    TokenStream.this.end = next.end();
                }
                if (next.lastLine() > TokenStream.this.lastLine) {
                    TokenStream.this.lastLine = next.lastLine();
                }
            }
            this.tryClose();
        }

        protected void tryClose() {
            if (this.tokenizer != null && this.next == null) {
                try {
                    this.tokenizer.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                TokenStream.this.tokenizer = null;
            }
        }

        public void skipTo(final int index) {
            final int amount = index - this.elementIndex;
            this.next = this.peek(amount + 1);
            this.elementIndex = index;
            this.tryClose();
        }

        public void skip(final int amount) {
            this.next = this.peek(amount + 1);
            this.elementIndex = this.elementIndex + amount;
            this.tryClose();
        }

        public int getIndex() {
            return this.elementIndex;
        }

        public String getText() {
            final Token t = this.peekOrParent();
            return this.getText(t.start(), t.end());
        }

        public String getText(final int s, final int e) {
            return this.getReference().subSequence(s, e).toString();
        }

        public CharSequence getReference() {
            return reference;
        }

        public TokenStream getParent() {
            return TokenStream.this;
        }

        public @Nullable Token peek() {
            return this.next;
        }

        public Token peek(final int amount, final Token defaultValue) {
            final Token peek = this.peek(amount);
            return peek != null ? peek : defaultValue;
        }

        public @Nullable Token peek(final int amount) {
            Token next = null;
            final int peekIndex = this.elementIndex + amount - 1;
            if (peekIndex >= 0 && peekIndex < tokens.size()) {
                return tokens.get(peekIndex);
            }
            if (this.tokenizer == null) {
                return null;
            }
            while (tokens.size() < this.elementIndex + amount) {
                try {
                    next = this.tokenizer.single();
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (next == null) {
                    return null;
                }
                tokens.add(next);
            }
            return next;
        }
    }
}
