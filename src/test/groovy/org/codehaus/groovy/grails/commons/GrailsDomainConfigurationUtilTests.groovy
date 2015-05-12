/* Copyright 2004-2005 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package grails.core

import junit.framework.TestCase

import org.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.grails.orm.hibernate.validation.HibernateConstraintsEvaluator
import org.grails.plugins.GrailsPlugin
import org.grails.plugins.MockGrailsPluginManager
import org.grails.plugins.PluginManagerHolder
import org.grails.validation.ConstrainedProperty
import org.grails.validation.NullableConstraint
import org.hibernate.cfg.ImprovedNamingStrategy

/**
 * Tests for the GrailsDomainConfigurationUtil class.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
class GrailsDomainConfigurationUtilTests extends TestCase {

    @Override
    protected void setUp() {
        super.setUp()
        ExpandoMetaClass.enableGlobally()
        MockGrailsPluginManager pluginManager = new MockGrailsPluginManager()
        PluginManagerHolder.setPluginManager(pluginManager)
        pluginManager.registerMockPlugin([getName: { -> 'hibernate' }] as GrailsPlugin)
    }

    @Override
    protected void tearDown() {
        super.tearDown()
        GrailsDomainBinder grailsDomainBinder = new GrailsDomainBinder()
        grailsDomainBinder.NAMING_STRATEGIES.clear()
        grailsDomainBinder.NAMING_STRATEGIES.put(
              GrailsDomainClassProperty.DEFAULT_DATA_SOURCE, ImprovedNamingStrategy.INSTANCE)
        PluginManagerHolder.setPluginManager(null)
    }

    void testIsNotConfigurational() {
        assertTrue(GrailsDomainConfigurationUtil.isNotConfigurational("foo"))
        assertFalse(GrailsDomainConfigurationUtil.isNotConfigurational("belongsTo"))
    }

    void testIsBasicType() {
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(boolean.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(long.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(int.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(short.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(char.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(double.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(float.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(byte.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Boolean.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Long.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Integer.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Short.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Character.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Double.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Float.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Byte.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Date.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(URL.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(URI.class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(boolean[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(long[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(int[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(short[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(char[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(double[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(float[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(byte[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Boolean[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Long[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Integer[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Short[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Character[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Double[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Float[].class))
        assertTrue(GrailsDomainConfigurationUtil.isBasicType(Byte[].class))
    }

    void testEvaluateConstraintsInsertableShouldBeNullableByDefault() {
        GroovyClassLoader cl = new GroovyClassLoader()
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "class TestInsertableUpdateableDomain {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    String testString1 \n" +
                "    String testString2 \n"+
                "\n" +
                "    static mapping = {\n" +
                "       testString1 insertable:false \n" +
                "       testString2 max:50 \n" +
                "    }\n" +
                "}"))

        getDomainConfig(cl, [domainClass.getClazz()] as Class[])
        Map<String, ConstrainedProperty> mapping = new HibernateConstraintsEvaluator().evaluate(domainClass.getClazz(), domainClass.getProperties())
        ConstrainedProperty property1 = mapping.get("testString1")
        assertTrue("constraint was not nullable and should be", ((NullableConstraint)property1.getAppliedConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT)).isNullable())
        ConstrainedProperty property2 = mapping.get("testString2")
        assertFalse("constraint was nullable and shouldn't be", ((NullableConstraint)property2.getAppliedConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT)).isNullable())

    }

    private DefaultGrailsDomainConfiguration getDomainConfig(GroovyClassLoader cl, Class<?>[] classes) {
        GrailsApplication grailsApplication = new DefaultGrailsApplication(classes, cl)
        grailsApplication.initialise()
        DefaultGrailsDomainConfiguration config = new DefaultGrailsDomainConfiguration(grailsApplication: grailsApplication)
        config.buildMappings()
        config
    }
}
