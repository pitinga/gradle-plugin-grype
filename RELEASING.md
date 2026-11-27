# Release process

1. Confirm all matrix checks, isolated-projects and real CLI smoke tests pass.
2. Change the plugin version from 0.1.0-SNAPSHOT to 0.1.0 (or next 0.X.Y) through a reviewed PR; update CHANGELOG and README.
3. Dispatch Release Gradle Grype Plugin on main. It validates the build, creates an annotated tag and GitHub release.
4. Configure repository/organization secrets GRADLE_PUBLISH_KEY and GRADLE_PUBLISH_SECRET for the Gradle Plugin Portal if required.
5. Dispatch Publish Gradle Plugin Portal for the release tag with operation validate; then publish only after successful validation.

No publication credentials are copied from another repository. No snapshot is published to the Portal.
