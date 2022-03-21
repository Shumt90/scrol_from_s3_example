package org.example;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class FileGenerator {
    public static void main(String[] args) throws IOException {
        var fos = new FileOutputStream(Path.of(UUID.randomUUID().toString() + ".json").toFile());

        var sb = new StringBuilder();
        sb.append("[");
        var first = true;
        for (int i = 999; i < 2000; i++) {

            if (first) {
                first = false;
            } else {
                sb.append(",");
            }

            sb.append("{\"id\":");
            sb.append(i);
            sb.append("}");
        }

        sb.append("]");

        fos.write(sb.toString().getBytes());
        fos.flush();
    }
}
