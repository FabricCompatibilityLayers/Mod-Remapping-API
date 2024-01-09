package fr.catcore.modremapperapi.test.impl;

import fr.catcore.modremappingapi.api.v1.ModDiscoverer;
import fr.catcore.modremappingapi.api.v1.ModInfos;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class MLModDiscoverer implements ModDiscoverer {
    @Override
    public String getId() {
        return "ml-mods";
    }

    @Override
    public String getName() {
        return "ML mods";
    }

    @Override
    public String[] getModFolders() {
        return new String[]{"mods"};
    }

    @Override
    public String[] getModExtensions() {
        return new String[]{".jar",".zip"};
    }

    @Override
    public boolean isMod(String fileName) {
        fileName = fileName.substring(1);
        return fileName.startsWith("mod_") && fileName.endsWith(".class") && !fileName.contains("/");
    }

    @Override
    public boolean isMod(String fileName, Path filePath) throws IOException {
        fileName = fileName.substring(1);

        if (fileName.startsWith("mod_") && fileName.endsWith(".class") && !fileName.contains("/")) {
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                ClassReader reader = new ClassReader(inputStream);
                ClassNode node = new ClassNode(Opcodes.ASM9);
                reader.accept(node, ClassReader.EXPAND_FRAMES);

                return node.superName != null && node.superName.equals("BaseMod");
            }
        }

        return false;
    }

    @Override
    public ModInfos parseModInfos(String fileName, Path filePath) throws IOException {
        String modName = fileName.substring(5).replace(".class", "");
        String modId = modName.toLowerCase(Locale.ENGLISH);
        String version = null;

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            ClassReader reader = new ClassReader(inputStream);
            ClassNode node = new ClassNode(Opcodes.ASM9);
            reader.accept(node, ClassReader.EXPAND_FRAMES);

            if (node.methods != null) {
                for (MethodNode methodNode : node.methods) {
                    if (methodNode.name.equals("Version") && methodNode.desc.equals("()Ljava/lang/String;")) {
                        if (methodNode.instructions != null) {
                            for (AbstractInsnNode insnNode : methodNode.instructions) {
                                if (insnNode instanceof LdcInsnNode) {
                                    Object val = ((LdcInsnNode) insnNode).cst;

                                    if (val instanceof String) {
                                        version = (String) val;
                                        break;
                                    }
                                }
                            }
                        }

                        break;
                    }
                }
            }
        }

        return new MLModInfos(modName, modId, version);
    }

    @Override
    public boolean addToClasspath() {
        return true;
    }

    @Override
    public boolean excludeEdits() {
        return true;
    }

    @Override
    public boolean acceptDirectories() {
        return true;
    }
}
