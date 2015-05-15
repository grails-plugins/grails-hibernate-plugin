package org.grails.orm.hibernate

import grails.persistence.Entity

import java.sql.SQLException

import org.hibernate.Session
import org.springframework.orm.hibernate3.HibernateCallback
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager

// test for GRAILS-8108
class WithTransactionConnectionCleanupTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [AuthorLeak, BookLeak]
    }

    private void safeWithNewSession(Closure callable) {
        GrailsHibernateTemplate template = new GrailsHibernateTemplate(sessionFactory)
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
        if (sessionHolder) {
            // This is the only way to force hibernate to actually close the db connection in the new session
            TransactionSynchronizationManager.unbindResource(sessionFactory)
        }
        def sf = sessionFactory
        Session previousSession = sessionHolder?.session
        try {
            template.alwaysUseNewSession = true
            template.execute(new HibernateCallback() {
                def doInHibernate(Session session) throws SQLException {
                    TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session))
                    callable(session)
                }
            })
        }
        finally {
            // This is the only way to force hibernate to actually close the db connection in the new session
            TransactionSynchronizationManager.unbindResource(sf)
            if (sessionHolder) {
                TransactionSynchronizationManager.bindResource(sf, sessionHolder)
            }
        }
    }

    private void nestedWithNewSessionWithNewTransactionLoop(boolean unbindSessionHolder = false) {
        100.times { i ->
            def currentSession = session
            BookLeak.withNewSession { session ->
                assert currentSession  != session
                Long authorId = AuthorLeak.withTransaction {
                    AuthorLeak author = new AuthorLeak(firstName: 'John', lastName: "Doe ${i}")
                    author.save()
                    author.id
                }

//                AuthorLeak author = new AuthorLeak(firstName: 'John', lastName: "Doe ${i}")
//                author.save()
//                Long authorId =  author.id

                AuthorLeak another = AuthorLeak.get(authorId)
                BookLeak book = new BookLeak(title: "The story of ${i}")
                another.addToBooks(book)
                another.save()
            }
            if (unbindSessionHolder) {
                TransactionSynchronizationManager.unbindResource(sessionFactory)
            }
        }
    }

    private void nestedSafeWithNewSessionWithNewTransactionLoop(boolean withException = false) {
        100.times { i ->
            def currentSession = session
            safeWithNewSession { session ->
                assert currentSession  != session
                Long authorId = AuthorLeak.withTransaction {
                    AuthorLeak author = new AuthorLeak(firstName: 'John', lastName: "Doe ${i}")
                    author.save()
                    author.id
                }
                AuthorLeak another = AuthorLeak.get(authorId)
                BookLeak book = new BookLeak(title: "The story of ${i}")
                another.addToBooks(book)
                if (withException) {
                    throw new RuntimeException("Faking error (${i})")
                }
                another.save()
            }
        }
    }

    private void closeSession() {
        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory)
        if (sessionHolder) {
            TransactionSynchronizationManager.unbindResource(sessionFactory)
            SessionFactoryUtils.closeSession(sessionHolder.getSession())
        }
    }

    void testDbConnectionLeak() {
        nestedWithNewSessionWithNewTransactionLoop()
    }

    void testDbConnectionLeakSessionClosed() {
        closeSession()
        nestedWithNewSessionWithNewTransactionLoop()
    }

    void testNestedCustomWithNewSessionWithNewTransaction() {
        // Will succeed because using custom safeWithNewSession
        nestedSafeWithNewSessionWithNewTransactionLoop()
    }

    void testNestedCustomWithNewSessionWithNewTransactionSessionClosed() {
        // Will succeed because using custom safeWithNewSession
        // Must be run by itself because once the main session is closed, the other tests don't run correctly
        closeSession()
        nestedSafeWithNewSessionWithNewTransactionLoop()
    }

    void testExceptionInNestedCustomWithNewSessionWithNewTransaction() {
        shouldFail {
            nestedSafeWithNewSessionWithNewTransactionLoop(true)
        }
    }
}

@Entity
class BookLeak {
    String title
    static belongsTo = [author: AuthorLeak]
}

@Entity
class AuthorLeak {
    String firstName
    String lastName
    static hasMany = [books: BookLeak]
}
