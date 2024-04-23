package xjs.data.serialization.parser;

import org.jetbrains.annotations.ApiStatus;
import xjs.data.JsonArray;
import xjs.data.JsonContainer;
import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.Tokenizer;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenType;
import xjs.data.serialization.util.BufferedStack;

import java.util.ArrayList;

/**
 * A basic parser type for processing {@link Token tokens} into JSON data.
 *
 * <p>The {@link TokenParser} class should be safe to work with both {@link
 * Tokenizer containerized token streams} and regular {@link TokenStream
 * token streams}.
 *
 * <p>The basic procedure for using it with {@link TokenStream token streams}
 * is as follows:
 *
 * <ul>
 *   <li>
 *       Use <code>{@link #read()}</code> to pull the next value out of the
 *       current iterator. Its output will be written into <code>{@link
 *       #current}</code>. If the iterator does not yield a value, <code>
 *       {@link #EMPTY_VALUE}</code> will be written instead. This guards
 *       against null safety problems downstream.
 *   </li>
 *   <li>
 *       Implementors may check to see if any tokens are left in the input
 *       by calling <code>{@link #isEndOfContainer()}</code>. Be aware that
 *       the exact implementation of this method may change at a later date.
 *       The method itself should always be safe to call and accurate.
 *   </li>
 *   <li>
 *       When working with {@link Tokenizer containerized token streams},
 *       implementors have the option to call <code>{@link #push()}</code>
 *       to open the current token and push its iterator onto the stack, and
 *       <code>{@link #pop()}</code> to pop the current iterator out of the
 *       stack.
 *   </li>
 * </ul>
 *
 * <p>For non-containerized inputs, most of the containerized methods accept
 * an optional parameter specifying the opening or closing character. For
 * example, to <code>pop</code> the current container off the stack without
 * swapping iterators, call {@link #pop(char)}.
 *
 * Todo: most of these methods are missing direct unit test coverage
 */
public abstract class TokenParser implements ValueParser {

    /**
     * Represents that no values are left in the input.
     */
    protected static final TokenStream EMPTY_VALUE =
        new TokenStream(0, 0, 0, 0, 0, TokenType.OPEN, new ArrayList<>());

    /**
     * An iterator which always returns empty, representing the end of input.
     */
    protected static final TokenStream.Itr EMPTY_ITERATOR =
        EMPTY_VALUE.iterator();

    /**
     * Direct access to the data stack used by this parser. This indirection is
     * designed to alleviate pressure from implementors by housing information
     * which they will always need. It is optimized to minimize the performance
     * impact.
     */
    protected final BufferedStack.OfTwo<TokenStream.Itr, JsonContainer> stack;

    /**
     * The very root container of the input. Implementors may need access to
     * this information for analysis on how the stream was generated.
     *
     * @apiNote Experimental--this may be replaced with a <code>parent</code>
     *          value in the future.
     */
    @ApiStatus.Experimental
    protected final TokenStream root;

    /**
     * Houses any formatting data for the current value. As with {@link #stack},
     * this value is designed to alleviate pressure from downstream implementors.
     * It may come with a subtle performance penalty.
     */
    protected JsonContainer formatting;

    /**
     * The current source of tokens.
     */
    protected TokenStream.Itr iterator;

    /**
     * The most recent token in the stream.
     */
    protected Token current;

    /**
     * Represents any lines skipped since the last significant token. This value
     * can be used to infer formatting information in the output.
     */
    protected int linesSkipped;

    /**
     * Constructs a new Parser when given a root stream of tokens.
     *
     * <p>Implementors should be aware that all tokens within this
     * stream are <b>assumed to be in order</b> by index. The API
     * makes no safety guarantees otherwise.
     *
     * @param root The root container representing the input.
     */
    protected TokenParser(final TokenStream root) {
        this.stack = BufferedStack.ofTwo();
        this.root = root;
        this.formatting = new JsonArray();
        this.iterator = root.iterator();
        this.current = root;
    }

    /**
     * Advanced the iterator a single time. If the iterator has
     * no values to return, yields a dummy value instead.
     */
    protected void read() {
        final TokenStream.Itr itr = this.iterator;
        if (itr == EMPTY_ITERATOR) {
            return;
        }
        if (itr.hasNext()) {
            this.current = this.iterator.next();
        } else {
            this.current = EMPTY_VALUE;
        }
    }

    /**
     * Pushes the current token onto the stack, if it is a {@link TokenStream}.
     *
     * @return <code>true</code>, if the method was able to push the container.
     */
    protected boolean push() {
        if (this.iterator.getParent() == this.current) {
            return false;
        }
        if (this.current instanceof TokenStream) {
            this.stack.push(this.iterator, this.formatting);
            this.iterator = ((TokenStream) this.current).iterator();
            this.formatting = new JsonArray();
            return true;
        }
        return false;
    }

