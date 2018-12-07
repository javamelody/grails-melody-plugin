package grails.melody.plugin

class SampleDomain {
    def afterInsert() {
        SampleDomain.withNewSession {
            log.debug "Face count = ${Face.count()}"
        }
    }
}