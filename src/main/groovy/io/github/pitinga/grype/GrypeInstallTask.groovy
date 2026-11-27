package io.github.pitinga.grype

import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = 'Installs an external executable into a mutable tool cache')
abstract class GrypeInstallTask extends GrypeTaskBase {
    @TaskAction
    void install() {
        File executable = resolveExecutable()
        logVersionHeader(executable)
        logger.lifecycle('Grype executable: ' + executable.absolutePath)
    }
}
