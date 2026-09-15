package io.github.originalrecipe1.unfurlit.buildlogic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;

/** Removes build/test files while preserving compressed payloads and Unix metadata. */
public final class PythonRuntimeArchive {
    private PythonRuntimeArchive() {}

    // Deliberately exact: a Python version/layout change must be reviewed.
    public static final Set<String> REMOVED_PATHS = Set.of(
        "usr/lib/quickjs/libquickjs.a",
        "usr/lib/python3.12/lib-dynload/_testbuffer.cpython-312.so",
        "usr/lib/python3.12/lib-dynload/_testcapi.cpython-312.so",
        "usr/lib/python3.12/lib-dynload/_testsinglephase.cpython-312.so",
        "usr/lib/python3.12/lib-dynload/_testimportmultiple.cpython-312.so",
        "usr/lib/python3.12/lib-dynload/_testclinic.cpython-312.so",
        "usr/lib/python3.12/lib-dynload/_testinternalcapi.cpython-312.so",
        "usr/lib/python3.12/lib-dynload/_testmultiphase.cpython-312.so"
    );

    public static void trim(Path input, Path output) throws IOException {
        Set<String> missing = new HashSet<>(REMOVED_PATHS);
        try (ZipFile source = ZipFile.builder().setPath(input).get();
             ZipArchiveOutputStream destination = new ZipArchiveOutputStream(output)) {
            var entries = source.getEntriesInPhysicalOrder();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (REMOVED_PATHS.contains(entry.getName())) {
                    missing.remove(entry.getName());
                } else {
                    copyRaw(source, entry, destination);
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new IOException("Python runtime layout changed; review the trim list. Missing: " + missing);
        }
    }

    static void copyRaw(ZipFile source, ZipArchiveEntry entry,
                        ZipArchiveOutputStream destination) throws IOException {
        try (InputStream compressed = source.getRawInputStream(entry)) {
            destination.addRawArchiveEntry(new ZipArchiveEntry(entry), compressed);
        }
    }
}
