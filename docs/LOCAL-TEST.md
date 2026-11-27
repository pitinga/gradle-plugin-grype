# Local test configuration

Use a locally published 0.1.0-SNAPSHOT (gradle publishToMavenLocal in the plugin checkout; add mavenLocal() in consumer pluginManagement repositories), or includeBuild pointing at the checkout.

```groovy
grype {
    installation {
        version = 'latest'
        autoInstall = true
        preferInstalled = true
    }

    // Default: builds bootJar or jar
    // target = 'dir:.'
    // target = 'registry:alpine:3.20'
    // target = 'sbom:build/sbom.json'

    output = 'table'
    // file = 'build/reports/grype.txt'
    // output = ['table', 'json=build/reports/grype.json']

    addCpesIfNone = false
    byCve = true
    onlyFixed = true
    onlyNotfixed = false
    sortBy = 'severity'
    scope = 'squashed'
    // failOn = 'high'
    // ignoreStates = 'unknown,wont-fix'
    // showSuppressed = true // table only

    // name = 'my-project'
    // distro = 'alpine:3.20'
    // platform = 'linux/amd64' // image targets only
    // from = ['registry'] // use with an unprefixed image target
    // exclude = ['./build/tmp/**'] // directory scans
    // config = ['grype.yaml'] // existing file
    // profile = ['ci'] // existing configured profile
    // vex = ['vex.json'] // existing VEX file
    // template = 'report.tmpl' // select template output too

    quiet = false
    verbose = 0
    // help = true
    // version = true
}
```

For database operations use a separate configuration, not the scan options above:

```groovy
grype {
    command = ['db', 'search']
    output = 'json'
    pkg = ['log4j']
    vuln = ['CVE-2021-44228']
    ecosystem = 'maven'
    limit = 20
    // distro = ['alpine:3.20']
    // fixedState = ['fixed']
    // provider = ['nvd']
    // publishedAfter = '2025-01-01'
    // modifiedAfter = '2026-01-01'
    // broadCpeMatching = true
}
```

Other command-specific flags: config -> load; db diff -> packages, vulns; explain -> id. Use command and arguments for positional values. Do not enable all flags in one invocation: the CLI has command-specific and mutually incompatible options.
