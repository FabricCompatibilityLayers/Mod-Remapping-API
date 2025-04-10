package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.resource;

import com.google.gson.Gson;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@ApiStatus.Internal
public class RefmapRemapper implements OutputConsumerPath.ResourceRemapper {
    private Gson GSON;
    @Override
    public boolean canTransform(TinyRemapper remapper, Path relativePath) {
        return relativePath.toString().toLowerCase().contains("refmap") && relativePath.toString().endsWith(".json");
    }

    @Override
    public void transform(Path destinationDirectory, Path relativePath, InputStream input, TinyRemapper remapper) throws IOException {
        Path outputFile = destinationDirectory.resolve(relativePath);
        Path outputDir = outputFile.getParent();
        if (outputDir != null) Files.createDirectories(outputDir);

        if (GSON == null) GSON = new Gson();

        RefmapJson json = GSON.fromJson(new InputStreamReader(input), RefmapJson.class);

        json.remap(remapper);

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
            DataOutputStream outputStream = new DataOutputStream(os);
            outputStream.write(GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }
}
