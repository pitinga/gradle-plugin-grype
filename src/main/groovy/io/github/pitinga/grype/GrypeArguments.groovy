package io.github.pitinga.grype
import org.gradle.api.GradleException
class GrypeArguments {
    static final List<String> SOURCES = ['auto','dir','file','sbom','docker','registry','podman','containerd','docker-archive','oci-archive','oci-dir','singularity','purl','cpes','zarf']
    static String choice(Map options, String key, List<String> allowed) {
        String value = options[key]
        if (!allowed.contains(value)) throw new GradleException('Invalid grype.' + key + ': ' + value + '; expected ' + allowed)
        value
    }
    static boolean flag(Map options,String key) {
        if (!(options[key] in ['true','false'])) throw new GradleException('grype.' + key + ' must be true or false')
        options[key] == 'true'
    }
    static String source(Map options,File directory,File artifact,List<String> fromSources) {
        String type = choice(options,'sourceType',SOURCES)
        String target = options.target?.trim()
        if (!target) {
            if (artifact == null) throw new GradleException('No bootJar/jar task found; configure grype.target')
            if (!(type in ['auto','file']) || fromSources) throw new GradleException('Default JAR requires sourceType auto/file and no fromSources')
            return 'file:' + artifact.absolutePath
        }
        if (type != 'auto' && (fromSources || SOURCES.findAll { it != 'auto' }.any { target.startsWith(it + ':') })) throw new GradleException('Do not combine sourceType with source prefix or fromSources')
        File local = new File(target)
        if (!local.absolute) local = new File(directory,target)
        if (type == 'auto') {
            if (!fromSources && local.exists()) return (local.directory ? 'dir:' : 'file:') + local.absolutePath
            return target
        }
        if (type in ['dir','file','sbom','docker-archive','oci-archive','oci-dir','singularity','purl','cpes','zarf']) {
            if (!local.exists()) throw new GradleException('Grype target does not exist: ' + local)
            if ((type in ['dir','oci-dir']) != local.directory) throw new GradleException('Incorrect target type: ' + local)
            return type + ':' + local.absolutePath
        }
        return type + ':' + target
    }
    static List<String> build(Map options,Map lists) {
        List<String> formats = [GrypeReportOutput.checkedFormat(options.format)] + lists.additionalFormats.collect { GrypeReportOutput.checkedFormat(it) }
        if (flag(options,'onlyFixed') && flag(options,'onlyNotFixed')) throw new GradleException('onlyFixed and onlyNotFixed are mutually exclusive')
        if (flag(options,'showSuppressed') && formats != ['table']) throw new GradleException('showSuppressed requires table only')
        if (formats.contains('template') != (options.template?.trim() as boolean)) throw new GradleException('template format requires a template file and vice versa')
        if (!(options.verbose in ['0','1','2'])) throw new GradleException('verbose must be 0, 1 or 2')
        if (flag(options,'quiet') && options.verbose != '0') throw new GradleException('quiet and verbose are mutually exclusive')
        List<String> args = []
        formats.each { args.addAll(['--output',it]) }
        String fail = choice(options,'failOn',['none','negligible','low','medium','high','critical'])
        if (fail != 'none') args.addAll(['--fail-on',fail])
        String scope = choice(options,'scope',['squashed','all-layers','deep-squashed'])
        if (scope != 'squashed') args.addAll(['--scope',scope])
        String sort = choice(options,'sortBy',['package','severity','epss','risk','kev','vulnerability'])
        if (sort != 'risk') args.addAll(['--sort-by',sort])
        if (options.ignoreStates && !options.ignoreStates.split(',').every { it.trim() in ['fixed','not-fixed','unknown','wont-fix'] }) throw new GradleException('Invalid ignoreStates')
        [distro:'--distro',platform:'--platform',name:'--name',template:'--template',ignoreStates:'--ignore-states'].each { key, option ->
            if (options[key]?.trim()) args.addAll([option,options[key].trim()])
        }
        [excludePaths:'--exclude',fromSources:'--from',configFiles:'--config',profiles:'--profile',vexFiles:'--vex'].each { key, option ->
            lists[key].findAll { it?.trim() }.each { args.addAll([option,it]) }
        }
        [onlyFixed:'--only-fixed',onlyNotFixed:'--only-notfixed',byCve:'--by-cve',addCpesIfNone:'--add-cpes-if-none',showSuppressed:'--show-suppressed',quiet:'--quiet'].each { key, option ->
            if (flag(options,key)) args.add(option)
        }
        if (options.verbose != '0') args.add(options.verbose == '1' ? '-v' : '-vv')
        args.addAll(lists.additionalArguments)
        return args
    }
}
