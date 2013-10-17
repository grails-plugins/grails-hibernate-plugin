grails.project.work.dir = 'target'

forkConfig = false
grails.project.fork = [
    test:    forkConfig, // configure settings for the test-app JVM
    run:     forkConfig, // configure settings for the run-app JVM
    war:     forkConfig, // configure settings for the run-war JVM
    console: forkConfig, // configure settings for the Swing console JVM
    compile: forkConfig  // configure settings for compilation
]

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {

    inherits "global"
    log "warn"

    repositories {
        mavenLocal()
        grailsCentral()
        mavenRepo "http://repo.grails.org/grails/core"
    }

    dependencies {
        String datastoreVersion = '2.0.2.RELEASE'

        compile "org.grails:grails-datastore-gorm-hibernate:$datastoreVersion"

        runtime 'cglib:cglib:2.2.2'

        test 'org.spockframework:spock-grails-support:0.7-groovy-2.0', {
            export = false
        }
    }

    plugins {
        build(':release:3.0.1', ':rest-client-builder:1.0.3') {
            export = false
        }

        test ':spock:0.7', {
            exclude 'spock-grails-support'
            export = false
        }

        test ':scaffolding:1.0.0', {
            export = false
        }
    }
}
