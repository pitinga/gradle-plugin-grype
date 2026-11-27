package io.github.pitinga.grype
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files
import static org.junit.jupiter.api.Assertions.*
class GrypePluginTest {
    @TempDir Path projectDir
    def runBuild(String extra,String... args) {
        Files.writeString(projectDir.resolve('settings.gradle'),"rootProject.name = 'test-app'\n")
        Files.writeString(projectDir.resolve('build.gradle'),"plugins { id 'java'; id 'io.github.pitinga.grype' }\n" + extra)
        GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath()
            .withArguments(*(args.toList()+['--offline','--stacktrace'])).build()
    }
    @Test void defaultBuildsJar() {
        def result = runBuild('','grype','--dry-run')
        assertTrue(result.output.contains(':jar SKIPPED'))
        assertTrue(result.output.contains(':grype SKIPPED'))
    }
    @Test void explicitTargetSkipsJar() {
        def result = runBuild("grype { target = '.'; sourceType = 'dir' }",'grype','--dry-run')
        assertFalse(result.output.contains(':jar SKIPPED'))
    }
    @Test void registersInstall() {
        assertTrue(runBuild('','tasks','--all').output.contains('grypeInstall'))
    }
    @Test void versionMetadata() {
        assertNotEquals('unknown',GrypeTaskBase.pluginArtifactVersion())
        assertEquals('0.122.0',GrypeTaskBase.parseGrypeCliVersion('Application: grype\nVersion: 0.122.0\n'))
    }
    Map defaults() {
        def e = new GrypeExtension()
        ['format','failOn','scope','sortBy','distro','platform','name','template','ignoreStates','verbose','onlyFixed','onlyNotFixed','byCve','addCpesIfNone','showSuppressed','quiet','dbAutoUpdate','sourceType','target'].collectEntries { [(it):e[it].toString()] }
    }
    Map lists() {
        ['excludePaths','fromSources','configFiles','profiles','vexFiles','additionalArguments','additionalFormats'].collectEntries { [(it):[]] }
    }
    @Test void nativeFlags() {
        def args = GrypeArguments.build(defaults()+[onlyFixed:'true',failOn:'high',format:'json',byCve:'true'],lists()+[excludePaths:['./build/**']])
        assertEquals(['--output','json','--fail-on','high','--exclude','./build/**','--only-fixed','--by-cve'],args)
    }
    @Test void rejectsConflicts() {
        assertThrows(RuntimeException) { GrypeArguments.build(defaults()+[onlyFixed:'true',onlyNotFixed:'true'],lists()) }
    }
    @Test void mapsSources() {
        File dir = projectDir.toFile()
        assertEquals('dir:'+dir.absolutePath,GrypeArguments.source(defaults()+[target:dir.absolutePath],dir,null,[]))
        assertEquals('registry:alpine:3.20',GrypeArguments.source(defaults()+[sourceType:'registry',target:'alpine:3.20'],dir,null,[]))
    }
}
