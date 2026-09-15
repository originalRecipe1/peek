package io.github.originalrecipe1.unfurlit.buildlogic;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

/** Keeps dependency metadata/transitives intact; never modifies the downloaded AAR. */
@CacheableTransform
public abstract class TrimPythonRuntime implements TransformAction<TransformParameters.None> {
    @InputArtifact
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract Provider<FileSystemLocation> getInputArtifact();

    private static boolean isRuntime(String name) {
        return name.matches("jni/[^/]+/libpython\\.zip\\.so");
    }

    @Override
    public void transform(TransformOutputs outputs) {
        File input = getInputArtifact().get().getAsFile();
        try (ZipFile source = ZipFile.builder().setFile(input).get()) {
            boolean containsRuntime = java.util.Collections.list(source.getEntries()).stream()
                .anyMatch(entry -> isRuntime(entry.getName()));
            if (!containsRuntime) {
                outputs.file(input);
                return;
            }
            File output = outputs.file("trimmed-" + input.getName());
            Path originalRuntime = Files.createTempFile(output.toPath().getParent(), "runtime-", ".zip");
            Path trimmedRuntime = Files.createTempFile(output.toPath().getParent(), "trimmed-", ".zip");
            try (ZipArchiveOutputStream destination = new ZipArchiveOutputStream(output)) {
                var entries = source.getEntriesInPhysicalOrder();
                while (entries.hasMoreElements()) {
                    ZipArchiveEntry entry = entries.nextElement();
                    if (!isRuntime(entry.getName())) {
                        PythonRuntimeArchive.copyRaw(source, entry, destination);
                        continue;
                    }
                    try (var stream = source.getInputStream(entry)) {
                        Files.copy(stream, originalRuntime, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    PythonRuntimeArchive.trim(originalRuntime, trimmedRuntime);
                    // The outer entry has new content, so discard its old CRC/size.
                    ZipArchiveEntry replacement = new ZipArchiveEntry(entry.getName());
                    replacement.setTime(entry.getTime());
                    replacement.setUnixMode(entry.getUnixMode());
                    destination.putArchiveEntry(replacement);
                    Files.copy(trimmedRuntime, destination);
                    destination.closeArchiveEntry();
                }
            } finally {
                Files.deleteIfExists(originalRuntime);
                Files.deleteIfExists(trimmedRuntime);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Could not trim Python runtime in " + input.getName(), error);
        }
    }
}
