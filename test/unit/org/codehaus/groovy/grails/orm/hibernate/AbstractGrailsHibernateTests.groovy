package org.codehaus.groovy.grails.orm.hibernate

import grails.plugin.hibernate3.HibernatePluginSupport
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil
import grails.util.GrailsWebUtil
import grails.util.Metadata

import org.springframework.mock.web.MockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.hibernate.EntityMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.metadata.ClassMetadata
import org.springframework.context.ApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockServletContext
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.Log4jConfigurer
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractGrailsHibernateTests extends GroovyTestCase {

    GroovyClassLoader gcl = new GroovyClassLoader(getClass().classLoader)
    GrailsApplication ga
    GrailsApplication grailsApplication
    GrailsPluginManager mockManager
    MockApplicationContext ctx
    ApplicationContext appCtx
    ApplicationContext applicationContext
    MockHttpServletRequest request
    def originalHandler
    SessionFactory sessionFactory
    Session session
    GrailsDomainBinder grailsDomainBinder = new GrailsDomainBinder()

    protected void onSetUp() {}

    protected void setUp() {
        super.setUp()

        ConfigurationHelper.clearCachedConfigs()

        Log4jConfigurer.initLogging("test/unit/log4j.properties")

        ExpandoMetaClass.enableGlobally()

        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()

        configureDataSource()

        ctx = new MockApplicationContext()

        onSetUp()

        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl)
        ga.metadata[Metadata.APPLICATION_NAME] = getClass().name
        grailsApplication = ga

        def dependentPlugins = configurePlugins()

        afterPluginInitialization()

        initializeApplication()

        ctx.registerMockBean("messageSource", new StaticMessageSource())

        def springConfig = new WebRuntimeSpringConfiguration(ctx, gcl)

        def servletContext = new MockServletContext()
        springConfig.servletContext = servletContext

        doWithRuntimeConfiguration dependentPlugins, springConfig

        ga.setMainContext(springConfig.getUnrefreshedApplicationContext())
        appCtx = springConfig.getApplicationContext()

        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

        applicationContext = appCtx
        ga.setMainContext(appCtx)
        dependentPlugins*.doWithApplicationContext(appCtx)

        mockManager.applicationContext = appCtx
        mockManager.doDynamicMethods()

        registerHibernateSession()

        request = new GrailsMockHttpServletRequest(characterEncoding: "utf-8")
        def response = new GrailsMockHttpServletResponse()
        GrailsWebUtil.bindMockWebRequest(appCtx, request, response)
    }

     protected void configureDataSource() {
        gcl.parseClass('''
dataSource {
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
    dbCreate = "create-drop"
    url = "jdbc:h2:mem:grailsIntTestDB;MVCC=TRUE;LOCK_TIMEOUT=10000"
    properties {
       initialSize = 5
       maxActive = 50
       minIdle = 5
       maxIdle = 25
       maxWait = 10000
       maxAge = 10 * 60000
       timeBetweenEvictionRunsMillis = 5000
       minEvictableIdleTimeMillis = 60000
       validationQuery = "SELECT 1"
       validationQueryTimeout = 3
       validationInterval = 15000
       testOnBorrow = true
       testWhileIdle = true
       testOnReturn = false
       jdbcInterceptors = "ConnectionState;StatementCache(max=200)"
       defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
    }
}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true
    cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
}
''', "DataSource")
    }

    protected setCurrentController(controller) {
        RequestContextHolder.requestAttributes.controllerName = GrailsNameUtils.getLogicalName(
            controller.class.name, "Controller")
    }

    void onApplicationCreated() {}

    /**
     * Subclasses may override this method to return a list of classes which should
     * be added to the GrailsApplication as domain classes
     *
     * @return a list of classes
     */
    protected getDomainClasses() {
        Collections.EMPTY_LIST
    }

    protected void doWithRuntimeConfiguration(dependentPlugins, springConfig) {
        dependentPlugins*.doWithRuntimeConfiguration(springConfig)
     }

    GrailsWebRequest buildMockRequest(ConfigObject config = null) throws Exception {
        if (config != null) {
            ga.config = config
        }
        return GrailsWebUtil.bindMockWebRequest(appCtx)
    }

    protected void tearDown() {

        unbindSessionFactory sessionFactory
        sessionFactory = null

        GroovySystem.stopThreadedReferenceManager()

        try {
            TransactionSynchronizationManager.clear()
        }
        catch(e) {
            // means it is not active, ignore
        }
        try {
            getClass().classLoader.loadClass("net.sf.ehcache.CacheManager")
                                    .getInstance()?.shutdown()
        }
        catch(e) {
            // means there is no cache, ignore
        }
        gcl = null
        ga = null
        mockManager = null
        appCtx.close()
        ctx = null
        appCtx = null

        ExpandoMetaClass.disableGlobally()
        RequestContextHolder.setRequestAttributes(null)
        PluginManagerHolder.setPluginManager(null)

        originalHandler = null

        onTearDown()

        super.tearDown()
    }

    protected void unregisterHibernateSession() {
        unbindSessionFactory sessionFactory
    }

    protected void unbindSessionFactory(SessionFactory sessionFactory) {
        if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
            Session s = TransactionSynchronizationManager.getResource(sessionFactory).session
            TransactionSynchronizationManager.unbindResource sessionFactory
            SessionFactoryUtils.releaseSession s, sessionFactory
        }

        for (ClassMetadata metadata in sessionFactory.allClassMetadata.values()) {
            GroovySystem.getMetaClassRegistry().removeMetaClass metadata.getMappedClass(EntityMode.POJO)
        }
    }

    protected MockApplicationContext createMockApplicationContext() {
        return new MockApplicationContext()
    }

    protected Resource[] getResources(String pattern) throws IOException {
        return new PathMatchingResourcePatternResolver().getResources(pattern)
    }

    protected void onTearDown() {
    }

    protected List configurePlugins() {
        mockManager = new MockGrailsPluginManager(ga)

        ctx.registerMockBean("pluginManager", mockManager)
        PluginManagerHolder.setPluginManager(mockManager)

        def dependantPluginClasses = []
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin")
        dependantPluginClasses << MockHibernateGrailsPlugin

        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga) }

        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration()
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager())

        dependentPlugins
    }

    protected void afterPluginInitialization() {
    }

    protected void initializeApplication() {
        ga.initialise()
        onApplicationCreated()
        domainClasses?.each { dc -> ga.addArtefact 'Domain', dc }
        ga.setApplicationContext(ctx)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
    }

    protected void registerHibernateSession() {
        sessionFactory = appCtx.getBean(GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN)
        bindSessionFactory sessionFactory
        session = sessionFactory.currentSession
    }

    protected void bindSessionFactory(SessionFactory sessionFactory) {
        if (!TransactionSynchronizationManager.hasResource(sessionFactory)) {
            TransactionSynchronizationManager.bindResource(sessionFactory,
                new SessionHolder(sessionFactory.openSession()))
        }
    }
}

class MockHibernateGrailsPlugin {
    def version = GrailsUtil.grailsVersion
    def artefacts = [new AnnotationDomainClassArtefactHandler()]
    def loadAfter = ['controllers', 'domainClass']
    def doWithSpring = HibernatePluginSupport.doWithSpring
    def doWithDynamicMethods = HibernatePluginSupport.doWithDynamicMethods
    def doWithApplicationContext = HibernatePluginSupport.doWithApplicationContext
    def onChange = HibernatePluginSupport.onChange
}
