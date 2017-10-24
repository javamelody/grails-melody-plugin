package grails.melody.plugin

import net.bull.javamelody.JdbcWrapper
import org.springframework.beans.factory.config.BeanPostProcessor

import javax.sql.DataSource

class GrailsDataSourceBeanPostProcessor implements BeanPostProcessor {

    Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean
    }

    Object postProcessAfterInitialization(Object bean, String beanName) {

        if (bean instanceof DataSource) {
            return JdbcWrapper.SINGLETON.createDataSourceProxy(bean)
        }

        return bean
    }
}