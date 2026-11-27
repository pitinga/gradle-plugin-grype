package io.github.pitinga.grype

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger

import java.net.HttpURLConnection
import java.net.URI
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.AccessDeniedException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Downloads official Grype release artifacts, verifies SHA-256 and caches the executable.
 * Mirrors the installation principles of the Azure DevOps GrypeInstaller task.
 */
class GrypeInstaller {
    private static final long MAX_ARCHIVE_BYTES = 1_073_741_824L
    private static final long MAX_TEXT_BYTES = 5_242_880L
    private static final String RELEASES = 'https://github.com/anchore/grype/releases/download/'
    private final File gradleHome
    private final File projectRoot
    private final Logger logger
    private final GrypeExtension config

    GrypeInstaller(File gradleHome, File projectRoot, Logger logger, GrypeExtension config) {
        this.gradleHome = gradleHome
        this.projectRoot = projectRoot
        this.logger = logger
        this.config = config
    }

    File resolve() {
        if (config.preferInstalled) {
            File existing = onPath()
            if (existing != null) {
                logger.lifecycle('Using Grype from PATH: ' + existing.absolutePath)
                return existing
            }
        }

        String version = resolveVersion(config.version)
        String platform = platformId()
        List<Path> roots = [
            gradleHome.toPath().resolve('grype'),
            projectRoot.toPath().resolve('.gradle').resolve('grype')
        ]

        for (int i = 0; i < roots.size(); i++) {
            Path root = roots[i]
            try {
                return cachedOrDownload(root, version, platform).toFile()
            } catch (Exception ex) {
                if (i == 0 && permissionError(ex)) {
                    logger.warn('Shared Gradle Grype cache is not writable; falling back to project .gradle/grype: ' + ex.message)
                    continue
                }
                throw new GradleException('Unable to prepare Grype in ' + root + ': ' + ex.message, ex)
            }
        }
        throw new GradleException('Unable to prepare Grype.')
    }

    private Path cachedOrDownload(Path root, String version, String platform) {
        String binaryName = platform.startsWith('windows-') ? 'grype.exe' : 'grype'
        Path targetDir = root.resolve(version).resolve(platform)
        Path target = targetDir.resolve(binaryName)
        if (verified(targetDir, target)) {
            logger.lifecycle('Using cached Grype ' + version + ': ' + target)
            return target
        }
        if (!config.autoInstall) {
            throw new GradleException('Grype was not found in PATH or the cache, and autoInstall is disabled.')
        }

        Files.createDirectories(root)
        synchronized (GrypeInstaller.class) {
            Path lockPath = root.resolve('.install.lock')
            FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            try {
                FileLock lock = channel.lock()
                try {
                    if (verified(targetDir, target)) return target
                    return downloadInstall(root, version, platform, binaryName, targetDir)
                } finally {
                    lock.release()
                }
            } finally {
                channel.close()
            }
        }
    }

