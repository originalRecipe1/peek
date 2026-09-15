# Commons Compress registers ZIP extra-field classes through Class.newInstance().
# yt-dlp uses this path to unpack Python on first use. Keep the constructors,
# while still allowing class renaming and optimization of their implementations.
-keep,allowobfuscation,allowoptimization class org.apache.commons.compress.archivers.zip.** implements org.apache.commons.compress.archivers.zip.ZipExtraField {
    public <init>();
}
