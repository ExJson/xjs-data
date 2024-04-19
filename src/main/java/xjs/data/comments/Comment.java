package xjs.data.comments;

import xjs.data.serialization.token.CommentToken;

public record Comment(CommentStyle style, String text) {
    public Comment(final CommentToken token) {
        this(token.commentStyle(), token.parsed());
    }
}
