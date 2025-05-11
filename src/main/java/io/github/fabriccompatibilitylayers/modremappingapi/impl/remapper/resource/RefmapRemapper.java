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
    private Gson gson;
    
    @Override
    public boolean canTransform(TinyRemapper remapper, Path relativePath) {
        var path = relativePath.toString();
        return path.toLowerCase().contains("refmap") && path.endsWith(".json");
    }

    @Override
    public void transform(Path destinationDirectory, Path relativePath, InputStream input, TinyRemapper remapper) throws IOException {
        var outputFile = destinationDirectory.resolve(relativePath);
        var outputDir = outputFile.getParent();
        if (outputDir != null) Files.createDirectories(outputDir);

        if (gson == null) gson = new Gson();

        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            var json = gson.fromJson(reader, RefmapJson.class);
            json.remap(remapper);

            Files.writeString(outputFile, gson.toJson(json));
        }
    }
}
