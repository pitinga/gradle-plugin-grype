# Gradle Grype Plugin

Based on [gradle-plugin-trivy](https://github.com/pitinga/gradle-plugin-trivy), using the official [Grype CLI](https://oss.anchore.com/docs/reference/grype/cli/). Initial development version: 0.1.0-SNAPSHOT (not published).

Build with Gradle 8.14.3+ / Java 17+: `gradle clean test validatePlugins publishToMavenLocal`. Use an included build or configure mavenLocal in pluginManagement.

```groovy
plugins {
    id 'java'
    id 'io.github.pitinga.grype' version '0.1.0-SNAPSHOT'
}
grype {
    version = 'latest'
    format = 'table'
    onlyFixed = true
    failOn = 'high'
}
```

- `gradle grypeInstall`: find/download CLI, verify SHA-256, cache under Gradle user home/grype, with project .gradle/grype fallback on permission errors.
- `gradle grype`: build bootJar or jar and scan as file:; explicit targets do not trigger packaging.
- `gradle grype -PgrypeTarget=. -PgrypeSourceType=dir -PgrypeFormat=json -PgrypeOutput=build/reports/grype.json`
- `gradle grype -PgrypeTarget=alpine:3.20 -PgrypeSourceType=registry`
- `gradle grype -PgrypeTarget=build/sbom.json -PgrypeSourceType=sbom`

Installation prefers PATH. Set preferInstalled=false and a stable version for reproducible downloads. autoInstall=false disables binary download; latest may still query metadata. Grype includes Syft cataloging internally; standalone Syft is not included. Existing SBOMs are supported.

## DSL

Scalar scan options support -Pgrype<Property> (outputFile uses -PgrypeOutput). List settings use the DSL; grypeExcludePaths supports comma-separated values.

| Option | Default |
|---|---|
| sourceType | auto |
| target | (empty) |
| format | table |
| outputFile | (empty) |
| failOn | none |
| scope | squashed |
| sortBy | risk |
| distro | (empty) |
| platform | (empty) |
| name | (empty) |
| template | (empty) |
| ignoreStates | (empty) |
| dbCacheDir | (empty) |
| verbose | 0 |
| onlyFixed | false |
| onlyNotFixed | false |
| byCve | false |
| addCpesIfNone | false |
| showSuppressed | false |
| quiet | false |
| dbAutoUpdate | true |
| excludePaths | [] |
| fromSources | [] |
| configFiles | [] |
| profiles | [] |
| vexFiles | [] |
| additionalArguments | [] |
| additionalFormats | [] |

Native formats: table, json, cyclonedx, cyclonedx-json, sarif, template. outputFile requires one format. Additional arguments are individual native CLI tokens and can override options. Explicit sourceType is recommended for local paths; auto can interpret an absent file as an image. failOn=none omits the threshold flag; native configuration may still set a threshold. Exit code 2 fails the task and preserves the generated report; other nonzero codes fail the task. Native report bytes are preserved. Windows table and JSON-family console output is adapted for legacy codepages.
