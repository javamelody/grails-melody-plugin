import net.bull.javamelody.MonitoringFilter
import net.bull.javamelody.SessionListener
import net.bull.javamelody.MonitoringProxy
import net.bull.javamelody.Counter
import net.bull.javamelody.Parameter
import net.bull.javamelody.Parameters

class GrailsMelodyGrailsPlugin {
    // the plugin version
    def version = "1.21"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2.4 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    def loadAfter = ['spring-security-core', 'acegi', 'shiro']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Liu Chao"
    def authorEmail = "liuchao@goal98.com"
    def title = "Grails Java Melody Plugin"
    def description = '''\\
Integrate Java Melody Monitor into grails application.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/grails-melody"

    def license = "LGPL3"
    def organization = [ name: "JavaMelody", url: "http://javamelody.googlecode.com" ]
    def developers = [
            [ name: "Liu Chao", email: "" ],
            [ name: "Emeric Vernat", email: "" ] ]
    def issueManagement = [ system: "github", url: "https://github.com/evernat/grails-melody-plugin/issues" ]
    def scm = [ url: "git://github.com/evernat/grails-melody-plugin.git" ]

    def doWithSpring = {
        //Wrap grails datasource with java melody JdbcWapper
        'grailsDataSourceBeanPostProcessor'(GrailsDataSourceBeanPostProcessor)

    }

    def doWithApplicationContext = {applicationContext ->

    }

	def getWebXmlFilterOrder() {
		def FilterManager = getClass().getClassLoader().loadClass('grails.plugin.webxml.FilterManager')
		[ monitoring : FilterManager.GRAILS_WEB_REQUEST_POSITION + 200]
	}

    def doWithWebDescriptor = {xml ->

        def contextParam = xml.'context-param'

        contextParam[contextParam.size() - 1] + {
            'filter' {
                'filter-name'('monitoring')
                'filter-class'(MonitoringFilter.name)
                //load configuration from GrailsMelodyConfig.groovy
                def conf = GrailsMelodyUtil.grailsMelodyConfig?.javamelody
				conf?.each {
                    String name = it.key
                    String value = it.value
                    log.debug "Grails Melody Param: $name = $value"
                    'init-param' {
                        'param-name'(name)
                        'param-value'(value)
                    }
                }
            }
        }

        findMappingLocation.delegate = delegate
        def mappingLocation = findMappingLocation(xml)
        mappingLocation + {
            'filter-mapping' {
                'filter-name'('monitoring')
                'url-pattern'('/*')
            }
        }


        def filterMapping = xml.'filter-mapping'
        filterMapping[filterMapping.size() - 1] + {

            'listener' {
                'listener-class'(SessionListener.name)
            }
        }

    }

    private def findMappingLocation = {xml ->

        // find the location to insert the filter-mapping; needs to be after the 'charEncodingFilter'
        // which may not exist. should also be before the sitemesh filter.
        // thanks to the JSecurity plugin for the logic.

        def mappingLocation = xml.'filter-mapping'.find { it.'filter-name'.text() == 'charEncodingFilter' }
        if (mappingLocation) {
            return mappingLocation
        }

        // no 'charEncodingFilter'; try to put it before sitemesh
        int i = 0
        int siteMeshIndex = -1
        xml.'filter-mapping'.each {
            if (it.'filter-name'.text().equalsIgnoreCase('sitemesh')) {
                siteMeshIndex = i
            }
            i++
        }
        if (siteMeshIndex > 0) {
            return xml.'filter-mapping'[siteMeshIndex - 1]
        }

        if (siteMeshIndex == 0 || xml.'filter-mapping'.size() == 0) {
            def filters = xml.'filter'
            return filters[filters.size() - 1]
        }

        // neither filter found
        def filters = xml.'filter'
        return filters[filters.size() - 1]
    }

    def doWithDynamicMethods = {ctx ->
        //For each service class in Grails, the plugin use groovy meta programming (invokeMethod)
        //to 'intercept' method call and collect infomation for monitoring purpose.
        //The code below mimics 'MonitoringSpringInterceptor.invoke()'
        def SPRING_COUNTER = MonitoringProxy.getSpringCounter();
		final boolean DISABLED = Boolean.parseBoolean(Parameters.getParameter(Parameter.DISABLED));

        if (DISABLED || !SPRING_COUNTER.isDisplayed()) {
			return
		}
		
        //Enable groovy meta programming
        ExpandoMetaClass.enableGlobally()

        application.serviceClasses.each {serviceArtifactClass ->
            def serviceClass = serviceArtifactClass.getClazz()

            serviceClass.metaClass.invokeMethod = {String name, args ->
                def metaMethod = delegate.metaClass.getMetaMethod(name, args)
                if (!metaMethod) {
                    List methods = delegate.metaClass.getMethods();
                    boolean found = false
                    for (int i = 0; i < methods.size(); i++) {
                        groovy.lang.MetaMethod method = (groovy.lang.MetaMethod) methods.get(i);
                        if (method.getName() == name) {
                            metaMethod = method
                            found = true
                            break
                        }

                    }
					if(!found && delegate.metaClass.properties.find {it.name == name}){
						def property = delegate."${name}"
						if(property instanceof Closure){
							found = true
							metaMethod = [doMethodInvoke: {dlg, arguments-> property.call(arguments)}]
						}
					}
                    if (!found){
						return delegate.metaClass.invokeMissingMethod(delegate, name, args)
/*						throw new MissingMethodException(name, delegate.class, args)*/
					}
                }

                if (DISABLED || !SPRING_COUNTER.isDisplayed()) {
                    return metaMethod.doMethodInvoke(delegate, args)
                }

				final String requestName = "${serviceClass.name}.${name}";

				boolean systemError = false;
				try {
					SPRING_COUNTER.bindContextIncludingCpu(requestName);
					return metaMethod.doMethodInvoke(delegate, args)
				} catch (final Error e) {
					systemError = true;
					throw e;
				} finally {
					SPRING_COUNTER.addRequestForCurrentContext(systemError);
				}
            }
        }
    }

    def onChange = {event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = {event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