    /**
     * Pops the current iterator and formatting information out of the stack.
     *
     * @return <code>true</code>, if the method was able to pop the container.
     */
    protected boolean pop() {
        this.expectEndOfContainer();
        if (this.stack.isEmpty()) {
            this.iterator = EMPTY_ITERATOR;
            return false;
        }
        this.stack.pop();
        this.iterator = this.stack.getFirst();
        this.formatting = this.stack.getSecond();
        return true;
    }

    /**
     * Begins recursion into a new container. Takes care of reading any initial
     * whitespace at the top of the container.
     *
     * @return <code>true</code>, if there are more tokens to parse.
     */
    protected boolean open() {
        this.push();
        if (this.isEndOfContainer()) {
            return false;
        }
        this.read();
        this.readWhitespace();
        return true;
    }

    /**
     * Closes the current container and pops one frame of the stack, resetting
     * the formatting and iterator to that of the previous level.
     *
     * @param container The current container being parsed.
     * @param <T> The type of container being parsed.
     * @return The input container, with formatting applied.
     */
    protected <T extends JsonContainer> T close(
            final T container) {
        this.setTrailing();
        this.takeFormatting(container);
        this.pop();
        this.read();
        return container;
    }

    /**
     * Variant of {@link #push()} for non-containerized input streams.
     *
     * <p>This method assumes that a matching opener has been found.
     *
     * @param opener The opening character of the container.
     * @throws SyntaxException if the opener is not found.
     * @see #push()
     */
    protected void push(final char opener) {
        this.expect(opener);
        this.stack.push(null, this.formatting);
        this.formatting = new JsonArray();
    }

    /**
     * Variant of {@link #pop()} for non-containerized input streams.
     *
     * <p>This method assumes that a matching closer has been found.
     *
     * @param closer The closing character of the container.
     * @throws SyntaxException if the closer is not found.
     * @see #pop()
     */
    protected void pop(final char closer) {
        this.expectEndOfContainer(closer);
        this.stack.pop();
        this.formatting = this.stack.getSecond();
    }

    /**
     * Variant of {@link #open()} for non-containerized input streams.
     *
     * @param opener The opening character for this container.
     * @param closer The closing character for this container.
     * @return <code>true</code> if there are more tokens to parse.
     * @see #open()
     */
    protected boolean open(final char opener, final char closer) {
        this.push(opener);
        this.readWhitespace();
        return !this.isEndOfContainer(closer);
    }

    /**
     * Variant of {@link #close(JsonContainer)} for non-containerized
     * input streams.
     *
     * @param container The container being closed and formatted.
     * @param closer The closing character for this type of container.
     * @param <T> The type of container being closed.
     * @return The input container, with formatting applied.
     * @see #close(JsonContainer)
     */
    protected <T extends JsonContainer> T close(
            final T container, final char closer) {
        this.setTrailing();
        this.takeFormatting(container);
        this.pop(closer);
        this.read();
        return container;
    }

    /**
     * Invokes the {@link #read()} method, <em>if</em> the current token
     * matches the given symbol.
     *
     * @param symbol The expected symbol at this position.
     * @return <code>true</code>, if the symbol matches {@link #current}.
     */
    protected boolean readIf(final char symbol) {
        if (this.current.isSymbol(symbol)) {
            this.read();
            return true;
        }
        return false;
    }

    /**
     * Specialized variant of {@link #readIf} designed for newline
     * characters. This method additionally takes care of flagging
     * the current line as skipped, if applicable.
     *
     * @return <code>true</code>, if the current token is a newline.
     */
    protected boolean readNl() {
        if (this.current.type() == TokenType.BREAK) {
            this.read();
            this.flagLineAsSkipped();
            return true;
        }
        return false;
    }

    /**
     * Flags the current newline symbol as being skipped. Implementors
     * may use this to perform additional actions when lines are skipped.
     *
     * <p>For example, in some situations, the line may need to be
     * appended to a comment or stored in some other variable.
     */
    protected void flagLineAsSkipped() {
        this.linesSkipped++;
    }

    /**
     * Indicates whether the current iterator has reached the end of input.
     *
     * @return <code>true</code>, if the iterator has finished.
     */
    protected boolean isEndOfContainer() {
        return this.current == EMPTY_VALUE;
    }

    /**
     * Indicates whether the parser has reached the end of input.
     *
     * @return <code>true</code>, if the parser has reached end of input.
     */
    protected boolean isEndOfText() {
        return this.stack.isEmpty() && !this.iterator.hasNext();
    }

    /**
     * Variant of {@link #isEndOfContainer()} for non-containerized input
     * streams.
     *
     * @param closer The symbol indicating the end of this container.
     * @return <code>true</code>, if the parser has reached this token.
     * @see #isEndOfContainer()
     */
    protected boolean isEndOfContainer(final char closer) {
        return this.current.isSymbol(closer);
    }

