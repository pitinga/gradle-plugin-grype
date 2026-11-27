package io.github.pitinga.grype

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import groovy.json.JsonSlurper

import java.nio.file.Files
import java.nio.file.Path
import static org.junit.jupiter.api.Assertions.*

class GrypeReportOutputTest {
    @TempDir Path tempDir

    @Test
    void defaultsToNativeGrypeFormatsOnly() {
        assertEquals(['table', 'json', 'cyclonedx', 'cyclonedx-json', 'sarif', 'template'], GrypeReportOutput.FORMATS)
        assertEquals(['--output', 'table'], GrypeReportOutput.formatArguments('TABLE'))
        assertEquals(['--output', 'json'], GrypeReportOutput.formatArguments('json'))
        assertThrows(GradleException) { GrypeReportOutput.formatArguments('custom') }
        assertEquals(['--output', 'template'], GrypeReportOutput.formatArguments('template'))
    }

    @Test
    void adaptsNativeTableWithoutChangingCveIdsOrNativeFiles() {
        String unicode = '┌─────┬──────────┐\n│ Ação │ CVE-2026-63337 │\n├─────┼──────────┤\n└─────┴──────────┘\n'
        byte[] original = unicode.getBytes('UTF-8')
        File report = tempDir.resolve('native-table.txt').toFile()
        GrypeReportOutput.saveExactBytes(report, original)
        assertArrayEquals(original, Files.readAllBytes(report.toPath()))

        String shown = GrypeReportOutput.portableConsole('table', unicode)
        assertTrue(shown.contains('CVE-2026-63337'))
        assertTrue(shown.contains('Acao'))
        assertTrue(shown.contains('+-----+----------+'))
        assertTrue(shown.startsWith('+-----+----------+\n'), 'The top-left table corner must be +')
        assertTrue(shown.contains('\n+-----+----------+\n'), 'The junction row must retain +')
        assertTrue(shown.endsWith('+-----+----------+\n'), 'The bottom-left table corner must be +')
        assertFalse(shown.startsWith('-'), 'A corner must never be rendered as -')
        assertTrue(shown.contains('| Acao |'))
        assertTrue(shown.chars().allMatch { it < 128 }, 'Console must use only ASCII')
        assertFalse(shown.contains('┌'))
        assertTrue(Files.readString(report.toPath()).contains('┌'))
    }

    @Test
    void jsonConsoleEscapesUnicodeWithoutChangingJsonSemantics() {
        String original = '{"name":"ação","city":"東京","CVE":"CVE-2026-63337","emoji":"😃"}'
        String shown = GrypeReportOutput.portableConsole('json', original)
        assertTrue(shown.chars().allMatch { it < 128 }, 'JSON console must use only ASCII')
        assertEquals(new JsonSlurper().parseText(original), new JsonSlurper().parseText(shown))
        assertTrue(shown.contains('CVE-2026-63337'))
        assertTrue(shown.contains('\\u'))
    }

    @Test
    void classifiesTimestampedGrypeInfoWithoutConfusingItWithErrors() {
        assertEquals('INFO', GrypeReportOutput.diagnosticLevel(
            '2026-11-11T10:29:32-03:00\tINFO\t[vuln] Vulnerability scanning is enabled'))
        assertEquals('INFO', GrypeReportOutput.diagnosticLevel(
            '2026-11-11T10:29:32-03:00\tINFO\tNumber of language-specific files\tnum=1'))
        assertEquals('INFO', GrypeReportOutput.diagnosticLevel('INFO Scanner ready'))
        assertEquals('INFO', GrypeReportOutput.diagnosticLevel('DEBUG loading database'))
        assertEquals('WARN', GrypeReportOutput.diagnosticLevel(
            '2026-11-11T10:29:32-03:00\tWARN\tDatabase needs updating'))
        assertEquals('WARN', GrypeReportOutput.diagnosticLevel('WARNING Legacy setting'))
        assertEquals('ERROR', GrypeReportOutput.diagnosticLevel(
            '2026-11-11T10:29:32-03:00\tERROR\tUnable to load database'))
        assertEquals('ERROR', GrypeReportOutput.diagnosticLevel('FATAL failed to scan'))
        assertEquals('ERROR', GrypeReportOutput.diagnosticLevel('unclassified stderr must remain an error'))
    }

    @Test
    void fileDestinationsDoNotRewriteOrDamageNativeBytes() {
        File root = tempDir.toFile()
        assertNull(GrypeReportOutput.destination(root, ''))
        assertEquals(new File(root, 'build/reports/result.json'),
            GrypeReportOutput.destination(root, 'build/reports/result.json'))
        assertEquals(root, GrypeReportOutput.destination(root, root.absolutePath))
        assertThrows(GradleException) {
            GrypeReportOutput.destination(root, 'plugin=unexpected')
        }
        File file = tempDir.resolve('nested/grype.json').toFile()
        byte[] bytes = '{"note":"ação","box":"┌"}'.getBytes('UTF-8')
        GrypeReportOutput.saveExactBytes(file, bytes)
        assertArrayEquals(bytes, Files.readAllBytes(file.toPath()))
    }
}
