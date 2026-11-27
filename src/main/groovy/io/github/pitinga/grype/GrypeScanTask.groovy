package io.github.pitinga.grype
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.StandardCopyOption
@DisableCachingByDefault(because = 'Vulnerability results depend on mutable external databases')
abstract class GrypeScanTask extends GrypeTaskBase {
    @Optional @InputFile @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getArtifactFile()
    @Internal abstract DirectoryProperty getWorkingDirectory()
    @Input abstract MapProperty<String,String> getOptions()
    @Input abstract ListProperty<String> getExcludePaths()
    @Input abstract ListProperty<String> getFromSources()
    @Input abstract ListProperty<String> getConfigFiles()
    @Input abstract ListProperty<String> getProfiles()
    @Input abstract ListProperty<String> getVexFiles()
    @Input abstract ListProperty<String> getAdditionalArguments()
    @Input abstract ListProperty<String> getAdditionalFormats()
    @TaskAction
    void scan() {
        Map config = options.get()
        File directory = workingDirectory.get().asFile
        Map listValues = [excludePaths: new ArrayList<String>(excludePaths.get()), fromSources: new ArrayList<String>(fromSources.get()), configFiles: new ArrayList<String>(configFiles.get()), profiles: new ArrayList<String>(profiles.get()), vexFiles: new ArrayList<String>(vexFiles.get()), additionalArguments: new ArrayList<String>(additionalArguments.get()), additionalFormats: new ArrayList<String>(additionalFormats.get())]
        String source = GrypeArguments.source(config,directory,artifactFile.orNull?.asFile,listValues.fromSources)
        if (source.startsWith('dir:')) {
            File target = new File(source.substring(4))
            if (!target.absolute) target = new File(directory,target.path)
            def root = target.canonicalFile.toPath()
            [new File(rootDirectory.get().asFile,'.gradle/grype'),new File(gradleHomeDirectory.get().asFile,'grype')].each { cache ->
                def path = cache.canonicalFile.toPath()
                if (path.startsWith(root) && path != root) {
                    String pattern = './' + root.relativize(path).toString().replace('\\','/') + '/**'
                    if (!listValues.excludePaths.contains(pattern)) listValues.excludePaths.add(pattern)
                }
            }
        }
        List<String> args = GrypeArguments.build(config,listValues)
        String format = GrypeReportOutput.checkedFormat(config.format)
        File report = GrypeReportOutput.destination(directory,config.outputFile)
        if (report != null) {
            String local = source.replaceFirst(/^(file|sbom|docker-archive|oci-archive|singularity|purl|cpes|zarf):/,'')
            File target = new File(local)
            if (!target.absolute) target = new File(directory,local)
            if (target.canonicalFile == report.canonicalFile) throw new GradleException('Report must not overwrite the scan target')
            if (listValues.additionalFormats) throw new GradleException('outputFile requires a single format')
        }
        File executable = resolveExecutable()
        logVersionHeader(executable)
        logger.lifecycle('Scanning with Grype: ' + source)
        boolean windows = System.getProperty('os.name','').toLowerCase(Locale.ROOT).contains('win')
        File temporary = (report != null || (windows && !listValues.additionalFormats)) ? Files.createTempFile('gradle-grype-report-', '.tmp').toFile() : null
        try {
            if (temporary != null) args.addAll(['--file',temporary.absolutePath])
            args.addAll(['--',source])
            boolean update = GrypeArguments.flag(config,'dbAutoUpdate')
            def result = execOperations.exec { spec ->
                spec.executable = executable.absolutePath
                spec.args(args)
                spec.workingDir = directory
                spec.environment('GRYPE_DB_AUTO_UPDATE',update.toString())
                if (config.dbCacheDir?.trim()) spec.environment('GRYPE_DB_CACHE_DIR',config.dbCacheDir)
                spec.ignoreExitValue = true
            }
            if (temporary != null && result.exitValue in [0,2]) {
                if (temporary.length() == 0) throw new GradleException('Grype did not create a report')
                if (report != null) {
                    Files.createDirectories(report.absoluteFile.parentFile.toPath())
                    Files.copy(temporary.toPath(),report.toPath(),StandardCopyOption.REPLACE_EXISTING)
                    logger.lifecycle('Report saved: ' + report.absolutePath)
                } else {
                    temporary.withReader('UTF-8') { reader ->
                        reader.eachLine { line -> logger.lifecycle(GrypeReportOutput.portableConsole(format,line)) }
                    }
                }
            }
            if (result.exitValue != 0) throw new GradleException(result.exitValue == 2 ? 'Grype vulnerability threshold exceeded (exit code 2)' : 'Grype scan failed (exit code ' + result.exitValue + ')')
        } finally {
            if (temporary != null) Files.deleteIfExists(temporary.toPath())
        }
    }
}
