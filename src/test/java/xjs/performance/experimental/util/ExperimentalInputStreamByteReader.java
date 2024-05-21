package xjs.performance.experimental.util;

import org.jetbrains.annotations.Nullable;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public class ExperimentalInputStreamByteReader extends PositionTrackingReader {
    final InputStream is;
    final CharsetDecoder decoder;
    final byte[] buffer;
    final ByteBuffer charsIn;
    final CharBuffer charsOut;
    @Nullable StringBuilder out;

    int bufferOffset;
    int overflow;
    boolean finishedReading;

    public ExperimentalInputStreamByteReader(final InputStream is, final int size, final boolean captureFullText) {
        this.is = is;
        this.decoder = StandardCharsets.UTF_8.newDecoder();
        this.buffer = new byte[size];
        this.charsIn = ByteBuffer.wrap(this.buffer);
        this.charsOut = CharBuffer.allocate(size / 2);
        this.bufferOffset = 0;
        this.overflow = 0;
        if (captureFullText) this.out = new StringBuilder();
        this.finishedReading = false;
    }

    @Override
    public String getFullText() {
        if (this.out == null) {
            throw new IllegalStateException("output not configured");
        }
        return this.out.toString();
    }

    @Override
    public boolean isCapturingFullText() {
        return this.out != null;
    }

    @Override
    protected void appendToCapture() {
        this.capture.append(this.charsOut, this.captureStart, this.index);
    }

    @Override
    public int peek() {
        return -1;
    }

    public void read() throws IOException {
        if (this.index == this.charsOut.position()) {
            if (this.captureStart != -1) {
                this.appendToCapture();
                this.captureStart = 0;
            }
            if (this.finishedReading) {
                this.current = -1;
                return;
            }
            this.bufferOffset += this.charsOut.position();
            final int newLen = this.rotateBuffer();
            final int bytesRead = this.fillBuffer(newLen);
            this.finishedReading = bytesRead == -1;
            final int remaining = newLen + Math.max(0, bytesRead) - this.overflow;
            this.overflow = this.buffer.length - remaining;
            this.index = 0;
            if (remaining == 0) {
                this.current = -1;
                return;
            }
            this.decodeBuffer(remaining);
        }
        if (this.current == '\n') {
            this.line++;
            this.linesSkipped++;
            this.column = -1;
        }
        this.column++;
        this.current = this.charsOut.array()[this.index++];
    }

    private int rotateBuffer() {
        final int len = this.buffer.length - this.charsIn.position();
        if (len != 0 && len != this.buffer.length) {
            System.arraycopy(
                this.buffer, this.charsIn.position(), this.buffer, 0, len);
            return len;
        }
        return 0;
    }

    private int fillBuffer(final int offset) throws IOException {
        return this.is.read(this.buffer, offset, this.buffer.length - offset);
    }

    private void decodeBuffer(final int end) {
        this.charsIn.limit(end);
        this.charsIn.position(0);
        this.charsOut.position(0);
        this.decoder.reset();

        this.decoder.decode(this.charsIn, this.charsOut, this.finishedReading);
        if (this.out != null) {
            this.out.append(
                this.charsOut.array(), this.charsOut.arrayOffset(), this.charsOut.position());
        }
    }

    @Override
    public void close() throws IOException {
        this.is.close();
    }
}