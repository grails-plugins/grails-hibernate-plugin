/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.scaffolding

import grails.util.BuildSettings
import grails.util.BuildSettingsHolder

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import junit.framework.TestCase

import grails.core.DefaultGrailsDomainClass
import grails.core.GrailsClassUtils
import grails.core.GrailsDomainClass
import org.grails.plugins.GrailsPluginUtils
import org.grails.plugins.MockGrailsPluginManager
import org.grails.plugins.PluginManagerHolder

/**
 * @author Graeme Rocher
 */
class GrailsTemplateGeneratorsTests extends TestCase {

    private GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader())
    private DefaultGrailsTemplateGenerator generator = new DefaultGrailsTemplateGenerator(gcl)

    @Override
    protected void setUp() {
        BuildSettings buildSettings = new BuildSettings(new File("."))
        buildSettings.setProjectPluginsDir(new File("target/plugins"))
        BuildSettingsHolder.setSettings(buildSettings)
        MockGrailsPluginManager pluginManager = new MockGrailsPluginManager()
        PluginManagerHolder.setPluginManager(pluginManager)
        pluginManager.registerMockPlugin(DefaultGrailsTemplateGeneratorTests.fakeHibernatePlugin)
        generator.basedir = "."
        generator.setPluginManager(pluginManager)
        GrailsPluginUtils.getPluginBuildSettings().setPluginDirPath("target/plugins")
    }

    @Override
    protected void tearDown() {
        BuildSettingsHolder.setSettings(null)
        PluginManagerHolder.setPluginManager(null)
    }

    void testGenerateController() throws Exception {

        Class dc = gcl.parseClass("""
class Test {
    Long id
    Long version
}""")
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc)

        File generatedFile = new File("test/grails-app/controllers/TestController.groovy")
        if (generatedFile.exists()) {
            generatedFile.delete()
        }

        StringWriter sw = new StringWriter()
        generator.generateController(domainClass,sw)

        String text = sw.toString()

        Class controllerClass = gcl.parseClass(text)

        assertEquals("TestController", controllerClass.getName())

        verifyMethod(controllerClass, "create", [] as Class[])
        verifyMethod(controllerClass, "delete", [Long] as Class[])
        verifyMethod(controllerClass, "edit", [Long] as Class[])
        verifyMethod(controllerClass, "index", [] as Class[])
        verifyMethod(controllerClass, "list", [Integer] as Class[])
        verifyMethod(controllerClass, "save", [] as Class[])
        verifyMethod(controllerClass, "show", [Long] as Class[])
        verifyMethod(controllerClass, "update", [Long, Long] as Class[])

        Object propertyValue = GrailsClassUtils.getStaticPropertyValue(controllerClass, "allowedMethods")
        assertTrue("allowedMethods property was the wrong type", propertyValue instanceof Map)
        Map map = (Map) propertyValue
        assertTrue("allowedMethods did not contain the delete action", map.containsKey("delete"))
        assertTrue("allowedMethods did not contain the save action", map.containsKey("save"))
        assertTrue("allowedMethods did not contain the update action", map.containsKey("update"))

        assertEquals("allowedMethods had incorrect value for delete action", "POST", map.get("delete"))
        assertEquals("allowedMethods had incorrect value for save action", "POST", map.get("save"))
        assertEquals("allowedMethods had incorrect value for update action", "POST", map.get("update"))
    }

    private void verifyMethod(Class<?> controllerClass, String name, Class[] args) throws SecurityException, NoSuchMethodException {
        Method method = controllerClass.getMethod(name, args)
        assertTrue(Modifier.isPublic(method.getModifiers()))
        assertFalse(Modifier.isFinal(method.getModifiers()))
        assertSame(Object, method.getReturnType())
    }

    void testGenerateViews() throws Exception {

        Class dc = gcl.parseClass("""
class Test {
    Long id
    Long version
    String name
    TimeZone tz
    Locale locale
    Currency currency
    Boolean active
    Date age
}""")
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc)

        File show = new File("test/grails-app/views/test/show.gsp")
        show.deleteOnExit()
        File list = new File("test/grails-app/views/test/list.gsp")
        list.deleteOnExit()
        File edit = new File("test/grails-app/views/test/edit.gsp")
        edit.deleteOnExit()
        File create = new File("test/grails-app/views/test/create.gsp")
        create.deleteOnExit()
        File form = new File("test/grails-app/views/test/_form.gsp")
        form.deleteOnExit()

        generator.generateViews(domainClass,"test")

        assertTrue(show.exists())
        assertTrue(list.exists())
        assertTrue(edit.exists())
        assertTrue(create.exists())
        assertTrue(form.exists())
    }
}
