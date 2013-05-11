grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        compile "net.bull.javamelody:javamelody-core:1.44.0"
        compile ("com.lowagie:itext:2.1.7") {excludes "bcmail-jdk14", "bcprov-jdk14", "bctsp-jdk14"}
        compile "org.jrobin:jrobin:1.5.9"
    }

    plugins {
        build ':release:2.2.1', ':rest-client-builder:1.0.3', {
            export = false
        }
    }
}