    private Path downloadInstall(Path root, String version, String platform, String binaryName, Path targetDir) {
        String asset = archiveName(version, platform)
        Path staging = Files.createTempDirectory(root, '.grype-staging-')
        try {
            URI base = URI.create(RELEASES + 'v' + version + '/')
            String checksums = new String(fetch(base.resolve('grype_' + version + '_checksums.txt'), MAX_TEXT_BYTES), StandardCharsets.UTF_8)
            String expected = checksumFor(checksums, asset)
            Path archive = staging.resolve(asset)
            logger.lifecycle('Downloading Grype ' + version + ' (' + platform + ') from official GitHub Releases')
            Files.write(archive, fetch(base.resolve(asset), MAX_ARCHIVE_BYTES),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            String actual = hexSha256(archive)
            if (!actual.equalsIgnoreCase(expected)) {
                throw new GradleException('Grype archive SHA-256 mismatch for ' + asset + '. Installation aborted.')
            }

            Path extracted = staging.resolve('extracted')
            Files.createDirectories(extracted)
            extractBinary(archive, extracted, binaryName, platform)
            Path bin = extracted.resolve(binaryName)
            if (!Files.isRegularFile(bin, LinkOption.NOFOLLOW_LINKS)) {
                throw new GradleException('Expected Grype executable was not present in the verified archive.')
            }
            if (!platform.startsWith('windows-') && !bin.toFile().setExecutable(true, false)) {
                throw new GradleException('Unable to mark the Grype binary executable: ' + bin)
            }
            Files.writeString(extracted.resolve('.verified'), hexSha256(bin) + System.lineSeparator(), StandardCharsets.UTF_8)

            Files.createDirectories(targetDir.parent)
            if (Files.exists(targetDir)) deleteTree(targetDir) // remove an incomplete prior installation
            try {
                Files.move(extracted, targetDir, StandardCopyOption.ATOMIC_MOVE)
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(extracted, targetDir)
            }
            logger.lifecycle('Grype ' + version + ' installed and SHA-256 verified: ' + targetDir)
            return targetDir.resolve(binaryName)
        } finally {
            deleteTree(staging)
        }
    }

    private static boolean verified(Path targetDir, Path binary) {
        Files.isRegularFile(binary, LinkOption.NOFOLLOW_LINKS) &&
            Files.isRegularFile(targetDir.resolve('.verified'), LinkOption.NOFOLLOW_LINKS) &&
            Files.readString(targetDir.resolve('.verified')).trim() == hexSha256(binary)
    }

    private static void extractBinary(Path archive, Path destination, String binaryName, String platform) {
        if (platform.startsWith('windows-')) {
            boolean found = false
            ZipInputStream zin = new ZipInputStream(Files.newInputStream(archive))
            try {
                def entry
                while ((entry = zin.nextEntry) != null) {
                    if (!entry.directory && entry.name == binaryName) {
                        Files.copy(zin, destination.resolve(binaryName), StandardCopyOption.REPLACE_EXISTING)
                        found = true
                        break
                    }
                    zin.closeEntry()
                }
            } finally {
                zin.close()
            }
            if (!found) throw new GradleException('Grype executable not found in ZIP archive.')
        } else {
            Process process = new ProcessBuilder('tar', '-xzf', archive.toString(), '-C', destination.toString(), binaryName)
                .redirectErrorStream(true).start()
            // Read error output while waiting so the subprocess cannot block on a full pipe.
            String details = process.inputStream.getText('UTF-8')
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                throw new GradleException('Timed out extracting Grype archive with tar.')
            }
            if (process.exitValue() != 0) {
                throw new GradleException('Unable to extract Grype: ' + details.trim())
            }
        }
    }

    static String platformId() {
        String os = System.getProperty('os.name').toLowerCase(Locale.ROOT)
        String arch = System.getProperty('os.arch').toLowerCase(Locale.ROOT)
        String cpu = (arch in ['amd64', 'x86_64']) ? 'amd64' : (arch in ['aarch64', 'arm64'] ? 'arm64' : null)
        if (cpu == null) throw new GradleException('Unsupported Grype architecture: ' + arch)
        if (os.contains('win')) {
            if (cpu != 'amd64') throw new GradleException('Windows ARM64 Grype package is not supported.')
            return 'windows-amd64'
        }
        if (os.contains('mac')) return 'macos-' + cpu
        if (os.contains('linux')) return 'linux-' + cpu
        throw new GradleException('Unsupported Grype operating system: ' + os)
    }

    static String archiveName(String version, String platform) {
        String suffix = [
            'windows-amd64': 'windows_amd64.zip',
            'linux-amd64'  : 'linux_amd64.tar.gz',
            'linux-arm64'  : 'linux_arm64.tar.gz',
            'macos-amd64'  : 'darwin_amd64.tar.gz',
            'macos-arm64'  : 'darwin_arm64.tar.gz'
        ][platform]
        if (!suffix) throw new GradleException('Unsupported Grype platform: ' + platform)
        return 'grype_' + version + '_' + suffix
    }

    static String checksumFor(String contents, String asset) {
        for (String line : contents.readLines()) {
            def matcher = (line =~ /^([a-fA-F0-9]{64})\s+\*?(.+)$/)
            if (matcher.matches() && matcher.group(2).trim() == asset) {
                return matcher.group(1).toLowerCase(Locale.ROOT)
            }
        }
        throw new GradleException('Official SHA-256 entry not found for ' + asset)
    }

