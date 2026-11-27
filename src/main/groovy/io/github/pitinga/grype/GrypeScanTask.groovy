package io.github.pitinga.grype

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@DisableCachingByDefault(because = 'Grype results depend on mutable external databases')
abstract class GrypeScanTask extends GrypeTaskBase {
    @Optional @InputFile @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getArtifactFile()
    @Internal abstract DirectoryProperty getWorkingDirectory()
    @Input abstract ListProperty<String> getNativeOptions()
    @Input abstract ListProperty<String> getCommand()
    @Input abstract ListProperty<String> getArguments()
    @Input abstract Property<String> getScanTarget()
    @Input abstract Property<Boolean> getBuildArtifact()
    @Internal abstract MapProperty<String,String> getNativeEnvironment()

    @TaskAction
    void scan() {
        File directory = workingDirectory.get().asFile
        List<String> args = new ArrayList<>(command.get())
        List<String> options = new ArrayList<>(nativeOptions.get())
        String target = scanTarget.get()
        if (buildArtifact.get()) target = GrypeArguments.target(target,artifactFile.orNull?.asFile)
        // Respect native output settings, including repeated --output format=path.
        // Do not reinterpret URI/image prefixes as Windows paths.
        if (!command.get() && target && options.any { it.startsWith('--file=') }) {
            String reportName = options.findAll { it.startsWith('--file=') }.last().substring(7)
            if (reportName) {
                File report = new File(reportName)
                if (!report.absolute) report = new File(directory,reportName)
                String local = target.replaceFirst(/^(dir|file|sbom|docker-archive|oci-archive|oci-dir|singularity|purl|cpes|zarf):/,'')
                boolean pathTarget = local != target || !target.contains(':') || (target ==~ /^[A-Za-z]:[\\\/].*/)
                if (pathTarget) {
                    File input = new File(local)
                    if (!input.absolute) input = new File(directory,local)
                    if (input.canonicalFile == report.canonicalFile) throw new GradleException('--file must not overwrite scan target')
                }
                Files.createDirectories(report.absoluteFile.parentFile.toPath())
            }
        }
        args.addAll(options)
        List<String> positional = new ArrayList<>(arguments.get())
        if (target) positional.add(target)
        if (positional) {
            args.add('--')
            args.addAll(positional)
        }
        File executable = resolveExecutable()
        logVersionHeader(executable)
        def result = execOperations.exec { spec ->
            spec.executable = executable.absolutePath
            spec.args(args)
            spec.workingDir = directory
            spec.environment(nativeEnvironment.get())
            spec.ignoreExitValue = true
        }
        if (result.exitValue != 0) throw new GradleException(result.exitValue == 2 ? 'Grype vulnerability threshold exceeded (exit code 2)' : 'Grype command failed (exit code ' + result.exitValue + ')')
    }
}
