package org.codehaus.groovy.grails.orm.hibernate.validation

import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.validation.exceptions.ConstraintException

/**
 * Checks UniqueConstraint using GORM-s.
 *
 * @author Alexey Sergeev
 */
class GORMUniqueConstraintTests extends AbstractUniqueConstraintTests {

    protected Class getUserClass() { UserGormUnique }
    protected Class getLinkedUserClass() { LinkedUserGormUnique }
    protected getDomainClasses() {
        [UserGormUnique, LinkedUserGormUnique]
    }

    void testValidatorBeanPresence() {
        assertTrue applicationContext.containsBean(getUserClass().name + "Validator")
        def validator = applicationContext.getBean(getUserClass().name + "Validator")
        assertNotNull(validator.domainClass)
        assertTrue applicationContext.containsBean(getLinkedUserClass().name + "Validator")
        validator = applicationContext.getBean(getLinkedUserClass().name + "Validator")
        assertNotNull(validator.domainClass)
    }

    void testWrongUniqueParams() {
        // Test argument with wrong type (Long)
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class userClass
        shouldFail(ConstraintException) {
            userClass = gcl.parseClass('''
            class User {
                Long id
                Long version

                String login
                String grp
                String department
                String code

                static constraints = {
                    login(unique:1L)
                }
            }
            ''')
            ga.addArtefact(DomainClassArtefactHandler.TYPE, userClass).constrainedProperties
        }

        // Test list argument with wrong type (Long)
        shouldFail(ConstraintException) {
            userClass = gcl.parseClass('''
            class User {
                Long id
                Long version

                String login
                String grp
                String department
                String code

                static constraints = {
                    login(unique:['grp',1L])
                }
            }
            ''')
            ga.addArtefact(DomainClassArtefactHandler.TYPE, userClass).constrainedProperties
        }

        // Test argument with non-existent property value
        shouldFail(ConstraintException) {
            userClass = gcl.parseClass('''
                class User {
                    Long id
                    Long version

                    String login
                    String grp
                    String department
                    String code

                    static constraints = {
                        login(unique:'test')
                    }
                }
                ''')
            ga.addArtefact(DomainClassArtefactHandler.TYPE, userClass).constrainedProperties
        }

        // Test list argument with non-existent property value
        shouldFail(ConstraintException) {
            userClass = gcl.parseClass('''
                class User {
                    Long id
                    Long version

                    String login
                    String grp
                    String department
                    String code

                    static constraints = {
                        login(unique:['grp','test'])
                    }
                }
                ''')
            ga.addArtefact(DomainClassArtefactHandler.TYPE, userClass).constrainedProperties
        }

        // Test that right syntax doesn't throws exception
        userClass = gcl.parseClass('''
            class User {
                Long id
                Long version

                String login
                String grp
                String department
                String code

                static constraints = {
                    login(unique:['grp'])
                }
            }
            ''')
        ga.addArtefact(DomainClassArtefactHandler.TYPE, userClass).constrainedProperties
    }
}

@Entity
class UserGormUnique {
    String login
    String grp
    String department
    String organization
    String code

    static belongsTo = LinkedUserGormUnique

    static constraints = {
        login(unique:['grp','department'])
        department(unique:"organization")
        code(unique:true)
    }
}

@Entity
class LinkedUserGormUnique {

    UserGormUnique user1
    UserGormUnique user2

    static constraints = {
        user2(unique:'user1')
    }
}