    private static String hexSha256(Path file) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        Files.newInputStream(file).withCloseable { stream ->
            byte[] buffer = new byte[64 * 1024]
            int count
            while ((count = stream.read(buffer)) != -1) digest.update(buffer, 0, count)
        }
        return HexFormat.of().formatHex(digest.digest())
    }

    private static String resolveVersion(String requested) {
        if (requested == null) throw new GradleException('grype.version is required')
        String version = requested.trim().replaceFirst(/^v/, '')
        if (version == 'latest') {
            def metadata = new JsonSlurper().parseText(new String(
                fetch(URI.create('https://api.github.com/repos/anchore/grype/releases/latest'), MAX_TEXT_BYTES),
                StandardCharsets.UTF_8
            ))
            if (metadata.draft || metadata.prerelease) throw new GradleException('Latest Grype GitHub release is not stable.')
            version = metadata.tag_name?.toString()?.replaceFirst(/^v/, '')
        }
        if (!(version ==~ /\d+\.\d+\.\d+/)) {
            throw new GradleException('grype.version must be latest or a stable version, for example 0.122.0')
        }
        return version
    }

    private static File onPath() {
        String searchPath = System.getenv('PATH') ?: ''
        String name = System.getProperty('os.name').toLowerCase(Locale.ROOT).contains('win') ? 'grype.exe' : 'grype'
        for (String dir : searchPath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!dir) continue
            File candidate = new File(dir, name)
            if (candidate.isFile() && (name.endsWith('.exe') || candidate.canExecute())) return candidate
        }
        return null
    }

    private static boolean permissionError(Throwable error) {
        Throwable current = error
        while (current != null) {
            if (current instanceof AccessDeniedException || current instanceof SecurityException) return true
            if (current instanceof FileSystemException) {
                String reason = (current.reason ?: current.message ?: '').toLowerCase(Locale.ROOT)
                if (reason.contains('permission denied') || reason.contains('access is denied') ||
                    reason.contains('read-only') || reason.contains('operation not permitted')) return true
            }
            current = current.cause
        }
        return false
    }

    private static void deleteTree(Path folder) {
        if (folder == null || !Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) return
        def walk = Files.walk(folder)
        try {
            walk.sorted(Comparator.reverseOrder()).forEach { Path file -> Files.deleteIfExists(file) }
        } finally {
            walk.close()
        }
    }

    private static byte[] fetch(URI initial, long maxBytes) {
        URI uri = initial
        for (int redirects = 0; redirects <= 8; redirects++) {
            if (uri.scheme != 'https' || uri.userInfo != null || !approvedHost(uri.host)) {
                throw new GradleException('Unsafe Grype download destination: ' + uri)
            }
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection()
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 20_000
            conn.readTimeout = 120_000
            conn.setRequestProperty('User-Agent', 'gradle-plugin-grype/0.1.0')
            conn.setRequestProperty('Accept', 'application/vnd.github+json')
            if (uri.host == 'api.github.com' && System.getenv('GITHUB_TOKEN')) {
                conn.setRequestProperty('Authorization', 'Bearer ' + System.getenv('GITHUB_TOKEN'))
            }
            try {
                int status = conn.responseCode
                if (status in [301, 302, 303, 307, 308]) {
                    String location = conn.getHeaderField('Location')
                    if (!location) throw new GradleException('GitHub redirect without Location header.')
                    uri = uri.resolve(location)
                    continue
                }
                if (status != 200) throw new GradleException('GitHub download failed: HTTP ' + status + ' for ' + uri)
                ByteArrayOutputStream output = new ByteArrayOutputStream()
                conn.inputStream.withCloseable { stream ->
                    byte[] buffer = new byte[64 * 1024]
                    long total = 0
                    int count
                    while ((count = stream.read(buffer)) != -1) {
                        total += count
                        if (total > maxBytes) throw new GradleException('Grype download exceeded maximum allowed size.')
                        output.write(buffer, 0, count)
                    }
                }
                return output.toByteArray()
            } finally {
                conn.disconnect()
            }
        }
        throw new GradleException('Too many GitHub release redirects.')
    }

    private static boolean approvedHost(String host) {
        host != null && (host == 'github.com' || host == 'api.github.com' ||
            host == 'githubusercontent.com' || host.endsWith('.githubusercontent.com'))
    }
}
