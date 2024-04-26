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
        try {
            DjsTokenizer.containerize(generateNormallyDistributedSample()).readToEnd();
            return true;
        } catch (final SyntaxException ignored) {}
        return false;
    }

    private static String generateNormallyDistributedSample() {
        if (Math.random() >= 0.5) {
            return DJS_SAMPLE + "//";
        } else {
            return DJS_SAMPLE + ")";
        }
    }

    @Enabled(true)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue djsParsingSample() {
        return new DjsParser(SIMPLE_DJS_SAMPLE).parse();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String djsWritingSample() throws IOException {
        final StringWriter sw = new StringWriter();
        new DjsWriter(sw, true).write(DJS_WRITING_SAMPLE);
        return sw.toString();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue jsonParsingSample() throws IOException {
        return new JsonParser(JSON_SAMPLE).parse();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String jsonWritingSample() throws IOException {
        final StringWriter sw = new StringWriter();
        new JsonWriter(sw, true).write(JSON_WRITING_SAMPLE);
        return sw.toString();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String stringReaderSample() throws IOException {
        return PositionTrackingReader.fromString(
            READER_INPUT_SAMPLE).readToEnd();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_smallestBuffer() throws IOException {
        return new ExperimentalInputStreamByteReader(
            getReadingSampleIS(), 8, true).readToEnd();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_mediumBuffer() throws IOException {
        return new ExperimentalInputStreamByteReader(
            getReadingSampleIS(), 128, true).readToEnd();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String byteBufferSample_normalBuffer() throws IOException {
        return new ExperimentalInputStreamByteReader(
            getReadingSampleIS(), 1024, true).readToEnd();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_smallestBuffer() throws IOException {
        return PositionTrackingReader.fromIs(
            getReadingSampleIS(), 8, true).readToEnd();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_mediumBuffer() throws IOException {
        return PositionTrackingReader.fromIs(
            getReadingSampleIS(), 128, true).readToEnd();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String charBufferSample_normalBuffer() throws IOException {
        return PositionTrackingReader.fromIs(
            getReadingSampleIS(), 1024, true).readToEnd();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStandardReader() throws IOException {
        final Reader reader =
            new InputStreamReader(getReadingSampleIS());
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[1024];
        int bytesRead;
        while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
            sb.append(buffer, 0, bytesRead);
        }
        return DjsTokenizer.stream(sb.toString());
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromPositionTrackingReader() throws IOException {
        final PositionTrackingReader reader =
            PositionTrackingReader.fromIs(getReadingSampleIS());
        return DjsTokenizer.stream(reader.readToEnd());
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStreamReader() throws IOException {
        final TokenStream stream = DjsTokenizer.stream(getReadingSampleIS());
        stream.forEach(t -> {});
        return stream;
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TokenStream stream_fromStringReader() throws IOException {
        final Reader reader =
            new InputStreamReader(getReadingSampleIS());
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[1024];
        int bytesRead;
        while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
            sb.append(buffer, 0, bytesRead);
        }
        final TokenStream stream = DjsTokenizer.stream(sb.toString());
        stream.forEach(t -> {});
        return stream;
    }

    private static InputStream getReadingSampleIS() {
        return new ByteArrayInputStream(
            READER_INPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
    }
}
