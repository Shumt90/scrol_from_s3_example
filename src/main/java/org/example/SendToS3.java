package org.example;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SendToS3 {
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

        var folder = "/Users/a19045391/IdeaProjects/edu/stream-from-s3/src/main/resources/divided_arch";

        send(folder,client);
    }

    public static List<String> send(String folder, AmazonS3 client) throws IOException {


        var s3Folder = "dictionary/" + UUID.randomUUID().toString();
        var bucketName = System.getenv("BUCKET_NAME");
        var index = new AtomicInteger();

        return Files.walk(Path.of(folder))
                .filter(file -> !folder.equals(file.toString())) //self
                .filter(file->file.toString().charAt(file.toString().lastIndexOf("/")+1)!='.') //hidden files
                .sorted()
                .peek(System.out::println)
                .map(v -> {
                    try {
                        String s3FilePath = wrapToFolder(String.valueOf(index.incrementAndGet()),s3Folder);
                        writeOne(bucketName, s3Folder, Files.newInputStream(v), s3FilePath,client);
                        return s3FilePath;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .peek(System.out::println)
                .collect(Collectors.toList());

    }

    private static void writeOne(String bucketName, String folder, InputStream is, String s3FilePath, AmazonS3 client) throws IOException {
        client.putObject(bucketName, s3FilePath, is, new ObjectMetadata());
    }
    private static String wrapToFolder(String id, String folderName) {
        return folderName + "/" + id;
    }
}
