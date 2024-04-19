package xjs.performance.experimental.util;

import xjs.data.serialization.token.Token;

public class ExperimentalTripleParser {

    public static String parseTriple(final CharSequence reference, final Token t) {
        final StringBuilder sb = new StringBuilder();
        final int start = t.start() + 3;
        final int end = t.end() - 3;

        if (start == end) {
            return "";
        }

        int i = start;
        char c = reference.charAt(i);

        while (i < end && isLineWhitespace(c)) {
            c = reference.charAt(++i);
        }

        final int offset = t.offset();
        if (c == '\n') {
            i = skipToOffset(reference,i + 1, offset);
        }

        int marker = i;
        while (++i < end) {
            c = reference.charAt(i);
            if (c == '\n') {
                sb.append(reference, marker, i + 1);
                marker = skipToOffset(reference, i + 1, offset);
                if (marker >= end - 1) {
                    break;
                }
                i = marker;
            } else if (c == '\\' && i < end - 1) {
                sb.append(reference, marker, i);
                c = reference.charAt(++i);
                if (c != '\'') {
                    sb.append('\\');
                }
                sb.append(c);
                marker = i;
            }
        }
        if (i > marker) {
            sb.append(reference, marker, i);
        }
        if (sb.length() != 0 & sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static int skipToOffset(
            final CharSequence reference, final int start, final int offset) {
        for (int i = start; i < start + offset; i++) {
            if (!isLineWhitespace(reference.charAt(i))) {
                return i;
            }
        }
        return start + offset;
    }

    private static boolean isLineWhitespace(final char c) {
        return c == ' ' || c == '\r' || c == '\t';
    }
}
