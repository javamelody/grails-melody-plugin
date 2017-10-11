package grails.melody.plugin

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import net.bull.javamelody.MonitoringProxy
import net.bull.javamelody.Parameter
import net.bull.javamelody.internal.common.Parameters

/**
 * Enhance Grails artefacts, intercepting method calls for monitoring purpose.
 */
@Slf4j
class MelodyInterceptorEnhancer {

    void enhance(GrailsApplication grailsApplication) {
        //For each service class in Grails, the plugin use groovy meta programming (invokeMethod)
        //to 'intercept' method call and collect infomation for monitoring purpose.
        //The code below mimics 'MonitoringSpringInterceptor.invoke()'
        def SPRING_COUNTER = MonitoringProxy.getSpringCounter()
        final boolean DISABLED = GrailsMelodyUtil.getGrailsMelodyConfig(grailsApplication)?.javamelody?.disabled || Parameter.DISABLED.getValueAsBoolean()

        if (DISABLED || Parameters.isCounterHidden(SPRING_COUNTER.getName())) {
            if (DISABLED) {
               log.debug("Melody is disabled, services will not be enhanced.")
            } else {
                log.debug("Spring counter is not displayed, services will not be enhanced.")
            }
            return
        }

        //Enable groovy meta programming
        ExpandoMetaClass.enableGlobally()

        grailsApplication.serviceClasses.each { serviceArtifactClass ->

            def serviceClass = serviceArtifactClass.getClazz()

            log.debug("Enhancing ${serviceClass}")

            serviceClass.metaClass.invokeMethod = { String name, args ->
                def metaMethod = delegate.metaClass.getMetaMethod(name, args)
                if (!metaMethod) {
                    List methods = delegate.metaClass.getMethods()
                    boolean found = false
                    for (MetaMethod method in methods) {
                        if (method.getName() == name) {
                            def parameterTypes = method.nativeParameterTypes
                            if (parameterTypes.length == args.length) {
                                found = true
                                for (int i = 0; i < parameterTypes.length; i++) {
                                    if ((args[i] != null && !parameterTypes[i].isAssignableFrom(args[i].getClass())) || (parameterTypes[i].primitive && args[i] == null)) {
                                        found = false
                                        break
                                    }
                                }
                                if (found) {
                                    metaMethod = method
                                    break
                                }
                            }
                        }
                    }
                    if (!found && delegate.metaClass.hasProperty(delegate, name)) {
                        def property = delegate."${name}"
                        if (property instanceof Closure) {
                            found = true
                            metaMethod = [doMethodInvoke: { dlg, arguments ->
                                def theArgs = arguments ? arguments.size() == 1 ? arguments[0] : arguments as List : null
                                property.call(theArgs)
                            }]
                        }
                    }
                    if (!found) {
                        return delegate.metaClass.invokeMissingMethod(delegate, name, args)
                        /*throw new MissingMethodException(name, delegate.class, args)*/
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
