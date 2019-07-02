package com.divinitor.dn.lib.game.mod.compiler.processors;

import co.phoenixlab.dn.subfile.shader.Shader;
import co.phoenixlab.dn.subfile.shader.ShaderPack;
import co.phoenixlab.dn.subfile.shader.ShaderPackWriter;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShaderProcessor implements Processor {
    @Override
    public Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src) {
        return () -> {
            Path in = modPack.getKit().resolveModPackagePath(modPack, src);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            ShaderPack pack = new ShaderPack();

            List<Path> collect;
            try (Stream<Path> files = Files.walk(in)) {
                collect = files
                    .filter(negate(Files::isDirectory))
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .collect(Collectors.toList());
            }

            for (Path path : collect) {
                Shader shdr;
                try (BufferedReader reader = Files.newBufferedReader(path) ) {
                    shdr = gson.fromJson(reader, Shader.class);
                }

                Path parentDir = path.getParent().getFileName();
                int quality = Integer.parseInt(parentDir.toString());
                shdr.setQuality(quality);

                Path parent = path.getParent();
                Path dataFile = parent.resolve(shdr.getName());
                shdr.setShaderData(Files.readAllBytes(dataFile));
                shdr.setShaderDataSize(shdr.getShaderData().length);
                pack.getShaders().add(shdr);
            }

            ShaderPackWriter writer = new ShaderPackWriter();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            writer.write(pack, os);
            byte[] result = os.toByteArray();

            try {
                Path root = modPack.getKit().getRoot();
                Path temp = root.resolve("temp");
                Files.createDirectories(temp);
                String tempFileName = modPack.getId() + "-" + "dnshaders.dat";
                Path tempFile = temp.resolve(tempFileName);
                Files.deleteIfExists(tempFile);
                Files.write(tempFile, result);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        };
    }

    private static <T> Predicate<T> negate(Predicate<T> predicate) {
        return predicate.negate();
    }

    @Getter
    @Setter
    static class ShaderDescriptor extends Shader {
        private String ep;
    }
}


// fxc /T fx_2_0 /E DOFFilterVS /FoDOFFilter.fx DOFFilter.src.fx
