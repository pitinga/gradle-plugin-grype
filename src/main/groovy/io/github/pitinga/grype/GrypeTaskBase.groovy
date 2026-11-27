package io.github.pitinga.grype

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations
import org.gradle.util.GradleVersion
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/**
 * Provides Grype configuration without accessing Project at task execution time.
 */
@DisableCachingByDefault(because = 'Grype tasks resolve external tools and do not declare cacheable output artifacts')
abstract class GrypeTaskBase extends DefaultTask {
    @Inject
    abstract ExecOperations getExecOperations()

    @Input
    abstract Property<String> getGrypeVersion()

    @Input
    abstract Property<Boolean> getAutoInstall()

    @Input
    abstract Property<Boolean> getPreferInstalled()

    @Internal
    abstract DirectoryProperty getGradleHomeDirectory()

    @Internal
    abstract DirectoryProperty getRootDirectory()

    protected File resolveExecutable() {
        // Task inputs are snapshots of the extension values configured by the build.
        GrypeExtension options = new GrypeExtension()
        options.version = grypeVersion.get()
        options.autoInstall = autoInstall.get()
        options.preferInstalled = preferInstalled.get()
        return new GrypeInstaller(gradleHomeDirectory.get().asFile, rootDirectory.get().asFile, logger, options).resolve()
    }

    /**
     * Version is embedded into the distributed plugin artifact during processResources.
     * It must not be inferred from the consuming project's version or the Grype CLI.
     */
    static String pluginArtifactVersion() {
        InputStream input = GrypeTaskBase.class.getResourceAsStream('plugin.properties')
        if (input == null) return 'unknown'
        Properties metadata = new Properties()
        input.withCloseable { metadata.load(it) }
        String version = metadata.getProperty('version', '').trim()
        return version ?: 'unknown'
    }

    static String parseGrypeCliVersion(String output) {
        def matcher = (output ?: '') =~ /(?m)^\s*Version:\s*(\S+)/
        return matcher.find() ? matcher.group(1) : 'unknown'
    }

    /**
     * Query the actual resolved executable, which may differ from grype.version
     * when preferInstalled=true selects a binary from PATH.
     * Version reporting is informational and never hides a scan error.
     */
    protected String detectedGrypeCliVersion(File executable) {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream()
        ByteArrayOutputStream standardError = new ByteArrayOutputStream()
        try {
            def result = execOperations.exec { spec ->
                spec.executable = executable.absolutePath
                spec.args('version')
                spec.standardOutput = standardOutput
                spec.errorOutput = standardError
                spec.ignoreExitValue = true
            }
            if (result.exitValue != 0) {
                logger.warn('Unable to determine Grype CLI version: version exited with code ' + result.exitValue)
                return 'unknown'
            }
            return parseGrypeCliVersion(standardOutput.toString('UTF-8'))
        } catch (Exception ex) {
            logger.warn('Unable to determine Grype CLI version: ' + ex.message)
            return 'unknown'
        }
    }

    protected void logVersionHeader(File executable) {
        String line = '=================================================='
        logger.lifecycle(line)
        logger.lifecycle('Gradle Grype Plugin : ' + pluginArtifactVersion())
        logger.lifecycle('Grype CLI           : ' + detectedGrypeCliVersion(executable))
        logger.lifecycle('Gradle              : ' + GradleVersion.current().version)
        logger.lifecycle('Java                : ' + System.getProperty('java.version', 'unknown'))
        logger.lifecycle(line)
    }
}
