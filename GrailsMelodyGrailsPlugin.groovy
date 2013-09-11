import grails.plugin.melody.GrailsDataSourceBeanPostProcessor
import grails.plugin.melody.GrailsMelodyUtil
import net.bull.javamelody.MonitoringFilter
import net.bull.javamelody.MonitoringProxy
import net.bull.javamelody.Parameter
import net.bull.javamelody.Parameters
import net.bull.javamelody.SessionListener

class GrailsMelodyGrailsPlugin {

	def version = "1.46"
	def grailsVersion = "2.0 > *"

	def loadAfter = [
		'spring-security-core',
		'acegi',
		'shiro'
	]

	def title = "JavaMelody Grails Plugin"
	def description = 'Integrate JavaMelody Monitoring into grails application.'
	def documentation = "http://grails.org/plugin/grails-melody"

	def license = "LGPL3"
	def organization = [ name: "JavaMelody", url: "http://javamelody.googlecode.com" ]
	def developers = [
			[ name: "Liu Chao", email: "liuchao@goal98.com" ],
			[ name: "Emeric Vernat", email: "evernat@free.fr" ] ]
	def issueManagement = [ system: "GitHub", url: "https://github.com/evernat/grails-melody-plugin/issues" ]
	def scm = [ url: "https://github.com/evernat/grails-melody-plugin.git" ]

	def doWithSpring = {
		//Wrap grails datasource with java melody JdbcWapper
		grailsDataSourceBeanPostProcessor(GrailsDataSourceBeanPostProcessor)
	}

	def getWebXmlFilterOrder() {
		def FilterManager = getClass().getClassLoader().loadClass('grails.plugin.webxml.FilterManager')
		[monitoring : FilterManager.GRAILS_WEB_REQUEST_POSITION + 200]
	}

	def doWithWebDescriptor = {xml ->

		def contextParam = xml.'context-param'

		contextParam[contextParam.size() - 1] + {
			'filter' {
				'filter-name'('monitoring')
				'filter-class'(MonitoringFilter.name)
				//load configuration from GrailsMelodyConfig.groovy
				def conf = GrailsMelodyUtil.getGrailsMelodyConfig(application)?.javamelody
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
			'listener' { 'listener-class'(SessionListener.name) }
		}
	}

	private findMappingLocation = {xml ->

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
		def SPRING_COUNTER = MonitoringProxy.getSpringCounter()
		final boolean DISABLED = Boolean.parseBoolean(Parameters.getParameter(Parameter.DISABLED))

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
					List methods = delegate.metaClass.getMethods()
					boolean found = false
					for (MetaMethod method in methods) {
						if (method.getName() == name) {
							metaMethod = method
							found = true
							break
						}
					}
					if(!found && delegate."${name}"){
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

				final String requestName = "${serviceClass.name}.${name}"

				boolean systemError = false
				try {
					SPRING_COUNTER.bindContextIncludingCpu(requestName)
					return metaMethod.doMethodInvoke(delegate, args)
				} catch (final Error e) {
					systemError = true
					throw e
				} finally {
					SPRING_COUNTER.addRequestForCurrentContext(systemError)
				}
			}
		}
	}
}
