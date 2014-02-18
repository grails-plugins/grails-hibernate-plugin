package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class AssertionFailureInEventTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [AssertionParent, AssertionChild, AssertionSubChild]
    }

    // test for HHH-2763 and GRAILS-4453
    void testNoAssertionErrorInEventDuringFlush() {
        def Parent = ga.getDomainClass(AssertionParent.name).clazz
        def Child = ga.getDomainClass(AssertionChild.name).clazz
        def p = Parent.newInstance(s:"parent").save()
        p.addToChilds(Child.newInstance(s:"one"))
        p.save(flush:true)

        session.clear()

        p = Parent.findById(1, [fetch:[childs:'join']])

        p.addToChilds(Child.newInstance(s:"two"))
        p.save(flush:true)

        session.clear()

        p = Parent.get(1)
        p.childs.each { println it.s }
    }

    // test for GPHIB-19
    void testNoAssertionErrorInEventDuringAutoflush() {
        def Parent = ga.getDomainClass(AssertionParent.name).clazz
        def Child = ga.getDomainClass(AssertionChild.name).clazz
        def p = Parent.newInstance(s: 'parent').save()
        p.addToChilds(Child.newInstance(s: 'one'))
        p.addToChilds(Child.newInstance(s: 'two'))
        p.save(flush: true)

        Parent.withNewSession {
            Parent.withTransaction {
                p = Parent.get(1)
                p.s = 'chanched'
                Parent.list()
            }
        }
    }
}

@Entity
class AssertionParent {
    static hasMany = [childs : AssertionChild]
    String s

    def beforeUpdate = {
        calc()
    }

    def calc = {
        childs.each { it.s = "change" }
    }
}

@Entity
class AssertionChild {
    static belongsTo = [AssertionParent]
    static hasMany = [ subChilds : AssertionSubChild ]
    String s
}

@Entity
class AssertionSubChild {
    static belongsTo = [AssertionChild]
    String s
}
