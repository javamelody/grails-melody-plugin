package grails.melody.plugin

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import groovy.util.logging.Slf4j
import net.bull.javamelody.MonitoringFilter
import net.bull.javamelody.SessionListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.embedded.FilterRegistrationBean
import org.springframework.boot.context.embedded.ServletContextInitializer
import org.springframework.context.annotation.Bean

import javax.servlet.DispatcherType
import javax.servlet.ServletContext
import javax.servlet.ServletException

/**
 * Class to initialize Melody Filter
 */
@Slf4j
class MelodyConfig implements GrailsApplicationAware {


    GrailsApplication grailsApplication

    @Bean
    public ServletContextInitializer melodyInitializer() {
        return new ServletContextInitializer() {
            @Override
            void onStartup(ServletContext servletContext) throws ServletException {
                servletContext.addListener(new SessionListener())
            }
        }
    }

    @Bean
    public FilterRegistrationBean melodyFilter() {
        log.debug "Creating Melody Filter..."
        FilterRegistrationBean melodyFilterBean = new FilterRegistrationBean()
        melodyFilterBean.setFilter(new MonitoringFilter())
        melodyFilterBean.setAsyncSupported(true);
        melodyFilterBean.setName(MonitoringFilter.name);
        melodyFilterBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
        def conf = GrailsMelodyUtil.getGrailsMelodyConfig(grailsApplication)?.javamelody
        conf?.each {
            String name = it.key
            String value = it.value
            log.debug "Grails Melody Param: $name = $value"

            melodyFilterBean.addInitParameter(name, value)
        }

        melodyFilterBean.addUrlPatterns("/*")

        melodyFilterBean

    }

}
