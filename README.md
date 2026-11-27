# Gradle Grype Plugin

Initial 0.1.0-SNAPSHOT, not yet published. Based on gradle-plugin-trivy for architecture, installation and CI; the Grype CLI is the source of truth for flags. Native kebab-case names are converted mechanically to camelCase for Gradle, without semantic aliases.

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
    output = 'json'
    file = 'build/reports/grype.json'
    onlyFixed = true
    failOn = 'high'
    sortBy = 'severity'
    byCve = true
    exclude = ['./build/tmp/**']
}
```

Configure options directly with Groovy assignments, not an options map. Strings are forwarded verbatim, lists repeat the native flag, booleans use --flag=true/false, and verbose is a count. Unspecified properties are omitted so Grype retains its native defaults. The scanner validates options and values.

```powershell
.\gradlew grypeInstall
.\gradlew grype "-Pgrype.output=json" "-Pgrype.file=build/reports/grype.json"
.\gradlew grype "-Pgrype.target=dir:." "-Pgrype.onlyFixed=true"
.\gradlew grype "-Pgrype.target=registry:alpine:3.20" "-Pgrype.failOn=high"
.\gradlew grype "-Pgrype.command=db status" "-Pgrype.output=json"
.\gradlew grype "-Pgrype.command=db search" "-Pgrype.vuln=CVE-2021-44228"
```

Use `output = ['table', 'json=build/reports/grype.json']` for multiple native outputs. Use `command = ['db','diff']`, `arguments = ['old.db','new.db']` for subcommands with positional arguments. Without target or command, the plugin builds bootJar or jar and scans it as file:. Native --help/--version skip packaging. Native prefixes, output formats and flags are not rewritten.

## Native CLI properties

| Gradle property | Grype long flag |
|---|---|
| `addCpesIfNone` | `--add-cpes-if-none` |
| `byCve` | `--by-cve` |
| `config` | `--config` |
| `distro` | `--distro` |
| `exclude` | `--exclude` |
| `failOn` | `--fail-on` |
| `file` | `--file` |
| `from` | `--from` |
| `help` | `--help` |
| `ignoreStates` | `--ignore-states` |
| `name` | `--name` |
| `onlyFixed` | `--only-fixed` |
| `onlyNotfixed` | `--only-notfixed` |
| `output` | `--output` |
| `platform` | `--platform` |
| `profile` | `--profile` |
| `quiet` | `--quiet` |
| `scope` | `--scope` |
| `showSuppressed` | `--show-suppressed` |
| `sortBy` | `--sort-by` |
| `template` | `--template` |
| `verbose` | `--verbose` |
| `version` | `--version` |
| `vex` | `--vex` |
| `load` | `--load` |
| `packages` | `--packages` |
| `vulns` | `--vulns` |
| `broadCpeMatching` | `--broad-cpe-matching` |
| `ecosystem` | `--ecosystem` |
| `fixedState` | `--fixed-state` |
| `limit` | `--limit` |
| `modifiedAfter` | `--modified-after` |
| `pkg` | `--pkg` |
| `provider` | `--provider` |
| `publishedAfter` | `--published-after` |
| `vuln` | `--vuln` |
| `id` | `--id` |
| `noDescriptions` | `--no-descriptions` |

Reference: https://oss.anchore.com/docs/reference/grype/cli/ (documented 0.119.0; CI exercises 0.122.0). Future flags can be set through `option('futureGrypeOption', value)` or `-Pgrype.futureGrypeOption=value` without a plugin update. For repeated CLI properties pass a JSON array as the value. Native config/environment may be set through config, profile and environment (e.g. GRYPE_DB_AUTO_UPDATE); these are not aliases for CLI flags. Installation options are separate because --version is a native boolean flag, not a binary version selector.

Build locally: gradle clean test validatePlugins publishToMavenLocal. Installation verifies SHA-256 and caches in Gradle user home/grype, falling back to project .gradle/grype on permission errors. Native Grype exit codes are preserved as Gradle failures (including 2). Standalone Syft generation is not included. Publication requires stable version and validated CI.
