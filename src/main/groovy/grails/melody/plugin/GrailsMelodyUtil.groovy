package grails.melody.plugin

import grails.core.GrailsApplication
import grails.util.Environment


class GrailsMelodyUtil {

	static ConfigObject getGrailsMelodyConfig(GrailsApplication application) {
		def config = application.config
		GroovyClassLoader classLoader = new GroovyClassLoader(application.getClassLoader())
		try {
			config.merge(new ConfigSlurper(Environment.current.name().toLowerCase()).parse(classLoader.loadClass('GrailsMelodyConfig')))
		} catch (Exception e) {
			// ignored, use defaults
		}
		return config
	}
}
