package io.github.pitinga.grype

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest
import groovy.json.JsonOutput
import static org.junit.jupiter.api.Assertions.*

/** Independent native flag/value fixtures; one named test per property. */
class GrypeParameterContractTest {
    static final List CASES = [
        ['addCpesIfNone', 'add-cpes-if-none', true],
        ['byCve', 'by-cve', true],
        ['config', 'config', ['first.yaml', 'config with spaces.yaml']],
        ['distro', 'distro', 'alpine:3.20'],
        ['exclude', 'exclude', ['./build/tmp/**', './.gradle/**']],
        ['failOn', 'fail-on', 'high'],
        ['file', 'file', 'build/reports/grype report.json'],
        ['from', 'from', ['docker', 'registry']],
        ['help', 'help', true],
        ['ignoreStates', 'ignore-states', 'unknown,wont-fix'],
        ['name', 'name', 'my project'],
        ['onlyFixed', 'only-fixed', true],
        ['onlyNotfixed', 'only-notfixed', false],
        ['output', 'output', ['table', 'json=build/report.json']],
        ['platform', 'platform', 'linux/amd64'],
        ['profile', 'profile', ['first', 'second']],
        ['quiet', 'quiet', false],
        ['scope', 'scope', 'all-layers'],
        ['showSuppressed', 'show-suppressed', true],
        ['sortBy', 'sort-by', 'severity'],
        ['template', 'template', 'template with spaces.tmpl'],
        ['verbose', 'verbose', 2],
        ['version', 'version', true],
        ['vex', 'vex', ['one.json', 'two.json']],
        ['load', 'load', true],
        ['packages', 'packages', true],
        ['vulns', 'vulns', false],
        ['broadCpeMatching', 'broad-cpe-matching', true],
        ['ecosystem', 'ecosystem', 'maven'],
        ['fixedState', 'fixed-state', ['fixed', 'unknown']],
        ['limit', 'limit', 20],
        ['modifiedAfter', 'modified-after', '2026-01-01'],
        ['pkg', 'pkg', ['log4j', 'openssl']],
        ['provider', 'provider', ['nvd', 'github']],
        ['publishedAfter', 'published-after', '2025-01-01'],
        ['vuln', 'vuln', ['CVE-2021-44228', 'CVE-2021-45046']],
        ['id', 'id', ['CVE-2021-44228', 'CVE-2021-45046']]
    ]
    @TestFactory
    Collection<DynamicTest> eachParameter() {
        CASES.collect { row ->
            String property = row[0]
            String flag = row[1]
            Object value = row[2]
            DynamicTest.dynamicTest(property + ' -> --' + flag) {
                def extension = new GrypeExtension()
                extension[property] = value
                assertEquals(value, extension[property])
                assertEquals(flag, GrypeArguments.nativeName(property))
                assertEquals([(flag):value], extension.nativeOptionsSnapshot())
                List values = value instanceof List ? value : [value]
                List<String> expected = values.collect { '--' + flag + '=' + it.toString() }
                assertEquals(expected, GrypeArguments.build(extension.nativeOptionsSnapshot()))
                String cliValue = value instanceof List ? JsonOutput.toJson(value) : value.toString()
                assertEquals(expected, GrypeArguments.build([(flag):GrypeArguments.propertyValue(flag,cliValue)]))
            }
        }
    }
    @Test void fixturesCoverEveryDocumentedProperty() {
        assertEquals(GrypeArguments.DOCUMENTED as Set, CASES.collect { it[1] } as Set)
        assertEquals(CASES.size(), (CASES.collect { it[0] } as Set).size())
    }
    @Test void unspecifiedOptionsPreserveNativeDefaults() {
        assertEquals([], GrypeArguments.build(new GrypeExtension().nativeOptionsSnapshot()))
    }
    @Test void rejectsInvalidNamesAndVerbosity() {
        assertThrows(RuntimeException) { GrypeArguments.nativeName('fail-on') }
        assertThrows(RuntimeException) { GrypeArguments.build([verbose:-1]) }
        assertThrows(RuntimeException) { GrypeArguments.build([verbose:'invalid']) }
    }
    @Test void explicitBooleanFalseAndRepeatedValuesAreNotLost() {
        assertEquals(['--only-fixed=false','--exclude=a b','--exclude=c'], GrypeArguments.build(['only-fixed':false,exclude:['a b','c']]))
    }
}
