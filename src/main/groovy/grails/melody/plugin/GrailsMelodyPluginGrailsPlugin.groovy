package grails.melody.plugin

import grails.plugins.Plugin
import net.bull.javamelody.SpringDataSourceBeanPostProcessor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GrailsMelodyPluginGrailsPlugin extends Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(GrailsMelodyPluginGrailsPlugin.class)

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = '3.2 > *'
    def loadAfter = ['spring-security-core', 'acegi', 'shiro', 'quartz']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            'grails-app/views/*',
            'grails-app/init/*'
    ]

    def title = 'JavaMelody Grails Plugin'
    def description = 'Integrate JavaMelody monitoring into Grails application.'
    def documentation = 'http://grails.org/plugin/grails-melody'

    def license = 'ASL'
    def organization = [name: 'JavaMelody', url: 'https://github.com/javamelody/javamelody/wiki']

    def developers = [
            [name: 'Liu Chao', email: 'liuchao@goal98.com'],
            [name: 'Emeric Vernat', email: 'evernat@free.fr'],
            [name: 'Sérgio Michels', email: 'sergiomichelss@gmail.com']
    ]

    def issueManagement = [system: 'GitHub', url: 'https://github.com/javamelody/grails-melody-plugin/issues']
    def scm = [url: 'https://github.com/javamelody/grails-melody-plugin.git']

    void doWithDynamicMethods() {
        new MelodyInterceptorEnhancer().enhance(getGrailsApplication())
    }

    Closure doWithSpring() {
        { ->
            melodyConfig(MelodyConfig)

            //Wrap grails datasource with java melody JdbcWapper
            springDataSourceBeanPostProcessor(SpringDataSourceBeanPostProcessor) {
                excludedDatasources = ['dataSource', 'dataSourceLazy']
            }
        }
    }
}
