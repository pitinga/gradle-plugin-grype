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
            task.description = 'Run native Grype scan or subcommand'
            task.outputs.upToDateWhen { false }
        }
        project.afterEvaluate {
            install.configure { task -> configureInstaller(task,project,settings.installation) }
            scan.configure { task ->
                configureInstaller(task,project,settings.installation)
                task.workingDirectory.set(project.layout.projectDirectory)
                String target = project.providers.gradleProperty('grype.target').getOrElse(settings.target ?: '')
                String commandOverride = project.providers.gradleProperty('grype.command').orNull
                List<String> command = commandOverride == null ? new ArrayList<String>(settings.command) : commandOverride.tokenize(' ')
                String argsOverride = project.providers.gradleProperty('grype.arguments').orNull
                List<String> arguments = argsOverride == null ? new ArrayList<String>(settings.arguments) : (new groovy.json.JsonSlurper().parseText(argsOverride) as List).collect { it.toString() }
                Map<String,Object> opts = settings.nativeOptionsSnapshot()
                // Project properties use the same camelCase names as the DSL.
                project.providers.gradlePropertiesPrefixedBy('grype.').get().each { key,value ->
                    String name = key.substring('grype.'.length())
                    if (!(name in ['target','command','arguments','installation.version','installation.autoInstall','installation.preferInstalled'])) {
                        String nativeName = GrypeArguments.nativeName(name)
                        opts[nativeName] = GrypeArguments.propertyValue(nativeName,value)
                    }
                }
                task.nativeOptions.set(GrypeArguments.build(opts))
                task.command.set(command)
                task.arguments.set(arguments)
                task.scanTarget.set(target)
                task.nativeEnvironment.set(new LinkedHashMap<String,String>(settings.environment))
                boolean needsArtifact = GrypeArguments.requiresArtifact(command,target,opts)
                task.buildArtifact.set(needsArtifact)
                if (needsArtifact) {
                    def archive = project.tasks.findByName('bootJar') ?: project.tasks.findByName('jar')
                    if (archive instanceof AbstractArchiveTask) {
                        task.artifactFile.set(archive.archiveFile)
                        task.dependsOn(archive)
                    }
                }
            }
        }
    }

    private static void configureInstaller(GrypeTaskBase task, Project project, GrypeInstallation settings) {
        task.grypeVersion.set(project.providers.gradleProperty('grype.installation.version').getOrElse(settings.version))
        task.autoInstall.set(project.providers.gradleProperty('grype.installation.autoInstall').map { it.toBoolean() }.getOrElse(settings.autoInstall))
        task.preferInstalled.set(project.providers.gradleProperty('grype.installation.preferInstalled').map { it.toBoolean() }.getOrElse(settings.preferInstalled))
        task.gradleHomeDirectory.set(project.gradle.gradleUserHomeDir)
        task.rootDirectory.fileValue(project.rootDir)
    }
}
