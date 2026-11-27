package io.github.pitinga.grype

/** Native Grype flags exposed as direct camelCase Gradle DSL properties. */
class GrypeExtension {
    final GrypeInstallation installation = new GrypeInstallation()
    String target = ''
    List<String> command = []
    List<String> arguments = []
    Map<String,String> environment = [:]
    private final Map<String,Object> nativeValues = new LinkedHashMap<>()

    void installation(Closure configure) {
        configure.delegate = installation
        configure.resolveStrategy = Closure.DELEGATE_FIRST
        configure.call()
    }

    /** Escape hatch for future Grype options, using the same camelCase convention. */
    void option(String name, Object value) {
        nativeValues[GrypeArguments.nativeName(name)] = value
    }

    Map<String,Object> nativeOptionsSnapshot() {
        return new LinkedHashMap<>(nativeValues)
    }

    void setAddCpesIfNone(Object value) { option('addCpesIfNone', value) }
    Object getAddCpesIfNone() { nativeValues['add-cpes-if-none'] }

    void setByCve(Object value) { option('byCve', value) }
    Object getByCve() { nativeValues['by-cve'] }

    void setConfig(Object value) { option('config', value) }
    Object getConfig() { nativeValues['config'] }

    void setDistro(Object value) { option('distro', value) }
    Object getDistro() { nativeValues['distro'] }

    void setExclude(Object value) { option('exclude', value) }
    Object getExclude() { nativeValues['exclude'] }

    void setFailOn(Object value) { option('failOn', value) }
    Object getFailOn() { nativeValues['fail-on'] }

    void setFile(Object value) { option('file', value) }
    Object getFile() { nativeValues['file'] }

    void setFrom(Object value) { option('from', value) }
    Object getFrom() { nativeValues['from'] }

    void setHelp(Object value) { option('help', value) }
    Object getHelp() { nativeValues['help'] }

    void setIgnoreStates(Object value) { option('ignoreStates', value) }
    Object getIgnoreStates() { nativeValues['ignore-states'] }

    void setName(Object value) { option('name', value) }
    Object getName() { nativeValues['name'] }

    void setOnlyFixed(Object value) { option('onlyFixed', value) }
    Object getOnlyFixed() { nativeValues['only-fixed'] }

    void setOnlyNotfixed(Object value) { option('onlyNotfixed', value) }
    Object getOnlyNotfixed() { nativeValues['only-notfixed'] }

    void setOutput(Object value) { option('output', value) }
    Object getOutput() { nativeValues['output'] }

    void setPlatform(Object value) { option('platform', value) }
    Object getPlatform() { nativeValues['platform'] }

    void setProfile(Object value) { option('profile', value) }
    Object getProfile() { nativeValues['profile'] }

    void setQuiet(Object value) { option('quiet', value) }
    Object getQuiet() { nativeValues['quiet'] }

    void setScope(Object value) { option('scope', value) }
    Object getScope() { nativeValues['scope'] }

    void setShowSuppressed(Object value) { option('showSuppressed', value) }
    Object getShowSuppressed() { nativeValues['show-suppressed'] }

    void setSortBy(Object value) { option('sortBy', value) }
    Object getSortBy() { nativeValues['sort-by'] }

    void setTemplate(Object value) { option('template', value) }
    Object getTemplate() { nativeValues['template'] }

    void setVerbose(Object value) { option('verbose', value) }
    Object getVerbose() { nativeValues['verbose'] }

    void setVersion(Object value) { option('version', value) }
    Object getVersion() { nativeValues['version'] }

    void setVex(Object value) { option('vex', value) }
    Object getVex() { nativeValues['vex'] }

    void setLoad(Object value) { option('load', value) }
    Object getLoad() { nativeValues['load'] }

    void setPackages(Object value) { option('packages', value) }
    Object getPackages() { nativeValues['packages'] }

    void setVulns(Object value) { option('vulns', value) }
    Object getVulns() { nativeValues['vulns'] }

    void setBroadCpeMatching(Object value) { option('broadCpeMatching', value) }
    Object getBroadCpeMatching() { nativeValues['broad-cpe-matching'] }

    void setEcosystem(Object value) { option('ecosystem', value) }
    Object getEcosystem() { nativeValues['ecosystem'] }

    void setFixedState(Object value) { option('fixedState', value) }
    Object getFixedState() { nativeValues['fixed-state'] }

    void setLimit(Object value) { option('limit', value) }
    Object getLimit() { nativeValues['limit'] }

    void setModifiedAfter(Object value) { option('modifiedAfter', value) }
    Object getModifiedAfter() { nativeValues['modified-after'] }

    void setPkg(Object value) { option('pkg', value) }
    Object getPkg() { nativeValues['pkg'] }

    void setProvider(Object value) { option('provider', value) }
    Object getProvider() { nativeValues['provider'] }

    void setPublishedAfter(Object value) { option('publishedAfter', value) }
    Object getPublishedAfter() { nativeValues['published-after'] }

    void setVuln(Object value) { option('vuln', value) }
    Object getVuln() { nativeValues['vuln'] }

    void setId(Object value) { option('id', value) }
    Object getId() { nativeValues['id'] }

    void setNoDescriptions(Object value) { option('noDescriptions', value) }
    Object getNoDescriptions() { nativeValues['no-descriptions'] }
}
