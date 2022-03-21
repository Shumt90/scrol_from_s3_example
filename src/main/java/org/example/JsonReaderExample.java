package org.example;


import org.jsfr.json.JacksonParser;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.provider.JacksonProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.jsfr.json.compiler.JsonPathCompiler.compile;

public class JsonReaderExample {

    private static final JsonSurfer surfer = new JsonSurfer(JacksonParser.INSTANCE, JacksonProvider.INSTANCE);

    public static void main(String[] args) throws IOException {

        var arr=List.of(new ByteArrayInputStream("[{\"id\":1".getBytes()),
                new ByteArrayInputStream("01}]".getBytes()));


            var is= new SequenceInputStream(new Enumeration<InputStream>() {

                int count=0;

                @Override
                public boolean hasMoreElements() {
                    return count<=1;
                }

                @Override
                public InputStream nextElement() {
                    return arr.get(count++);
                }
            });


            Iterator<Object> iterator = surfer.iterator(is, compile("$[*].id"));

            iterator.forEachRemaining(v -> {
                System.out.println(v.toString());
            });




    }

    private static class FileDownloader implements Enumeration<InputStream> {

        private int step = -1;
        private AtomicReference<byte[]> current;
        private List<Path> files;
        private Iterator<Object> iterator;

        public FileDownloader(List<Path> files) {
            this.files = files;
        }

        @Override
        public boolean hasMoreElements() {

            if (++step < files.size()) {
                try {
                    System.out.println("get: "+files.get(step));
                    current = new AtomicReference<>(Files.readAllBytes(files.get(step)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public InputStream nextElement() {
            var currentL = current.get();
            current = null;
            return new ByteArrayInputStream(currentL);
        }

        public void setIterator(Iterator<Object> iterator) {
            this.iterator = iterator;
        }
    }
}
