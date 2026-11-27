package io.github.pitinga.grype

/** Grype long option names are used verbatim, without the leading --. */
class GrypeExtension {
    final GrypeInstallation installation = new GrypeInstallation()
    String target = ''
    List<String> command = []
    List<String> arguments = []
    Map<String,Object> options = [:]
    Map<String,String> environment = [:]

    void installation(Closure configure) {
        configure.delegate = installation
        configure.resolveStrategy = Closure.DELEGATE_FIRST
        configure.call()
    }

    void option(String name, Object value) {
        GrypeArguments.checkName(name)
        options[name] = value
    }

    void setConfig(Object value) { option('config',value) }
    Object getConfig() { options['config'] }
    void setDistro(Object value) { option('distro',value) }
    Object getDistro() { options['distro'] }
    void setExclude(Object value) { option('exclude',value) }
    Object getExclude() { options['exclude'] }
    void setFile(Object value) { option('file',value) }
    Object getFile() { options['file'] }
    void setFrom(Object value) { option('from',value) }
    Object getFrom() { options['from'] }
    void setHelp(Object value) { option('help',value) }
    Object getHelp() { options['help'] }
    void setOutput(Object value) { option('output',value) }
    Object getOutput() { options['output'] }
    void setPlatform(Object value) { option('platform',value) }
    Object getPlatform() { options['platform'] }
    void setProfile(Object value) { option('profile',value) }
    Object getProfile() { options['profile'] }
    void setQuiet(Object value) { option('quiet',value) }
    Object getQuiet() { options['quiet'] }
    void setScope(Object value) { option('scope',value) }
    Object getScope() { options['scope'] }
    void setTemplate(Object value) { option('template',value) }
    Object getTemplate() { options['template'] }
    void setVerbose(Object value) { option('verbose',value) }
    Object getVerbose() { options['verbose'] }
    void setVex(Object value) { option('vex',value) }
    Object getVex() { options['vex'] }
    void setLoad(Object value) { option('load',value) }
    Object getLoad() { options['load'] }
    void setPackages(Object value) { option('packages',value) }
    Object getPackages() { options['packages'] }
    void setVulns(Object value) { option('vulns',value) }
    Object getVulns() { options['vulns'] }
    void setEcosystem(Object value) { option('ecosystem',value) }
    Object getEcosystem() { options['ecosystem'] }
    void setLimit(Object value) { option('limit',value) }
    Object getLimit() { options['limit'] }
    void setPkg(Object value) { option('pkg',value) }
    Object getPkg() { options['pkg'] }
    void setProvider(Object value) { option('provider',value) }
    Object getProvider() { options['provider'] }
    void setVuln(Object value) { option('vuln',value) }
    Object getVuln() { options['vuln'] }
    void setId(Object value) { option('id',value) }
    Object getId() { options['id'] }
    void setName(Object value) { option('name',value) }
    Object getName() { options['name'] }
    void setVersion(Object value) { option('version',value) }
    Object getVersion() { options['version'] }
}
