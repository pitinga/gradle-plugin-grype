package io.github.pitinga.grype

import org.gradle.api.GradleException
import groovy.json.JsonSlurper

/** Preserve native Grype names, values, repetitions and command order. */
class GrypeArguments {
    static final List<String> DOCUMENTED = ['add-cpes-if-none','by-cve','config','distro','exclude','fail-on','file','from','help','ignore-states','name','only-fixed','only-notfixed','output','platform','profile','quiet','scope','show-suppressed','sort-by','template','verbose','version','vex','load','packages','vulns','broad-cpe-matching','ecosystem','fixed-state','limit','modified-after','pkg','provider','published-after','vuln','id'].asImmutable()
    static final Set<String> BOOLEAN_FLAGS = ['add-cpes-if-none','by-cve','help','only-fixed','only-notfixed','quiet','show-suppressed','version','load','packages','vulns','broad-cpe-matching'] as Set
    static String nativeName(String property) {
        if (!(property ==~ /[a-z][a-zA-Z0-9]*/)) {
            throw new GradleException('Use camelCase Grype property names: ' + property)
        }
        property.replaceAll(/[A-Z]/) { letter -> '-' + letter.toLowerCase(Locale.ROOT) }
    }
    static void checkName(String name) {
        if (!(name ==~ /[a-z][a-z0-9-]*/)) throw new GradleException('Invalid native Grype flag name: ' + name)
        // No allowlist: a new Grype option is usable without a plugin release.
    }
    static Object propertyValue(String name, String value) {
        if (value.startsWith('[')) {
            def parsed = new JsonSlurper().parseText(value)
            if (!(parsed instanceof List)) throw new GradleException('Expected JSON array for ' + name)
            return parsed
        }
        if (BOOLEAN_FLAGS.contains(name) && value in ['true','false']) return value == 'true'
        return value
    }
    static List<String> build(Map<String,Object> options) {
        List<String> args = []
        options.each { name,value ->
            checkName(name)
            if (value == null) return
            List values = value instanceof Collection ? new ArrayList(value) : [value]
            values.each { item ->
                if (item == null) throw new GradleException('Null value for --' + name)
                if (name == 'verbose') {
                    String n = item.toString()
                    if (!(n ==~ /\d+/)) throw new GradleException('--verbose requires a nonnegative count')
                    int count = Integer.parseInt(n)
                    // pflag count accepts an explicit number; do not silently cap at 2.
                    args.add('--verbose=' + count)
                } else if (item instanceof Boolean) {
                    args.add('--' + name + '=' + item.toString())
                } else {
                    args.add('--' + name + '=' + item.toString())
                }
            }
        }
        args
    }
    static String target(String requested, File artifact) {
        if (requested?.trim()) return requested
        if (artifact == null) throw new GradleException('No bootJar/jar task found; set grype.target')
        'file:' + artifact.absolutePath
    }
    static boolean requiresArtifact(List<String> command, String target, Map options) {
        !command && !target?.trim() && options.help?.toString() != 'true' && options.version?.toString() != 'true'
    }
}
