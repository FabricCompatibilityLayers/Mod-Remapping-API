package fr.catcore.modremapperapi.utils;

import fr.catcore.modremapperapi.ModRemappingAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.Tiny1Reader;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipError;

public class MappingsUtils {
    private static MemoryMappingTree MINECRAFT_MAPPINGS;
    private static boolean initialized = false;

    public static String getNativeNamespace() {
        if (ModRemappingAPI.BABRIC) {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "client" : "server";
        }

        return "official";
    }

    public static String getTargetNamespace() {
        return "intermediary";
    }
    
    private static void initialize() {
        if (initialized) return;

        URL url = MappingConfiguration.class.getClassLoader().getResource("mappings/mappings.tiny");

        if (url != null) {
            try {
                URLConnection connection = url.openConnection();

                if (connection instanceof JarURLConnection) {
                    Manifest manifest = ((JarURLConnection) connection).getManifest();

                    if (manifest != null) {
//                        gameId = ManifestUtil.getManifestValue(manifest, new Attributes.Name("Game-Id"));
//                        gameVersion = ManifestUtil.getManifestValue(manifest, new Attributes.Name("Game-Version"));
                    }
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    long time = System.currentTimeMillis();
                    MINECRAFT_MAPPINGS = new MemoryMappingTree();

                    // We will only ever need to read tiny here
                    // so to strip the other formats from the included copy of mapping IO, don't use MappingReader.read()
                    reader.mark(4096);
                    final MappingFormat format = MappingReader.detectFormat(reader);
                    reader.reset();

                    switch (format) {
                        case TINY:
                            Tiny1Reader.read(reader, MINECRAFT_MAPPINGS);
                            break;
                        case TINY_2:
                            Tiny2Reader.read(reader, MINECRAFT_MAPPINGS);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported mapping format: " + format);
                    }

                    Log.debug(LogCategory.MAPPINGS, "Loading mappings took %d ms", System.currentTimeMillis() - time);
                }
            } catch (IOException | ZipError e) {
                throw new RuntimeException("Error reading "+url, e);
            }
        }

        if (MINECRAFT_MAPPINGS == null) {
            Log.info(LogCategory.MAPPINGS, "Mappings not present!");
            MINECRAFT_MAPPINGS = new MemoryMappingTree();
        }

        initialized = true;
    }

    public static MappingTree loadMappings(Reader reader) {
        MemoryMappingTree tree = new MemoryMappingTree();
        try {
            loadMappings(reader, tree);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tree;
    }

    public static void loadMappings(Reader reader, MappingVisitor tree) throws IOException {
        MappingReader.read(reader, tree);
    }

    public static MappingTree getMinecraftMappings() {
        initialize();
        return MINECRAFT_MAPPINGS;
    }

    public static IMappingProvider createProvider(MappingTree mappings) {
        return (acceptor) -> {
            final int fromId = mappings.getNamespaceId(getNativeNamespace());
            final int toId = mappings.getNamespaceId(getTargetNamespace());

            for (MappingTree.ClassMapping classDef : mappings.getClasses()) {
                final String className = classDef.getName(fromId);
                String dstName = classDef.getName(toId);

                if (ModRemappingAPI.BABRIC && className == null) continue;

                if (dstName == null) {
                    dstName = className;
                }

                acceptor.acceptClass(className, dstName);

                for (MappingTree.FieldMapping field : classDef.getFields()) {
                    String fieldName = field.getName(fromId);

                    if (ModRemappingAPI.BABRIC && fieldName == null) continue;

                    acceptor.acceptField(memberOf(className, fieldName, field.getDesc(fromId)), field.getName(toId));
                }

                for (MappingTree.MethodMapping method : classDef.getMethods()) {
                    String methodName = method.getName(fromId);

                    if (ModRemappingAPI.BABRIC && methodName == null) continue;

                    IMappingProvider.Member methodIdentifier = memberOf(className, methodName, method.getDesc(fromId));
                    acceptor.acceptMethod(methodIdentifier, method.getName(toId));
                }
            }
        };
    }

    private static IMappingProvider.Member memberOf(String className, String memberName, String descriptor) {
        return new IMappingProvider.Member(className, memberName, descriptor);
    }
}
