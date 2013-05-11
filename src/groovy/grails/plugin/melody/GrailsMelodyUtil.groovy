package grails.plugin.melody

import grails.util.Environment

import org.codehaus.groovy.grails.commons.GrailsApplication

class GrailsMelodyUtil {

	static ConfigObject getGrailsMelodyConfig(GrailsApplication application) {
		def config = application.config
		GroovyClassLoader classLoader = new GroovyClassLoader(application.getClassLoader())
		try {
			config.merge(new ConfigSlurper(Environment.current.name()).parse(classLoader.loadClass('GrailsMelodyConfig')))
		} catch (Exception e) {
			// ignored, use defaults
		}
		return config
	}
}
