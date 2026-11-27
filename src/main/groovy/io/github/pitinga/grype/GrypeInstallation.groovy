package io.github.pitinga.grype

/** Plugin-only binary provisioning, independent of Grype's --version flag. */
class GrypeInstallation {
    String version = 'latest'
    boolean autoInstall = true
    boolean preferInstalled = true
}
