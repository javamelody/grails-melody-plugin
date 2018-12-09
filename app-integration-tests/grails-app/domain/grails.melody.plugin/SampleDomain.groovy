package grails.melody.plugin

class SampleDomain {

    def afterInsert() {
        log.info 'After insert of Sample Domain'
        SampleDomain.withNewSession {
            log.info "Domain count = ${SampleDomain.count()}"
        }
    }
}