    /**
     * If applicable, consumes all whitespace at this point.
     */
    protected void readWhitespace() {
        this.readWhitespace(true, true);
    }

    /**
     * Variant of {@link #readWhitespace}, indicating whether
     * to reset the {@link #linesSkipped lines skipped} counter.
     *
     * @param resetLinesSkipped Whether to reset the counter.
     */
    protected void readWhitespace(final boolean resetLinesSkipped) {
        this.readWhitespace(resetLinesSkipped, true);
    }

    /**
     * Variant of {@link #readWhitespace(boolean)} which does not
     * consume newline characters. For regular {@link TokenStream
     * token streams}, this is essentially the same as capturing
     * comment data.
     *
     */
    protected void readLineWhitespace() {
        this.readWhitespace(false, false);
    }

    /**
     * Verbose variant of {@link #readWhitespace} which specifies
     * both whether to reset the line counter and whether to read
     * newline characters.
     *
     * @param resetLinesSkipped Whether to reset the counter.
     * @param nl Whether to consume newline characters.
     */
    protected void readWhitespace(
            final boolean resetLinesSkipped, final boolean nl) {
        if (resetLinesSkipped) {
            this.linesSkipped = 0;
        }
        final TokenStream.Itr itr = this.iterator;
        if (itr == EMPTY_ITERATOR) {
            return;
        }
        Token t = this.current;
        int peekAmount = 0;
        while (t != EMPTY_VALUE) {
            if (!this.consumeWhitespace(t, nl)) {
                break;
            }
            t = itr.peek(++peekAmount, EMPTY_VALUE);
        }
        this.current = t;
        itr.skip(peekAmount);
    }

    /**
     * Processes any metadata skipped by {@link #readWhitespace}.
     *
     * @param t  The token being skipped.
     * @param nl Whether to consume newline characters, if applicable.
     * @return <code>true</code>, if the token was consumed.
     */
    protected boolean consumeWhitespace(final Token t, final boolean nl) {
        if (nl && t.type() == TokenType.BREAK) {
            this.flagLineAsSkipped();
            return true;
        }
        return false;
    }

    /**
     * Indicates whether the current character represents non-newline
     * whitespace.
     *
     * @param c The character being evaluated.
     * @return <code>true</code>, if the character is line whitespace.
     */
    protected boolean isLineWhitespace(final char c) {
        return c == ' ' || c == '\r' || c == '\t';
    }

    /**
     * Skips any and all characters until the given symbol, a newline character, or
     * the end of the file is found.
     *
     * @param symbol The symbol being researched. Can be \u0000 for none.
     * @param nl     Whether to stop when reaching a newline character.
     * @param eof    Whether to tolerate end of input.
     * @return The number of tokens skipped.
     */
    protected int skipTo(final char symbol, final boolean nl, final boolean eof) {
        final TokenStream.Itr itr = this.iterator;
        if (!itr.hasNext()) {
            if (eof) return 0;
            throw this.expectedSymbolOrNL(symbol, nl);
        }
        Token t = itr.peek();
        Token lastRecorded = this.current;
        int peekAmount = 1;
        while (t != null) { // newlines would only be inside of containers for values
            if (t.isSymbol(symbol)
                    || (nl && t.type() == TokenType.BREAK)) {
                itr.skip(peekAmount - 1);
                this.current = lastRecorded;
                return peekAmount - 1;
            } else if (!this.consumeWhitespace(t, false)) {
                lastRecorded = t;
            }
            t = itr.peek(++peekAmount);
        }
        if (eof) {
            if (peekAmount > 1) {
                itr.skip(peekAmount - 1);
                this.current = lastRecorded;
            }
            return peekAmount;
        }
        throw this.expectedSymbolOrNL(symbol, nl);
    }

    /**
     * Stores any data <em>above</em> the current value as formatting.
     */
    protected void setAbove() {
        this.formatting.setLinesAbove(this.takeLinesSkipped());
    }

    /**
     * Stores any data <em>between</em> the current key and value
     * as formatting.
     */
    protected void setBetween() {
        this.formatting.setLinesBetween(this.takeLinesSkipped());
    }

    /**
     * Stores any data at the end of the current container as
     * formatting.
     */
    protected void setTrailing() {
        this.formatting.setLinesTrailing(this.takeLinesSkipped());
    }

    /**
     * Reads whitespace and consumes it as formatting above any
     * given value.
     */
    protected void readAbove() {
        this.readWhitespace(false);
        this.setAbove();
    }

    /**
     * Reads whitespace and a separator, consuming any formatting
     * before the value.
     *
     * @param kvSeparator The expected separator, e.g. <code>:</code>
     *                    or <code>=</code>.
     */
    protected void readBetween(final char kvSeparator) {
        this.readWhitespace();
        this.expect(kvSeparator);
        this.readWhitespace();
        this.setBetween();
    }

