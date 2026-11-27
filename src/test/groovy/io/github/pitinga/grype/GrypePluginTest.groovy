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
        assertFalse(runBuild("grype { target = 'dir:.' }",'grype','--dry-run').output.contains(':jar SKIPPED'))
    }
    @Test void commandsDoNotBuildJar() {
        assertFalse(runBuild("grype { command = ['db','status']; output = 'json' }",'grype','--dry-run').output.contains(':jar SKIPPED'))
    }
    @Test void nativeVersionFlagDoesNotBuildJar() {
        assertFalse(runBuild("grype { version = true }",'grype','--dry-run').output.contains(':jar SKIPPED'))
    }
    @Test void registersInstall() {
        assertTrue(runBuild('','tasks','--all').output.contains('grypeInstall'))
    }
    @Test void versionMetadata() {
        assertNotEquals('unknown',GrypeTaskBase.pluginArtifactVersion())
        assertEquals('0.122.0',GrypeTaskBase.parseGrypeCliVersion('Application: grype\nVersion: 0.122.0\n'))
    }
    @Test void literalNativeFlags() {
        assertEquals(['--output=json','--output=table=report.txt','--only-fixed=true','--only-notfixed=false','--fail-on=high','--exclude=./build/**','--verbose=2'],
            GrypeArguments.build([output:['json','table=report.txt'],'only-fixed':true,'only-notfixed':false,'fail-on':'high',exclude:['./build/**'],verbose:2]))
    }
    @Test void allDocumentedNamesAreAcceptedLiterally() {
        GrypeArguments.DOCUMENTED.each { key ->
            assertTrue(GrypeArguments.build([(key):key == 'verbose' ? 1 : 'value']).first().startsWith('--'+key+'='))
        }
        assertEquals(['--future-grype-option=hello'],GrypeArguments.build(['future-grype-option':'hello']))
        assertThrows(RuntimeException) { GrypeArguments.build([onlyFixed:true]) }
    }
    @Test void nativePropertiesOverrideDsl() {
        def result = runBuild("grype { onlyFixed = false; output = 'table'; target = 'dir:.' }; afterEvaluate { println 'ARGS=' + tasks.named('grype').get().nativeOptions.get() }",'grype','--dry-run','-Pgrype.onlyFixed=true','-Pgrype.output=json')
        assertTrue(result.output.contains('--only-fixed=true'))
        assertTrue(result.output.contains('--output=json'))
    }
    @Test void allDocumentedFlagsHaveDirectCamelCaseProperties() {
        def extension = new GrypeExtension()
        GrypeArguments.DOCUMENTED.each { flag ->
            String property = flag.replaceAll(/-([a-z])/) { match, letter -> letter.toUpperCase(Locale.ROOT) }
            def value = flag == 'verbose' ? 2 : 'sample'
            extension[property] = value
            assertEquals(value, extension[property], property)
            assertEquals(flag, GrypeArguments.nativeName(property))
            assertEquals(value, extension.nativeOptionsSnapshot()[flag])
        }
        extension.option('futureGrypeOption','hello')
        assertEquals('hello',extension.nativeOptionsSnapshot()['future-grype-option'])
    }
    @Test void directDslMapsToNativeFlags() {
        def result = runBuild("grype { target = 'dir:.'; output = 'json'; file = 'build/report.json'; onlyFixed = true; failOn = 'high'; sortBy = 'severity'; byCve = true; exclude = ['./build/tmp/**'] }; afterEvaluate { println 'ARGS=' + tasks.named('grype').get().nativeOptions.get() }",'grype','--dry-run')
        ['--output=json','--file=build/report.json','--only-fixed=true','--fail-on=high','--sort-by=severity','--by-cve=true','--exclude=./build/tmp/**'].each { assertTrue(result.output.contains(it),it) }
    }
    @Test void preservesPrefixesAndImageReferences() {
        ['dir:.','file:C:\\folder\\app.jar','registry:alpine:3.20','sbom:report.json','pkg:maven/log4j/log4j@1.2.17'].each {
            assertEquals(it,GrypeArguments.target(it,null))
        }
    }
}
