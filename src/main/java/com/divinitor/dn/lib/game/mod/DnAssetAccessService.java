package com.divinitor.dn.lib.game.mod;

import co.phoenixlab.dn.pak.PakIndexEntry;
import co.phoenixlab.dn.pak.PakReader;
import com.divinitor.dn.lib.game.mod.util.Lockable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import gnu.trove.impl.Constants;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.divinitor.dn.lib.game.mod.util.Lockable.lock;
import static com.divinitor.dn.lib.game.mod.util.Utils.sneakyConsumer;

public class DnAssetAccessService {

        public static final int INITIAL_CAPACITY = 200000;
        private final TObjectIntMap<String> index;
        private final Map<String, PakIndexEntry> entryIndex;
        private final TIntObjectMap<PakReader> loadedPaks;
        private int pakIndexCounter;
        private final Cache<String, byte[]> dataCache;
        private final Map<String, String> relativeIndex;
        private final List<PakIndexEntry> invalidEntries;
        private final Path root;
        private final ReadWriteLock lock;

        public DnAssetAccessService(Path root) {
            this.lock = new ReentrantReadWriteLock();
            this.root = root;
            pakIndexCounter = 0;
            loadedPaks = new TIntObjectHashMap<>(15, Constants.DEFAULT_LOAD_FACTOR, -1);
            index = new TObjectIntHashMap<>(INITIAL_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
            entryIndex = new HashMap<>(INITIAL_CAPACITY);
            relativeIndex = new HashMap<>(INITIAL_CAPACITY);
            dataCache = CacheBuilder.newBuilder()
                .maximumWeight(50L * 1024L * 1024L) //  Max 50 MB cached
                .weigher((Weigher<String, byte[]>) (key, value) -> value.length)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build();
            invalidEntries = new ArrayList<>();
        }

        public void clear() {
            try (Lockable lck = lock(lock.writeLock())) {
                index.clear();
                loadedPaks.clear();
                dataCache.invalidateAll();
                pakIndexCounter = 0;
                invalidEntries.clear();
            }
        }

        public void indexPaks() throws IOException {
            try (Lockable lck = lock(lock.writeLock())) {
                Path pakDir = root;
                if (!Files.isDirectory(pakDir)) {
                    throw new IllegalArgumentException("Not a valid directory");
                }

                //  Resources
                List<Path> resources;
                try (Stream<Path> walk = Files.walk(pakDir, 1)) {
                    resources = walk.filter(p -> getPathFileNameStr(p).matches("^Resource[0-9][0-9]\\.pak$"))
                        .collect(Collectors.toList());
                }
                //  Sort in order (filesystem stream doesn't guarantee ordering)
                resources.sort((p1, p2) -> getPathFileNameStr(p1).compareToIgnoreCase(getPathFileNameStr(p2)));
                //  Index each
                resources.forEach(sneakyConsumer(this::indexPak));
            }
        }

        private String getPathFileNameStr(Path path) {
            return path.getFileName().toString();
        }

        private void indexPak(Path pak) throws IOException {
            try (Lockable lck = lock(lock.writeLock())) {
                Objects.requireNonNull(pak);
                if (!Files.isRegularFile(pak)) {
                    throw new IllegalArgumentException("Not a valid file");
                }
                PakReader reader = new PakReader(pak);
                reader.open();
                ++pakIndexCounter;
                int index = pakIndexCounter;
                loadedPaks.put(index, reader);
                for (PakIndexEntry pakIndexEntry : reader.getPakInfo().getFileIndex()) {
                    if (pakIndexEntry.getRealSize() != 0) {
                        indexPath(pakIndexEntry, index);
                    } else {
                        invalidEntries.add(pakIndexEntry);
                    }
                }
            }
        }

        public int getVersion() throws IOException {
            try (Lockable lck = lock(lock.readLock())){
                String version = new String(getAsset("version.cfg"), StandardCharsets.UTF_8);
                version = version.split("\n", 2)[0].trim();
                version = version.substring("version ".length());
                return Integer.parseInt(version);
            } catch (NoSuchElementException nsee) {
                throw new IOException("Unable to determine local version");
            }
        }

        private void indexPath(PakIndexEntry entry, final int pakIndex) {
            try (Lockable lck = lock(lock.writeLock())) {
                String file = entry.getFilePath();
                file = file.toLowerCase();
                String[] parts = file.split("\\\\");
                String filename = parts[parts.length - 1];
                index.putIfAbsent(file, pakIndex);
                entryIndex.putIfAbsent(file, entry);
                //  ONLY INDEX FILES UNDER CERTAIN DIRECTORIES
                //  Easier to just blacklist
                //  Ignore \mapdata\grid
                if (!file.startsWith("\\mapdata\\grid\\")) {
                    relativeIndex.putIfAbsent(filename, file);
                }
            }
        }

        public byte[] getAsset(final String path) throws IOException {
            try (Lockable lck = lock(lock.readLock())) {
                Objects.requireNonNull(path);
                String resolvedPath = resolve(path.toLowerCase());
                //  Cache check
                byte[] ret = dataCache.getIfPresent(resolvedPath);
                if (ret != null) {
                    return ret;
                }
                int parIndex = index.get(resolvedPath);
                if (parIndex == -1) {
                    throw new FileNotFoundException(path);
                }
                PakIndexEntry pie = entryIndex.get(resolvedPath);
                PakReader reader = loadedPaks.get(parIndex);
                if (reader == null) {
                    throw new IllegalStateException("Index entry refers to invalid pak for path " + path
                        + ": No pak at " + parIndex);
                }
                //  We have to use our index entry because the path resolver in PakReader will grab the first,
                //  even if it isnt necessarily valid (e.g. zero decomp size)
                ret = reader.getSubfileData(pie);
                dataCache.put(resolvedPath, ret);
                return ret;
            }
        }

        public boolean contains(String path) {
            try (Lockable lck = lock(lock.readLock())) {
                return entryIndex.containsKey(resolve(path));
            } catch (FileNotFoundException e) {
                return false;
            }
        }

        public String resolve(String path) throws FileNotFoundException {
            try (Lockable lck = lock(lock.readLock())) {
                //  Replace slash with backslash
                path = path.replaceAll("[/!]", Matcher.quoteReplacement("\\")).toLowerCase();
                if (path.startsWith("\\")) {
                    //  Path is already absolute
                    return path;
                }
                //  Look up the filename in the relative index
                String lookup = relativeIndex.get(path);
                if (lookup == null) {
                    //  We could do an expensive search through the entire index, oooooorrrr
                    throw new FileNotFoundException(path);
                }
                return lookup;
            }
        }

        public Stream<String> assetPaths() {
            return getAssetPaths().stream();
        }

        public Set<String> getAssetPaths() {
            HashSet<String> strings;
            try (Lockable lck = lock(lock.readLock())) {
                strings = new HashSet<>(index.keySet());
            }
            return strings;
        }

        public List<PakIndexEntry> getInvalidEntries() {
            try (Lockable lck = lock(lock.readLock())) {
                return Collections.unmodifiableList(invalidEntries);
            }
        }

        public Map<String, PakIndexEntry> getEntryIndex() {
            try (Lockable lck = lock(lock.readLock())) {
                return Collections.unmodifiableMap(entryIndex);
            }
        }
    }
