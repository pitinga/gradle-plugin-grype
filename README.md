# Gradle Grype Plugin

Initial 0.1.0-SNAPSHOT, not yet published. Based on gradle-plugin-trivy; native CLI names are the API, without the leading --. No camelCase aliases, no Trivy vocabulary.

```groovy
plugins {
    id 'java'
    id 'io.github.pitinga.grype' version '0.1.0-SNAPSHOT'
}
grype {
    installation {
        version = 'latest'
        autoInstall = true
        preferInstalled = true
    }
    options = [
        output: ['table', 'json=build/reports/grype.json'],
        'only-fixed': true,
        'fail-on': 'high',
        exclude: ['./build/tmp/**']
    ]
}
```

Use `option 'only-fixed', true` to set individual literal keys. Single-word option properties such as `output = 'json'` are also available. The map is the canonical API for all options, including future Grype flags. String values are forwarded verbatim, lists repeat the flag, booleans use --flag=true/false, verbose is a count. Grype validates options and values; the plugin does not invent extra severity levels or formats.

```powershell
.\gradlew grypeInstall
.\gradlew grype "-Pgrype.output=json" "-Pgrype.file=build/reports/grype.json"
.\gradlew grype "-Pgrype.target=dir:." "-Pgrype.only-fixed=true"
.\gradlew grype "-Pgrype.target=registry:alpine:3.20" "-Pgrype.fail-on=high"
.\gradlew grype "-Pgrype.command=db status" "-Pgrype.output=json"
.\gradlew grype "-Pgrype.command=db search" "-Pgrype.vuln=CVE-2021-44228"
```

Use command = ['db','diff'], arguments = ['old.db','new.db'] for subcommands with positional arguments. Without target or command, the plugin builds bootJar or jar and scans it as file:. Native --help/--version skip packaging. Native prefixes, output formats and flags are not rewritten.

All documented Grype long names:
`add-cpes-if-none`, `by-cve`, `config`, `distro`, `exclude`, `fail-on`, `file`, `from`, `help`, `ignore-states`, `name`, `only-fixed`, `only-notfixed`, `output`, `platform`, `profile`, `quiet`, `scope`, `show-suppressed`, `sort-by`, `template`, `verbose`, `version`, `vex`, `load`, `packages`, `vulns`, `broad-cpe-matching`, `ecosystem`, `fixed-state`, `limit`, `modified-after`, `pkg`, `provider`, `published-after`, `vuln`, `id`, `no-descriptions`.

Reference: https://oss.anchore.com/docs/reference/grype/cli/ (documented 0.119.0; CI exercises 0.122.0). Future flags can be set through options or -Pgrype.<literal-name> without a plugin update. For repeated CLI properties pass a JSON array as the value. Native config/environment may be set through config, profile and environment (e.g. GRYPE_DB_AUTO_UPDATE); these are not aliases for CLI flags. Installation options are separate because --version is a native boolean flag, not a binary version selector.

Build locally: gradle clean test validatePlugins publishToMavenLocal. Installation verifies SHA-256 and caches in Gradle user home/grype, falling back to project .gradle/grype on permission errors. Native Grype exit codes are preserved as Gradle failures (including 2). Standalone Syft generation is not included. Publication requires stable version and validated CI.