    /**
     * Reads whitespace after a value, until the end of the line, and
     * consumes it as formatting, if applicable.
     */
    protected void readAfter() {
        this.readLineWhitespace();
    }

    /**
     * Reads whitespace at the bottom of the file and consumes it as
     * formatting, if applicable.
     */
    protected void readBottom() {
        this.readWhitespace(false);
        this.expectEndOfText();
    }

    /**
     * Returns the current number of {@link #linesSkipped lines skipped},
     * resetting the counter to 0.
     *
     * @return The current number of lines skipped.
     */
    protected int takeLinesSkipped() {
        final int skipped = this.linesSkipped;
        this.linesSkipped = 0;
        return skipped;
    }

    /**
     * Transfers any formatting data into the given value.
     *
     * @param value The value being formatted.
     * @param <T>   The type of value being formatted.
     * @return The input, <code>value</code>.
     */
    protected <T extends JsonValue> T takeFormatting(final T value) {
        value.setDefaultMetadata(this.formatting);
        this.clearFormatting();
        return value;
    }

    /**
     * Resets the {@link #formatting formatting data} to its default
     * state.
     */
    protected void clearFormatting() {
        this.formatting.setLinesTrailing(-1)
            .setLinesAbove(-1)
            .setLinesBetween(-1)
            .setComments(null);
    }

    /**
     * Expects a symbol at the current position, or else throws a syntax
     * exception.
     *
     * @param expected The expected symbol at this position.
     */
    protected void expect(final char expected) {
        if (!this.readIf(expected)) {
            throw this.expected(expected);
        }
    }

    /**
     * Throws a syntax exception if more tokens are found in the input.
     */
    protected void expectEndOfText() {
        if (!this.isEndOfText()) {
            throw this.unexpected(this.current.type() + " before end of file");
        }
    }

    /**
     * Indicates that either a specific symbol or newline character was
     * expected at this position.
     *
     * @param symbol The expected symbol.
     * @param nl     Whether a newline character would have been accepted.
     * @return The exception to be thrown.
     */
    protected SyntaxException expectedSymbolOrNL(
            final char symbol, final boolean nl) {
        if (nl) {
            return this.expected("'" + symbol + "' or new line");
        }
        return this.expected(symbol);
    }

    /**
     * Throws an exception if there are more tokens left in the current
     * container.
     */
    protected void expectEndOfContainer() {
        if (!this.isEndOfContainer()) {
            throw this.tokensInContainer();
        }
    }

    /**
     * Variant of {@link #expectEndOfContainer()} for non-containerized
     * input streams.
     *
     * @param closer The closing character for this type of container.
     */
    protected void expectEndOfContainer(final char closer) {
        if (!this.isEndOfContainer(closer)) {
            throw this.tokensInContainer();
        }
    }

    /**
     * Indicates that unexpected tokens were found before the end of the
     * current container.
     *
     * @return The exception to be thrown.
     */
    protected SyntaxException tokensInContainer() {
        return this.unexpected(this.current.type() + " before end of container (missing delimiter?)");
    }

    /**
     * Indicates that the given token is not valid for this file format.
     *
     * @param text The text of the token found.
     * @return The exception to be thrown.
     */
    protected SyntaxException illegalToken(final String text) {
        return SyntaxException.illegalToken(text, this.current.line(), this.current.offset());
    }

    /**
     * Generates a generic "expected" message exception for a specific
     * symbol.
     *
     * @param expected The symbol expected at this position.
     * @return The exception to be thrown.
     */
    protected SyntaxException expected(final char expected) {
        return SyntaxException.expected(
            expected, this.current.line(), this.current.offset());
    }

    /**
     * Generates a generic "expected" message exception when given
     * a description of the expected tokens.
     *
     * @param expected A description of what was expected.
     * @return The exception to be thrown.
     */
    protected SyntaxException expected(final String expected) {
        return SyntaxException.expected(
            expected, this.current.line(), this.current.offset());
    }

    /**
     * Generates a generic "unexpected" message exception for a specific
     * symbol.
     *
     * @param unexpected The symbol not expected at this position.
     * @return The exception to be thrown.
     */
    protected SyntaxException unexpected(final char unexpected) {
        return SyntaxException.unexpected(
            unexpected, this.current.line(), this.current.offset());
    }

    /**
     * Generates a generic "unexpected" message exception when given
     * a description of the unexpected tokens.
     *
     * @param unexpected A description of what was not expected.
     * @return The exception to be thrown.
     */
    protected SyntaxException unexpected(final String unexpected) {
        return SyntaxException.unexpected(
            unexpected, this.current.line(), this.current.offset());
    }
}
