import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class GrailsMelodyUtil {
    static ConfigObject getGrailsMelodyConfig() {
		def config = ConfigurationHolder.config
		GroovyClassLoader classLoader = new GroovyClassLoader(GrailsMelodyUtil.getClassLoader())
		try {
			config.merge(new ConfigSlurper(GrailsUtil.environment).parse(classLoader.loadClass('GrailsMelodyConfig')))
		} catch (Exception e) {
			// ignored, use defaults
		}
		return config
	}
}
