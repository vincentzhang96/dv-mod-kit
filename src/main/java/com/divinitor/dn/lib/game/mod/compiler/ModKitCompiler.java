package com.divinitor.dn.lib.game.mod.compiler;

import com.divinitor.dn.lib.game.mod.CompileException;
import com.divinitor.dn.lib.game.mod.DnAssetAccessService;
import com.divinitor.dn.lib.game.mod.ModKit;
import com.divinitor.dn.lib.game.mod.UnsupportedVersionException;
import com.divinitor.dn.lib.game.mod.definition.BuildInfo;
import com.divinitor.dn.lib.game.mod.definition.CopyFromGameDirective;
import com.divinitor.dn.lib.game.mod.definition.CopyFromPackDirective;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.pak.ManagedPak;
import com.divinitor.dn.lib.game.mod.pak.ManagedPakIndexEntry;
import com.divinitor.dn.lib.game.mod.util.VersionCached;
import com.google.common.collect.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.divinitor.dn.lib.game.mod.compiler.ModCompiler.gameSource;
import static com.divinitor.dn.lib.game.mod.compiler.ModCompiler.packSource;

public class ModKitCompiler implements VersionCached.Versioned, ModCompiler {

    private final ModKit kit;
    private final DnAssetAccessService assetAccessService;
    private final VersionCached<BuildComputeResults> buildComputeResults;
    private final VersionCached<List<FileBuildStep>> buildSteps;
    /**
     * Internal state versioning. Used to determine if build lists need to be rebuilt.
     */
    private int stateVersion;
    private List<ModPackage> modPacks;
    private Map<String, ModPackage> modPackMap;

    public ModKitCompiler(ModKit kit) {
        this.kit = kit;
        buildComputeResults = new VersionCached<>(this);
        buildSteps = new VersionCached<>(this);
        modPacks = new LinkedList<>();
        modPackMap = new HashMap<>();
        assetAccessService = kit.getAssetAccessService();
    }

    public void addPackage(ModPackage modPackage) {
        checkKitCompilerVersion(modPackage);

        markDirty();
        //  TODO
    }

    public ModPackage removePackage(String packageId) {
        markDirty();
        //  TODO
        return null;
    }

    public void updatePackage(ModPackage modPackage) {
        checkKitCompilerVersion(modPackage);

        markDirty();
        //  TODO
    }

    public ModPackage addOrUpdatePackage(ModPackage modPackage) {
        checkKitCompilerVersion(modPackage);

        markDirty();
        //  TODO
        return null;
    }

    private void checkKitCompilerVersion(ModPackage modPackage) throws UnsupportedVersionException {
        BuildInfo build = modPackage.getBuild();
        if (build.getKitVersion().compareTo(ModKit.KIT_VERSION) > 0) {
            throw new UnsupportedVersionException(ModKit.KIT_VERSION, build.getKitVersion());
        }
    }

    public void markDirty() {
        ++stateVersion;
    }

    @Override
    public BuildComputeResults compute() {
        if (buildComputeResults.isValid()) {
            return buildComputeResults.get();
        }

        BuildComputeResults results = new BuildComputeResults();
        results.missing = MultimapBuilder.hashKeys().hashSetValues().build();
        results.conflicts = new ArrayList<>();
        results.rejected = MultimapBuilder.hashKeys().hashSetValues().build();
        results.steps = new ArrayList<>();

        SetMultimap<String, String> destinationFiles = MultimapBuilder.hashKeys().linkedHashSetValues().build();
        List<FileBuildStep> steps = results.steps;

        for (ModPackage modPack : modPacks) {
            BuildInfo build = modPack.getBuild();
            for (CopyFromGameDirective directive : build.getCopy()) {
                String src = directive.getSource();
                if (!assetAccessService.contains(src)) {
                    results.missing.put(modPack.getId(), "pak::" + src);
                    continue;
                }

                String dest = directive.getDest();
                destinationFiles.put(dest, modPack.getId());

                steps.add(new FileBuildStep(modPack, dest, gameSource(assetAccessService, src)));
            }

            for (CopyFromPackDirective directive : build.getAdd()) {
                String src = directive.getSource();
                if (!modPack.hasAsset(src)) {
                    results.missing.put(modPack.getId(), "mod::" + src);
                    continue;
                }

                String dest = directive.getDest();
                destinationFiles.put(dest, modPack.getId());

                steps.add(new FileBuildStep(modPack, dest, packSource(modPack, src)));
            }

            //  TODO table edits
        }

        destinationFiles.asMap().entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .forEach((e) -> results.conflicts.add(BuildConflict.builder()
                .file(e.getKey())
                .conflictingModIds(new LinkedHashSet<>(e.getValue()))
                .build()));

        buildComputeResults.set(results);
        return results;
    }

    /**
     * Compiles the managed pak. Active mods are retained, new mods are added, and deleted mods removed.
     *
     * @throws CompileException If there was an error compiling.
     */
    @Override
    public void compile() throws CompileException {
        if (!buildComputeResults.isValid()) {
            this.compute();
        }

        BuildComputeResults results = buildComputeResults.get();

        List<FileBuildStep> steps = results.steps;

        Path targetPak = kit.getRoot().resolve("00Resource_ModKit01.pak");



        Set<String> targetFiles = steps.stream().map(FileBuildStep::getDestination).collect(Collectors.toSet());

        ManagedPak mPak = new ManagedPak(); //  TODO
        RangeSet<Integer> freeSpace = TreeRangeSet.create();
        freeSpace.add(Range.closed(
            ManagedPak.SIZEOF_HEADER,
            Math.min(mPak.getFileIndexTableOffset(), mPak.getModPackIndexTableOffset())));

        //  First, perform a sweep to see if there are any files that need to be deleted/reclaimed
        List<ManagedPakIndexEntry> toRemove = new ArrayList<>();
        for (ManagedPakIndexEntry entry : mPak.getFileIndex()) {
            if (!targetFiles.contains(entry.getFilePath())) {
                toRemove.add(entry);
            } else {
                freeSpace.remove(Range.closedOpen(entry.getOffset(), entry.getOffset() + entry.getRawSize()));
            }
        }

        //  For each step, check if we have the destination file already and if it has the right content
        //  If its already there, we don't need to do anything
        //  If it isn't, go ahead and write it in






    }

    @Override
    public int getVersion() {
        return stateVersion;
    }

}
