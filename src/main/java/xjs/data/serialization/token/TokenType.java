package xjs.data.serialization.token;

/**
 * The type of token represented by a {@link Token} wrapper.
 */
public enum TokenType {

    /**
     * A single word, matching the pattern \\w+
     */
    WORD,

    /**
     * Any non-word stream of characters
     */
    SYMBOL,

    /**
     * Represents any sequence of numeric symbols.
     */
    NUMBER,

    /**
     * Any type of quoted, unquoted, or generated string.
     */
    STRING,

    /**
     * An open stream of tokens with no encapsulation.
     */
    OPEN,

    /**
     * Any stream of tokens encapsulated inside of braces.
     */
    BRACES,

    /**
     * Any stream of tokens encapsulated inside of brackets.
     */
    BRACKETS,

    /**
     * Any stream of tokens encapsulated inside of parentheses.
     */
    PARENTHESES,

    /**
     * A single-line, hash-style comment
     */
    COMMENT,

    /**
     * A line-break, either \n or \r\n
     */
    BREAK
}
