package grails.plugin.melody

import org.springframework.beans.factory.config.BeanPostProcessor
import javax.sql.DataSource
import net.bull.javamelody.JdbcWrapper

class GrailsDataSourceBeanPostProcessor implements BeanPostProcessor {

    Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean
    }

    Object postProcessAfterInitialization(Object bean, String beanName) {

        if (bean instanceof DataSource && "dataSource" == beanName){
            return JdbcWrapper.SINGLETON.createDataSourceProxy(bean)
        }

        return bean
    }
}
