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
        String datastoreVersion = '3.0.0.BUILD-SNAPSHOT'

        compile "org.grails:grails-datastore-gorm-hibernate:$datastoreVersion", 
                "org.grails:grails-datastore-simple:$datastoreVersion"

        compile('org.hibernate:hibernate-core:3.6.10.Final') {
            exclude group:'commons-logging', name:'commons-logging'
            exclude group:'commons-collections', name:'commons-collections'
            exclude group:'org.slf4j', name:'slf4j-api'
            exclude group:'xml-apis', name:'xml-apis'
            exclude group:'dom4j', name:'dom4j'
            exclude group:'antlr', name: 'antlr'
        }
        compile "javax.validation:validation-api:1.0.0.GA"
        compile "org.hibernate:hibernate-validator:4.1.0.Final"
        
        runtime "org.hibernate:hibernate-entitymanager:3.6.10.Final" 
        runtime "org.hibernate:hibernate-commons-annotations:3.2.0.Final"
        runtime "org.hibernate:hibernate-ehcache:3.6.10.Final"
        runtime "net.sf.ehcache:ehcache-core:2.4.6"
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
