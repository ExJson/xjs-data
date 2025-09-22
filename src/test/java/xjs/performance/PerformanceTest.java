package xjs.performance;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import xjs.data.Json;
import xjs.data.JsonCopy;
import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.token.DjsTokenizer;
import xjs.data.serialization.writer.JsonWriterOptions;
import xjs.performance.experimental.util.ExperimentalInputStreamByteReader;
import xjs.data.serialization.parser.JsonParser;
import xjs.data.serialization.parser.DjsParser;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.util.PositionTrackingReader;
import xjs.data.serialization.writer.JsonWriter;
import xjs.data.serialization.writer.DjsWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnusedReturnValue")
public class PerformanceTest {

    private static final String SIMPLE_DJS_SAMPLE =
        "[1234,5,6,7,'abc',\"def\",[[[['ghi',{},{}]]]],true,false,null,'hello','world']";

    private static final String DJS_COMMENTS_SAMPLE = """
        /**
         * banana
         *asdf
         *  as;ldkfjapoiwemaoiwcm;lzksdvmpaoiefj[qi23fmalsdmva;s/dv
         *
         *ffff
         *  asd;lfkmapwoi2mqp3ocasd;lk;asdklma;lwekvm   WPOIWQ3E
         *
         *alsidmfa;lwiem2qoicjo-3efa
         *  ASLDKFMAPLIWJQ0398WJAOPSIDFJPOAWIERA;LWIERU:lzDIFU;er
         *
         */
        
        
        // never gonna
        
        # give
        
        // you
        
        # a banana
        
        /*
         *
         *ASD;FLKMAWEasdfI C[AOI JV;LAISD JVLA;ISDJVLK;ASDV;KLAMSD; VKNSDV
         *
         *asdkvm;alskm[ OFaesfawergqehqergasd
         *
         *asdf
         *ASDF;LKAMSD[CFVAM;EWLKVMA;SLDCMA.KSDVMA.wke
         *
         *FFFFFF
         *TORTURE
         *asdf
         * sadfja;sldkfmaio[WFFFFOEIF
         *
         */
        """;

    private static final String SIMPLE_JSON_SAMPLE =
        "[1234,5,6,7,\"abc\",\"def\",[[[[\"ghi\",{},{}]]]],true,false,null,\"hello\",\"world\"]";

    private static final String DJS_SAMPLE = """
        // Comment
        a: 1 # Comment
        b:
          /* Comment */
          2
        c: [ '3a', '3b', '3c' ]
        d: { da: '4a', db: '4b' }
        e: '''
          multiline string
          that's correct,
          this really is
          multiple lines
          '''
        """;

    private static final String JSON_SAMPLE = """
        {
          "a": 1,
          "b":
            2,
          "c": [ "3a", "3b", "3c" ],
          "d": { "da": "4a", "db": "4b" },
          "e": "(Multiline text\\njust a few lines\\nnothing special\\"\\nnothing required)"
        }
        """;

    private static final JsonValue DJS_WRITING_SAMPLE =
        Json.parse(DJS_SAMPLE).copy(JsonCopy.UNFORMATTED | JsonCopy.COMMENTS);

    private static final JsonValue JSON_WRITING_SAMPLE =
        Json.parse(DJS_SAMPLE).copy(JsonCopy.UNFORMATTED);

    private static final String READER_INPUT_SAMPLE =
        DJS_SAMPLE.repeat(10);

    public static void main(final String[] args) throws Exception {
        LocalBenchmarkRunner.runIfEnabled();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public boolean containerizedIsBalanced() {
        try (final TokenStream stream =
                 containerize(generateNormallyDistributedSample())) {
            stream.readToEnd();
            return true;
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        } catch (final SyntaxException ignored) {
            return false;
        }
    }

    private static String generateNormallyDistributedSample() {
        if (Math.random() >= 0.5) {
            return DJS_SAMPLE + "//";
        } else {
            return DJS_SAMPLE + ")";
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue djsParsingSample() {
        try (final DjsParser parser = new DjsParser(PositionTrackingReader.fromString(SIMPLE_DJS_SAMPLE))) {
            return parser.parse();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(true)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String djsWritingSample() {
        try (final StringWriter sw = new StringWriter();
                final DjsWriter writer = new DjsWriter(sw, new JsonWriterOptions())) {
            writer.write(DJS_WRITING_SAMPLE);
            return sw.toString();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(true)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue jsonParsingSample() {
        try (final JsonParser parser = new JsonParser(PositionTrackingReader.fromString(JSON_SAMPLE))) {
            return parser.parse();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String jsonWritingSample() {
        try (final StringWriter sw = new StringWriter();
             final JsonWriter writer = new JsonWriter(sw, new JsonWriterOptions())) {
            writer.write(DJS_WRITING_SAMPLE);
            return sw.toString();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String stringReaderSample() {
        try (final PositionTrackingReader reader =
                 PositionTrackingReader.fromString(READER_INPUT_SAMPLE)) {
            return reader.readToEnd();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_smallestBuffer() {
        try (final PositionTrackingReader reader =
                new ExperimentalInputStreamByteReader(getReadingSampleIS(), 8, true)) {
            return reader.readToEnd();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_mediumBuffer() {
        try (final PositionTrackingReader reader =
                 new ExperimentalInputStreamByteReader(getReadingSampleIS(), 128, true)) {
            return reader.readToEnd();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_normalBuffer() throws IOException {
        try (final PositionTrackingReader reader =
                 new ExperimentalInputStreamByteReader(getReadingSampleIS(), 1024, true)) {
            return reader.readToEnd();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_smallestBuffer() {
        try (final PositionTrackingReader reader =
                 PositionTrackingReader.fromIs(getReadingSampleIS(), 8, true)) {
            return reader.readToEnd();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_mediumBuffer() {
        try (final PositionTrackingReader reader =
                 PositionTrackingReader.fromIs(getReadingSampleIS(), 128, true)) {
            return reader.readToEnd();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_normalBuffer() {
        try (final PositionTrackingReader reader =
                 PositionTrackingReader.fromIs(getReadingSampleIS(), 1024, true)) {
            return reader.readToEnd();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStandardReader() {
        try (final Reader reader = new InputStreamReader(getReadingSampleIS())) {
            final StringBuilder sb = new StringBuilder();
            final char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
                sb.append(buffer, 0, bytesRead);
            }
            return stream(sb.toString());
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromPositionTrackingReader() {
        try (final PositionTrackingReader reader =
                 PositionTrackingReader.fromIs(getReadingSampleIS())) {
            return stream(reader.readToEnd());
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStreamReader() throws IOException {
        final TokenStream stream = stream(getReadingSampleIS());
        stream.forEach(t -> {});
        return stream;
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStringReader() {
        try (final Reader reader = new InputStreamReader(getReadingSampleIS())) {
            final StringBuilder sb = new StringBuilder();
            final char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
                sb.append(buffer, 0, bytesRead);
            }
            final TokenStream stream = stream(sb.toString());
            stream.forEach(t -> {});
            return stream;
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    private static InputStream getReadingSampleIS() {
        return new ByteArrayInputStream(
            READER_INPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
    }

    private static TokenStream containerize(final String data) {
        return new DjsTokenizer(PositionTrackingReader.fromString(data), true).stream();
    }

    private static TokenStream stream(final String data) {
        return new DjsTokenizer(PositionTrackingReader.fromString(data), false).stream();
    }

    private static TokenStream stream(final InputStream data) throws IOException {
        return new DjsTokenizer(PositionTrackingReader.fromIs(data), false).stream();
    }
}
