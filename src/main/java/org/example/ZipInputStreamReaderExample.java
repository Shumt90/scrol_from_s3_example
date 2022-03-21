package org.example;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipInputStreamReaderExample {

    public static void main(String[] args) throws IOException {
        var folder = "src/main/resources/divided_arch";

        var files=        Files.walk(Path.of(folder))
                .filter(file -> !folder.equals(file.toString())) //self
                .filter(file->file.toString().charAt(file.toString().lastIndexOf("/")+1)!='.') //hidden files
                .sorted()
                .peek(System.out::println)
                .collect(Collectors.toList());

        var zis = new ZipInputStream(new SequenceInputStream(new FileDownloader(files)));


        ZipEntry entry;
        while ((entry=zis.getNextEntry())!= null){
            System.out.println(entry.getName());
            System.out.println(new String(zis.readAllBytes()));
        }

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
