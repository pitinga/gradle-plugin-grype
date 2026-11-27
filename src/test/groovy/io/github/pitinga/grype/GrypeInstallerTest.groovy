package io.github.pitinga.grype

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*

class GrypeInstallerTest {
    @Test
    void usesLatestStableGrypeByDefaultWhilePreferringPath() {
        def settings = new GrypeExtension()
        assertEquals('latest', settings.version)
        assertTrue(settings.preferInstalled)
        assertTrue(settings.autoInstall)
    }

    @Test
    void correctOfficialAssetNames() {
        assertEquals('grype_0.122.0_windows_amd64.zip', GrypeInstaller.archiveName('0.122.0', 'windows-amd64'))
        assertEquals('grype_0.122.0_linux_amd64.tar.gz', GrypeInstaller.archiveName('0.122.0', 'linux-amd64'))
        assertEquals('grype_0.122.0_linux_arm64.tar.gz', GrypeInstaller.archiveName('0.122.0', 'linux-arm64'))
        assertEquals('grype_0.122.0_darwin_amd64.tar.gz', GrypeInstaller.archiveName('0.122.0', 'macos-amd64'))
        assertEquals('grype_0.122.0_darwin_arm64.tar.gz', GrypeInstaller.archiveName('0.122.0', 'macos-arm64'))
    }

    @Test
    void parsesExactOfficialChecksumEntry() {
        String hash = 'a' * 64
        String content = ('b' * 64) + '  grype_other.zip\n' +
            hash + '  grype_0.122.0_windows_amd64.zip\n'
        assertEquals(hash, GrypeInstaller.checksumFor(content, 'grype_0.122.0_windows_amd64.zip'))
        assertThrows(RuntimeException) {
            GrypeInstaller.checksumFor(content, 'grype_unknown.zip')
        }
    }

    @Test
    void rejectsUnknownPlatforms() {
        assertThrows(RuntimeException) {
            GrypeInstaller.archiveName('0.122.0', 'windows-arm64')
        }
    }
}
