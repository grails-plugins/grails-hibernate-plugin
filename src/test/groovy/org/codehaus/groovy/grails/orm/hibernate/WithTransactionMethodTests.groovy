package org.grails.orm.hibernate

import grails.test.mixin.*

class WithTransactionMethodTests extends AbstractGrailsHibernateTests {

    void testWithTransactionMethod() {
        doTestWithTransactionMethod(ga.getDomainClass("Author1"))
    }

    void testWithTransactionMethodAndHiloGenerator() {
        doTestWithTransactionMethod(ga.getDomainClass("Author2"))
    }
    
    private doTestWithTransactionMethod(domainClass) {
        def authors = []
        5.times {
            authors << domainClass.newInstance()
        }

        authors[0].name = "Stephen King"
        authors[1].name = "John Grisham"
        authors[2].name = "James Patterson"
        authors[3].name = "Ernest Hemingway"
        authors[4].name = "Mika Waltari"

        domainClass.clazz.withTransaction { status ->
            authors[0].save()
            authors[1].save()
        }

        def results = domainClass.clazz.list()
        assertEquals 2, results.size()

        domainClass.clazz.withTransaction { status ->
            authors[2].save(flush: true)
            domainClass.clazz.withNewSession { 
                authors[3].save(flush: true)
            }
            domainClass.clazz.withNewTransaction { status2 ->
                assertEquals 2, domainClass.clazz.list().size()
                authors[4].save(flush: true)
                status2.setRollbackOnly()
            }
            assertEquals 4, domainClass.clazz.list().size()
            status.setRollbackOnly()
        }

        results = domainClass.clazz.list()
        assertEquals 2, results.size()
    }

    void onSetUp() {
        gcl.parseClass """
class Book1 {
    Long id
    Long version
    static belongsTo = Author1
    Author1 author
    String title
    boolean equals(obj) { title == obj?.title }
    int hashCode() { title ? title.hashCode() : super.hashCode() }
    String toString() { title }
}

class Author1 {
    Long id
    Long version
    String name
    Set books
    static hasMany = [books:Book1]
    boolean equals(obj) { name == obj?.name }
    int hashCode() { name ? name.hashCode() : super.hashCode() }
    String toString() { name }
}

class Book2 {
    static mapping = { id generator: 'org.hibernate.id.enhanced.TableGenerator', params: [optimizer: 'hilo', increment_size: 1, segment_value: 'book'] }
    Long id
    Long version
    static belongsTo = Author2
    Author2 author
    String title
    boolean equals(obj) { title == obj?.title }
    int hashCode() { title ? title.hashCode() : super.hashCode() }
    String toString() { title }
}

class Author2 {
    static mapping = { id generator: 'org.hibernate.id.enhanced.TableGenerator', params: [optimizer: 'hilo', increment_size: 1, segment_value: 'author'] }
    Long id
    Long version
    String name
    Set books
    static hasMany = [books:Book2]
    boolean equals(obj) { name == obj?.name }
    int hashCode() { name ? name.hashCode() : super.hashCode() }
    String toString() { name }
}
"""
    }
}
