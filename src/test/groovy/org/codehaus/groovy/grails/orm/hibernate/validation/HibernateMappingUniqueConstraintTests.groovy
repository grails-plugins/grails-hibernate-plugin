package org.grails.orm.hibernate.validation

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * Checks UniqueConstraint basing on Hibernate mapped classes.
 *
 * @author Alexey Sergeev
 */
class HibernateMappingUniqueConstraintTests extends AbstractUniqueConstraintTests {

    protected Class getUserClass() { UserHibUnique }
    protected Class getLinkedUserClass() { LinkedUserHibUnique }

    protected void setUp() {
        super.setUp()

        // need to re-parse the constraints block since the first time the classes aren't registered in
        // the GrailsApplication, so the constraints are ignored. This is only needed here because
        // they're not defined like they would be in an application
        ga.getDomainClass(UserHibUnique.name).refreshConstraints()
        ga.getDomainClass(LinkedUserHibUnique.name).refreshConstraints()
    }

    @Override
    protected void configureDataSource() {
        gcl.parseClass('''
dataSource {
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
    dbCreate = "create-drop"
    url = "jdbc:h2:mem:grailsIntTestDB"
    properties {
      maxWait = 10000
    }
}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true
    cache.provider_class='net.sf.ehcache.hibernate.SingletonEhCacheProvider'
    config.location = ['classpath:/org/grails/orm/hibernate/validation/hibernate.cfg.xml']
}
''', "DataSource")
    }
}

@Entity
class UserHibUnique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    String login
    String grp
    String department
    String organization
    String code

    static constraints = {
        login(unique:['grp','department'])
        department(unique:"organization")
        code(unique:true)
    }
}

@Entity
class LinkedUserHibUnique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    UserHibUnique user1
    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    UserHibUnique user2

    static constraints = {
        user2(unique:'user1')
    }
}
