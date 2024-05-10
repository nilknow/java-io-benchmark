package org.example;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

@State(Scope.Thread)
public class SmallFileIO {
    @Setup(Level.Iteration)
    public void setUp() {
    }

    @Benchmark
    public void do_aio() throws IOException {
        aio();
    }

    @Benchmark
    public void do_bio() throws IOException {
        blockReading();
    }

    @Benchmark
    public void do_nbio() throws IOException {
        nonBlockingReading();
    }

//    549.467 ops/s
    public void aio() throws IOException {
        String filePath = "C:\\Users\\nilknow\\IdeaProjects\\demo-interview\\src\\main\\resources\\small_file.txt";

        // Read the file asynchronously
        Path path = Paths.get(filePath);
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path);

        // Allocate a buffer
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // Read the file asynchronously
        fileChannel.read(buffer, 0, null, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        buffer.get();
                    }
                    buffer.clear();

                    // Read more data if available
                    fileChannel.read(buffer, 0, null, this);
                } else {
                    // Close the channel
                    try {
                        fileChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                exc.printStackTrace();
            }
        });
    }

    //    176.047 ops/s
    public void nonBlockingReading() throws IOException {
        // Get the file path
        String filePath = "C:\\Users\\nilknow\\IdeaProjects\\demo-interview\\src\\main\\resources\\small_file.txt";

        // Get the file channel
        Path path = Paths.get(filePath);
        FileChannel fileChannel = FileChannel.open(path);

        // Allocate a buffer
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // Read the file content
        while (fileChannel.read(buffer) > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                buffer.get();
            }
            buffer.clear();
        }

        // Close the channel
        fileChannel.close();
    }

    //    259.183 ops/s
    public void blockReading() throws IOException {
        ClassLoader classLoader = SmallFileIO.class.getClassLoader();

        // Get the file path
        String filePath = "small_file.txt";

        // Get the input stream
        InputStream inputStream = classLoader.getResourceAsStream(filePath);

        // Read the file content
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
        }

        // Close the reader
        reader.close();
    }

    public static void main(String[] args) throws RunnerException {
        final Options opts = new OptionsBuilder()
                .include(SmallFileIO.class.getSimpleName())
                .forks(1)
                .measurementIterations(2)
                .warmupIterations(1)
                .build();
        new Runner(opts).run();
    }
}