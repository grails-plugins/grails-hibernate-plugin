package org.grails.scaffolding

import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.containsString

import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import groovy.lang.GroovyClassLoader;

import grails.core.DefaultGrailsDomainClass
import grails.core.GrailsDomainConfigurationUtil

import org.grails.plugins.GrailsPlugin
import org.grails.plugins.MockGrailsPluginManager
import org.grails.plugins.PluginManagerHolder
import org.grails.orm.hibernate.cfg.GrailsDomainBinder

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DefaultGrailsTemplateGeneratorTests extends GroovyTestCase {

    public static GrailsPlugin fakeHibernatePlugin = [getName: { -> 'hibernate' }] as GrailsPlugin

    private MockGrailsPluginManager pluginManager
    private GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader())
    private DefaultGrailsTemplateGenerator templateGenerator = new DefaultGrailsTemplateGenerator(gcl)
    private GrailsDomainBinder grailsDomainBinder = new GrailsDomainBinder()

    protected void setUp() {
        def buildSettings = new BuildSettings(new File("."))
        BuildSettingsHolder.settings = buildSettings
        pluginManager = new MockGrailsPluginManager()
        pluginManager.registerMockPlugin fakeHibernatePlugin
        templateGenerator.basedir = "."
        templateGenerator.pluginManager = pluginManager
    }

    protected void tearDown() {
        BuildSettingsHolder.settings = null
    }

    GroovyClassLoader gcl = new GroovyClassLoader()
        String testDomain = '''
import grails.persistence.*

@Entity
class ScaffoldingTest {

   Integer status
   Date regularDate
   java.sql.Date sqlDate

   static constraints = {
      status inList:[1,2,3,4]
   }
}
'''

    void testGenerateDateSelect() {

        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")

        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def domainClass = new DefaultGrailsDomainClass(testClass)

        StringWriter sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assert sw.toString().contains('g:datePicker name="regularDate" precision="day"  value="${scaffoldingTestInstance?.regularDate}"') == true
        assert sw.toString().contains('datePicker name="sqlDate" precision="day"  value="${scaffoldingTestInstance?.sqlDate}') == true
    }

    void testGenerateNumberSelect() {
        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")

        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def domainClass = new DefaultGrailsDomainClass(testClass)

        StringWriter sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assertThat "Should have rendered a select box for the number editor",
            sw.toString(),
            containsString('g:select name="status" from="${scaffoldingTestInstance.constraints.status.inList}" required="" value="${fieldValue(bean: scaffoldingTestInstance, field: \'status\')}" valueMessagePrefix="scaffoldingTest.status"')
    }

    void testDoesNotGenerateInputForId() {
        gcl.parseClass(testDomain)
        def testClass = gcl.loadClass("ScaffoldingTest")
        def domainClass = new DefaultGrailsDomainClass(testClass)
        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        def sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assert !sw.toString().contains('name="id"'), "Should not have rendered an input for the id"
    }

    void testGeneratesInputForAssignedId() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class ScaffoldingTest {
    String id
    static mapping = {
        id generator: "assigned"
    }
}
        ''')
        def testClass = gcl.loadClass("ScaffoldingTest")
        def domainClass = new DefaultGrailsDomainClass(testClass)
        def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(testClass)
        testClass.metaClass.getConstraints = {-> constrainedProperties }

        grailsDomainBinder.evaluateMapping(domainClass)
        assert grailsDomainBinder.getMapping(domainClass)?.identity?.generator == 'assigned'

        def sw = new StringWriter()
        templateGenerator.generateView domainClass, "_form", sw

        assertThat "Should have rendered an input for the id",
                sw.toString(),
                containsString('g:textField name="id" value="${scaffoldingTestInstance?.id}"')
    }
}
