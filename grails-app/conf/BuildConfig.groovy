grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir="target/work"
grails.project.dependency.resolution = {

    inherits "global"

    log "warn"

    repositories {
        grailsCentral()
    }

    dependencies {
        compile('org.hibernate:hibernate-core:3.6.7.Final') {
            exclude group:'commons-logging', name:'commons-logging'            
            exclude group:'commons-collections', name:'commons-collections'
            exclude group:'org.slf4j', name:'slf4j-api'
            exclude group:'xml-apis', name:'xml-apis'            
        }
        compile('org.hibernate:hibernate-validator:4.1.0.Final') {
            exclude group:'commons-logging', name:'commons-logging'            
            exclude group:'commons-collections', name:'commons-collections'
            exclude group:'org.slf4j', name:'slf4j-api'
        }

        runtime 'javassist:javassist:3.12.0.GA'
        runtime 'antlr:antlr:2.7.6'
        runtime('dom4j:dom4j:1.6.1') {
            exclude group:'xml-apis', name:'xml-apis'
        }
        runtime('org.hibernate:hibernate-ehcache:3.6.7.Final') {
             exclude group:'commons-logging', name:'commons-logging'
             exclude group:'commons-collections', name:'commons-collections'
             exclude group:'org.slf4j', name:'slf4j-api'
             exclude group:'org.hibernate', name:'hibernate-core'
             exclude group:'net.sf.ehcache', name:'ehcache'
             exclude group:'net.sf.ehcache', name:'ehcache-core'             
        }
    }

    plugins {
        build(":release:1.0.0.RC3") {
            export = false
        }
    }
}
