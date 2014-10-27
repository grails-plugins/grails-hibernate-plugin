if(System.getenv('TRAVIS_BRANCH')) {
    grails.project.repos.grailsCentral.username = System.getenv("GRAILS_CENTRAL_USERNAME")
    grails.project.repos.grailsCentral.password = System.getenv("GRAILS_CENTRAL_PASSWORD")    
}


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
        mavenRepo "https://repo.grails.org/grails/core"
        mavenRepo "https://repo.grails.org/grails/libs-snapshots-local"
    }

    dependencies {
        String datastoreVersion = '3.1.2.RELEASE'
        String hibernateVersion = '3.6.10.Final'

        compile "org.grails:grails-datastore-core:$datastoreVersion",
                "org.grails:grails-datastore-gorm:$datastoreVersion",
                "org.grails:grails-datastore-gorm-hibernate:$datastoreVersion",
                "org.grails:grails-datastore-simple:$datastoreVersion", {
                exclude group:'org.springframework', name:'spring-context'
                exclude group:'org.springframework', name:'spring-core'
                exclude group:'org.springframework', name:'spring-beans'
                exclude group:'org.grails', name:'grails-bootstrap'
                exclude group:'org.grails', name:'grails-core'                
                exclude group:'org.grails', name:'grails-async'                 
        }
                
        compile 'commons-collections:commons-collections:3.2.1'
        compile("org.hibernate:hibernate-core:$hibernateVersion") {
            exclude group:'commons-logging', name:'commons-logging'
            exclude group:'commons-collections', name:'commons-collections'
            exclude group:'org.slf4j', name:'slf4j-api'
            exclude group:'xml-apis', name:'xml-apis'
            exclude group:'dom4j', name:'dom4j'
            exclude group:'antlr', name: 'antlr'
            exclude group:'org.hibernate.javax.persistence', name:'hibernate-jpa-2.0-api'
        }
        compile "javax.validation:validation-api:1.0.0.GA"
        compile "org.hibernate:hibernate-validator:4.1.0.Final"        
        compile "org.hibernate:hibernate-entitymanager:$hibernateVersion", {
            exclude group:'javassist', name:'javassist'
            exclude group:'org.hibernate.javax.persistence', name:'hibernate-jpa-2.0-api'
        } 
        compile "org.hibernate:hibernate-commons-annotations:3.2.0.Final"
        runtime "org.hibernate:hibernate-ehcache:$hibernateVersion", {
            exclude group: 'net.sf.ehcache', name: 'ehcache-core'
        }
        runtime "net.sf.ehcache:ehcache-core:2.4.8"
    }

    plugins {
        build(':release:3.0.1', ':rest-client-builder:2.0.3') {
            export = false
        }

        test ':scaffolding:1.0.0', {
            export = false
        }
    }
}
