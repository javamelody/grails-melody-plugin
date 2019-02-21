package grails.melody.plugin

import app.integration.tests.Application
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import net.bull.javamelody.JdbcWrapper
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@Integration(applicationClass = Application)
class GrailsTransactionSpec extends Specification {

    static {
        //avoid relying on counters from previous execution
        System.setProperty("javamelody.storage-directory", GrailsTransactionSpec.class.getResource(".").getFile())
    }

    @Transactional
    @Rollback
    def 'saving an domain instance should not throw IllegalStateException - validate connection wrapper'() {
        when:
        new SampleDomain().save(flush:true)

        then:
        notThrown IllegalStateException
        SampleDomain.count() == 1

        def requests = JdbcWrapper.SINGLETON.sqlCounter.requests
        requests.size() > 0
        requests[0].name.startsWith('insert into sample_domain')
        requests[1].name.startsWith('select count') //Domain class afterInsert method
    }

}