package org.example;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.io.FileUtils;
import org.jsfr.json.JacksonParser;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.path.JsonPath;
import org.jsfr.json.provider.JacksonProvider;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.lang.Math.min;
import static org.jsfr.json.compiler.JsonPathCompiler.compile;

public class Shell {
    private static final JsonSurfer surfer = new JsonSurfer(JacksonParser.INSTANCE, JacksonProvider.INSTANCE);
    private static AmazonS3 client;


    private static void initS3Client() {
        var endpoint = System.getenv("ENDPOINT");
        var accessKey = System.getenv("ACCESS_KEY");
        var secret = System.getenv("SECRET_KEY");

        System.out.printf("Endpoint: %s, accessKey: %s, secret: %s \n", endpoint, accessKey, secret);
        client = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(System.getenv("ENDPOINT"), "ru"))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(System.getenv("ACCESS_KEY"), System.getenv("SECRET_KEY"))))
                .withPathStyleAccessEnabled(false)
                .build();
    }

    public static void main(String[] args) throws IOException {

        initS3Client();

        var files = new LinkedList<String>();

        var id = "d8736142-fc69-4ff7-9d21-b085a4389149";

        for (int i = 1; i <= 5; i++) {
            files.add("dictionary/" + id + "/" + i);
        }


        var d = new FileDownloader(files);
        var is = new SequenceInputStream(d);

        var zis = new ZipInputStream(is);

        StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new ZipIterator(zis), Spliterator.ORDERED),
                false)
                .flatMap(e -> {

                    Iterator<Object> iterator = surfer.iterator(e, compile("$[*].id"));

                    return StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                            false)
                            .map(Object::toString);
                })
                .forEach(System.out::println);

    }

    private static class ZipEntryReader implements Enumeration<InputStream> {

        private final ZipInputStream zis;
        private final AtomicReference<byte[]> current = new AtomicReference<>();
        private final byte[] buf = new byte[1024 * 1024];

        public ZipEntryReader(ZipInputStream zis) {
            this.zis = zis;
        }

        @Override
        public boolean hasMoreElements() {

            System.out.println("read next byte from entry");
            int len = 0;
            try {
                len = zis.read(buf);
                if (len > 0)
                    current.set(Arrays.copyOf(buf, len));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return len > 0;
        }

        @Override
        public InputStream nextElement() {
            return new ByteArrayInputStream(Arrays.copyOf(current.get(), current.get().length));
        }
    }

    private static class ZipIterator implements Iterator<InputStream> {

        private final ZipInputStream zis;
        private final AtomicReference<InputStream> current = new AtomicReference<>();

        private ZipIterator(ZipInputStream zis) {
            this.zis = zis;
        }

        @Override
        public boolean hasNext() {

            System.out.println("read next entry");
            final byte[] buf = new byte[1024 * 1024];

            try {

                var ent = zis.getNextEntry();
                if (ent != null) {
                    current.set(new SequenceInputStream(new ZipEntryReader(zis)));
                    return true;
                } else {
                    zis.close();
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        public InputStream next() {
            return current.get();
        }
    }

    private static void listFolder(String bucketName, String folder) {
        folder = folder + "/";

        ListObjectsV2Result result = client.listObjectsV2(bucketName, folder);
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        for (S3ObjectSummary os : objects) {
            if (os.getKey().endsWith(folder))
                continue;

            var md = client.getObjectMetadata(bucketName, os.getKey());
            System.out.printf("MD for %s is: %s \n", os.getKey(), md.getLastModified());
        }
    }

    private static class FileDownloader implements Enumeration<InputStream> {

        private int step = -1;
        private AtomicReference<byte[]> current;
        private List<String> files;
        private String bucketName = System.getenv("BUCKET_NAME");

        public FileDownloader(List<String> files) {
            this.files = files;
        }

        @Override
        public boolean hasMoreElements() {

            if (++step < files.size()) {
                try {
                    current = new AtomicReference<>(client.getObject(bucketName, files.get(step)).getObjectContent().getDelegateStream().readAllBytes());
                    System.out.println("download file " + files.get(step));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            } else {
                current = null;
                return false;
            }
        }

        @Override
        public InputStream nextElement() {
            return new ByteArrayInputStream(current.get());
        }

    }

}
