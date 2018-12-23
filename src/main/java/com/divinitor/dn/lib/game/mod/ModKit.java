package com.divinitor.dn.lib.game.mod;

import com.divinitor.dn.lib.game.mod.compiler.ModKitCompiler;
import com.divinitor.dn.lib.game.mod.compiler.SingleModCompiler;
import com.divinitor.dn.lib.game.mod.constraints.ConstraintViolationException;
import com.divinitor.dn.lib.game.mod.constraints.ModPackageConstraints;
import com.divinitor.dn.lib.game.mod.definition.BuildInfo;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.InstantGsonAdapter;
import com.divinitor.dn.lib.game.mod.util.VersionGsonAdapter;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModKit {

    public static final Logger LOGGER = LoggerFactory.getLogger(ModKit.class);
    public static final Version KIT_VERSION = Version.forIntegers(0, 4, 0);

    @Getter
    private final Path root;

    @Getter
    private ModKitCompiler kitCompiler;

    @Getter
    private SingleModCompiler singleCompiler;

    @Getter
    private DnAssetAccessService assetAccessService;

    public ModKit(Path root) {
        this.root = root;
    }

    public void init() throws NotGameDirectoryException, IOException {
        this.checkIsGameDir();

        this.assetAccessService = new DnAssetAccessService(this.root);
        this.assetAccessService.indexPaks();

        this.kitCompiler = new ModKitCompiler(this);
        this.singleCompiler = new SingleModCompiler(this);
    }

    private void checkIsGameDir() throws NotGameDirectoryException {
        Path gameExe = this.root.resolve("dragonnest.exe");
        if (!Files.isRegularFile(gameExe)) {
            throw new NotGameDirectoryException(this.root, "Missing game executable");
        }

        Path resource00 = this.root.resolve("Resource00.pak");
        if (!Files.isRegularFile(resource00)) {
            throw new NotGameDirectoryException(this.root, "Missing Resource00.pak");
        }

        //  We can reasonably assume this is a game directory now
    }

    public ModPackage getPackage(String id, Version version) throws IOException {
        Gson gson = this.getGson();
        Path moduleRepo = this.root.resolve("modkit").resolve("modpacks");
        Path modDir = moduleRepo.resolve(id);
        if (Files.isDirectory(modDir)) {
            Path info = modDir.resolve(version.toString()).resolve("modinfo.json");
            if (!Files.isRegularFile(info)) {
                throw new FileNotFoundException("Missing modinfo.json for " + id + " v" + version.toString());
            }

            try (BufferedReader reader = Files.newBufferedReader(info, StandardCharsets.UTF_8)) {
                ModPackage ret = gson.fromJson(reader, ModPackage.class);
                if (!version.equals(ret.getVersion())) {
                    throw new IllegalArgumentException("modinfo.json reports a different version than its module");
                }

                if (!id.equalsIgnoreCase(ret.getId())) {
                    throw new IllegalArgumentException("modinfo.json reports a different ID than its module");
                }

                ret.setKit(this);
                return ret;
            }
        } else {
            throw new FileNotFoundException(id);
        }
    }

    public ModPackage getLatest(String id) throws IOException {
        Gson gson = this.getGson();
        Path moduleRepo = this.root.resolve("modkit").resolve("modpacks");
        //  Check for a ZIP
        Path modZip = moduleRepo.resolve(id + ".zip");
        Path modDir = moduleRepo.resolve(id);
        if (Files.isRegularFile(modZip)) {
            ZipFile zipFile = new ZipFile(modZip.toFile());
            ZipEntry entry = zipFile.getEntry("modinfo.json");
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                ModPackage ret = gson.fromJson(new InputStreamReader(inputStream), ModPackage.class);
                if (!id.equalsIgnoreCase(ret.getId())) {
                    throw new IllegalArgumentException("modinfo.json reports a different ID than its module");
                }

                ret.setKit(this);
                return ret;
            }
        } else if (Files.isDirectory(modDir)) {
            Version latest = null;

            //  Look for a "latest" folder
            Path latestFolder = modDir.resolve("latest");
            boolean checkVer = true;
            String dirName = null;
            if (Files.isDirectory(latestFolder)) {
                checkVer = false;
                dirName = "latest";
            }

            if (dirName == null) {
                //  Find latest version
                try (Stream<Path> stream = Files.walk(modDir, 1)) {
                    latest = stream
                        .filter(Files::isDirectory)
                        .filter((d) -> !modDir.equals(d))
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .map(Version::valueOf)
                        .min(Comparator.reverseOrder())
                        .orElseThrow(() -> new FileNotFoundException("No versions found for " + id));

                    dirName = latest.toString();
                }
            }

            Path info = modDir.resolve(dirName).resolve("modinfo.json");
            if (!Files.isRegularFile(info)) {
                if (latest != null) {
                    throw new FileNotFoundException("Missing modinfo.json for " + id + " v" + latest.toString());
                } else {
                    throw new FileNotFoundException("Missing modinfo.json for " + id + " (latest)");
                }
            }

            try (BufferedReader reader = Files.newBufferedReader(info, StandardCharsets.UTF_8)) {
                ModPackage ret = gson.fromJson(reader, ModPackage.class);
                if (checkVer && !latest.equals(ret.getVersion())) {
                    throw new IllegalArgumentException("modinfo.json reports a different version than its module");
                }

                if (latest == null) {
                    ret.setLatest(true);
                }

                if (!id.equalsIgnoreCase(ret.getId())) {
                    throw new IllegalArgumentException("modinfo.json reports a different ID than its module");
                }

                ret.setKit(this);
                return ret;
            }
        } else {
            throw new FileNotFoundException(id);
        }
    }

    public byte[] getModPackageFile(ModPackage modPackage, String file) throws IOException {
        String id = modPackage.getId();
        Path moduleRepo = this.root.resolve("modkit").resolve("modpacks");
        //  Check for a ZIP
        Path modZip = moduleRepo.resolve(id + ".zip");
        String ver;
        if (modPackage.isLatest()) {
            ver = "latest";
        } else {
            ver = modPackage.getVersion().toString();
        }
        Path modDir = moduleRepo.resolve(id).resolve(ver);
        Path filePath = modDir.resolve(file);
        if (Files.isRegularFile(modZip)) {
            ZipFile zipFile = new ZipFile(modZip.toFile());
            ZipEntry entry = zipFile.getEntry(file);
            if (entry == null) {
                throw new FileNotFoundException(file);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8196];
            int read;
            try (InputStream in = zipFile.getInputStream(entry)) {
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
            }

            return out.toByteArray();
        } else if (Files.isDirectory(modDir) && Files.isRegularFile(filePath)) {
            return Files.readAllBytes(filePath);
        } else {
            throw new FileNotFoundException(id);
        }
    }

    public Path resolveModPackagePath(ModPackage modPackage, String path) throws IOException {
        String id = modPackage.getId();
        Path moduleRepo = this.root.resolve("modkit").resolve("modpacks");
        //  Check for a ZIP
        Path modZip = moduleRepo.resolve(id + ".zip");
        String ver;
        if (modPackage.isLatest()) {
            ver = "latest";
        } else {
            ver = modPackage.getVersion().toString();
        }
        Path modDir = moduleRepo.resolve(id).resolve(ver);
        Path filePath = modDir.resolve(path);
        if (Files.isRegularFile(modZip)) {
            final FileSystem zipFs = FileSystems.newFileSystem(modZip, null);
            return zipFs.getPath(path);
        } else if (Files.isDirectory(modDir) && Files.exists(filePath)) {
            return filePath;
        } else {
            throw new FileNotFoundException(id);
        }
    }

    public boolean modPackageHasFile(ModPackage modPackage, String file) {
        try {
            String id = modPackage.getId();
            Path moduleRepo = this.root.resolve("modkit").resolve("modpacks");
            //  Check for a ZIP
            Path modZip = moduleRepo.resolve(id + ".zip");
            String ver;
            if (modPackage.isLatest()) {
                ver = "latest";
            } else {
                ver = modPackage.getVersion().toString();
            }
            Path modDir = moduleRepo.resolve(id).resolve(ver);
            if (Files.isRegularFile(modZip)) {
                ZipFile zipFile = new ZipFile(modZip.toFile());
                ZipEntry entry = zipFile.getEntry(file);
                return entry != null;
            } else {
                return Files.isDirectory(modDir) && Files.exists(modDir.resolve(file));
            }
        } catch (Exception e) {
            return false;
        }
    }

    public ModPackage createModPackage(ModPackage packageDefinition)
        throws IOException, ConstraintViolationException {
        ModPackageConstraints.check(packageDefinition);

        ModPackage copy = new ModPackage();
        copy.setAuthor(packageDefinition.getAuthor());
        copy.setDescription(packageDefinition.getDescription());
        //  Don't use the build provided
        BuildInfo build = new BuildInfo();
        build.setKitVersion(KIT_VERSION);
        copy.setBuild(build);
        copy.setId(packageDefinition.getId());
        copy.setName(packageDefinition.getName());
        copy.setTimestamp(Instant.now());
        copy.setVersion(packageDefinition.getVersion());
        copy.setEris(packageDefinition.getEris());
        copy.setKit(this);

        Path moduleRepo = this.root.resolve("modkit").resolve("modpacks");
        Path modDir = moduleRepo.resolve(copy.getId());
        Files.createDirectories(modDir);
        Path versionDir = modDir.resolve(copy.getVersion().toString());
        Files.createDirectories(versionDir);
        Path defFile = versionDir.resolve("modinfo.json");

        try (BufferedWriter writer = Files.newBufferedWriter(defFile, StandardCharsets.UTF_8)) {
            this.getGson().toJson(copy, writer);
            writer.flush();
        }

        return copy;
    }

    public Gson getGson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Version.class, new VersionGsonAdapter())
            .registerTypeAdapter(Instant.class, new InstantGsonAdapter())
            .serializeNulls()
            .create();
    }
}
