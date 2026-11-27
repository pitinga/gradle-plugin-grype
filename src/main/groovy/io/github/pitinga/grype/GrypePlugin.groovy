package io.github.pitinga.grype
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
class GrypePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        GrypeExtension settings = project.extensions.create('grype', GrypeExtension)
        def install = project.tasks.register('grypeInstall', GrypeInstallTask) { task ->
            task.group = 'verification'
            task.description = 'Find or download a verified Grype executable'
        }
        def scan = project.tasks.register('grype', GrypeScanTask) { task ->
            task.group = 'verification'
            task.description = 'Scan built JAR or explicit target for vulnerabilities'
            task.outputs.upToDateWhen { false }
        }
        project.afterEvaluate {
            install.configure { task -> configureInstaller(task, project, settings) }
            scan.configure { task ->
                configureInstaller(task, project, settings)
                task.workingDirectory.set(project.layout.projectDirectory)
                Map<String,String> values = [:]
                values['sourceType'] = project.providers.gradleProperty('grypeSourceType').getOrElse(settings.sourceType.toString())
                values['target'] = project.providers.gradleProperty('grypeTarget').getOrElse(settings.target.toString())
                values['format'] = project.providers.gradleProperty('grypeFormat').getOrElse(settings.format.toString())
                values['outputFile'] = project.providers.gradleProperty('grypeOutput').getOrElse(settings.outputFile.toString())
                values['failOn'] = project.providers.gradleProperty('grypeFailOn').getOrElse(settings.failOn.toString())
                values['scope'] = project.providers.gradleProperty('grypeScope').getOrElse(settings.scope.toString())
                values['sortBy'] = project.providers.gradleProperty('grypeSortBy').getOrElse(settings.sortBy.toString())
                values['distro'] = project.providers.gradleProperty('grypeDistro').getOrElse(settings.distro.toString())
                values['platform'] = project.providers.gradleProperty('grypePlatform').getOrElse(settings.platform.toString())
                values['name'] = project.providers.gradleProperty('grypeName').getOrElse(settings.name.toString())
                values['template'] = project.providers.gradleProperty('grypeTemplate').getOrElse(settings.template.toString())
                values['ignoreStates'] = project.providers.gradleProperty('grypeIgnoreStates').getOrElse(settings.ignoreStates.toString())
                values['dbCacheDir'] = project.providers.gradleProperty('grypeDbCacheDir').getOrElse(settings.dbCacheDir.toString())
                values['verbose'] = project.providers.gradleProperty('grypeVerbose').getOrElse(settings.verbose.toString())
                values['onlyFixed'] = project.providers.gradleProperty('grypeOnlyFixed').getOrElse(settings.onlyFixed.toString())
                values['onlyNotFixed'] = project.providers.gradleProperty('grypeOnlyNotFixed').getOrElse(settings.onlyNotFixed.toString())
                values['byCve'] = project.providers.gradleProperty('grypeByCve').getOrElse(settings.byCve.toString())
                values['addCpesIfNone'] = project.providers.gradleProperty('grypeAddCpesIfNone').getOrElse(settings.addCpesIfNone.toString())
                values['showSuppressed'] = project.providers.gradleProperty('grypeShowSuppressed').getOrElse(settings.showSuppressed.toString())
                values['quiet'] = project.providers.gradleProperty('grypeQuiet').getOrElse(settings.quiet.toString())
                values['dbAutoUpdate'] = project.providers.gradleProperty('grypeDbAutoUpdate').getOrElse(settings.dbAutoUpdate.toString())
                task.options.set(values)
                task.excludePaths.set(new ArrayList<String>(settings.excludePaths ?: []))
                task.fromSources.set(new ArrayList<String>(settings.fromSources ?: []))
                task.configFiles.set(new ArrayList<String>(settings.configFiles ?: []))
                task.profiles.set(new ArrayList<String>(settings.profiles ?: []))
                task.vexFiles.set(new ArrayList<String>(settings.vexFiles ?: []))
                task.additionalArguments.set(new ArrayList<String>(settings.additionalArguments ?: []))
                task.additionalFormats.set(new ArrayList<String>(settings.additionalFormats ?: []))
                String overrideExcludes = project.providers.gradleProperty('grypeExcludePaths').orNull
                if (overrideExcludes != null) task.excludePaths.set(overrideExcludes.split(',').collect { it.trim() }.findAll { it })
                if (!values.target.trim()) {
                    def archive = project.tasks.findByName('bootJar') ?: project.tasks.findByName('jar')
                    if (archive instanceof AbstractArchiveTask) {
                        task.artifactFile.set(archive.archiveFile)
                        task.dependsOn(archive)
                    }
                }
            }
        }
    }
    private static void configureInstaller(GrypeTaskBase task, Project project, GrypeExtension settings) {
        task.grypeVersion.set(project.providers.gradleProperty('grypeVersion').getOrElse(settings.version))
        task.autoInstall.set(settings.autoInstall)
        task.preferInstalled.set(settings.preferInstalled)
        task.gradleHomeDirectory.set(project.gradle.gradleUserHomeDir)
        task.rootDirectory.fileValue(project.rootDir)
    }
}
