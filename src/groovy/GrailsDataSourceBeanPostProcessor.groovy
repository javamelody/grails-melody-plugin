import org.springframework.beans.factory.config.BeanPostProcessor
import javax.sql.DataSource
import net.bull.javamelody.JdbcWrapper
/**
 * Created by IntelliJ IDEA.
 * User: liuchao
 * Date: Nov 26, 2009
 * Time: 11:37:46 PM
 * To change this template use File | Settings | File Templates.
 */
class GrailsDataSourceBeanPostProcessor implements BeanPostProcessor{

    Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    Object postProcessAfterInitialization(Object bean, String beanName) {

        def result = bean

        if(bean instanceof DataSource && "dataSource" == beanName){
            result = JdbcWrapper.SINGLETON.createDataSourceProxy(bean)

        }
        return result;
    }

}
