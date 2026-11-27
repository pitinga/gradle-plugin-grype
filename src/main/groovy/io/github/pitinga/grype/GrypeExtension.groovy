package io.github.pitinga.grype
class GrypeExtension {
    String version = 'latest'
    boolean autoInstall = true
    boolean preferInstalled = true
    String sourceType = 'auto'
    String target = ''
    String format = 'table'
    String outputFile = ''
    String failOn = 'none'
    String scope = 'squashed'
    String sortBy = 'risk'
    String distro = ''
    String platform = ''
    String name = ''
    String template = ''
    String ignoreStates = ''
    String dbCacheDir = ''
    int verbose = 0
    boolean onlyFixed = false
    boolean onlyNotFixed = false
    boolean byCve = false
    boolean addCpesIfNone = false
    boolean showSuppressed = false
    boolean quiet = false
    boolean dbAutoUpdate = true
    List<String> excludePaths = []
    List<String> fromSources = []
    List<String> configFiles = []
    List<String> profiles = []
    List<String> vexFiles = []
    List<String> additionalArguments = []
    List<String> additionalFormats = []
}
