package grails.melody.plugin

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@Integration
class ExpandoMetaClassIntegrationSpec extends Specification {

    @Transactional
    @Rollback
    def 'saving an domain instance should not throw IllegalStateException'() {
        when:
        new SampleDomain().save(flush:true)

        then:
        notThrown IllegalStateException
    }
}