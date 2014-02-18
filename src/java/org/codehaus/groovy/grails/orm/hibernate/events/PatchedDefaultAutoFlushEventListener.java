package org.codehaus.groovy.grails.orm.hibernate.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.event.EventSource;
import org.hibernate.event.def.DefaultAutoFlushEventListener;

/**
 * Patches Hibernate to prevent this issue
 * http://jira.grails.org/browse/GPHIB-19
 *
 * @author Aliaksandr Pyrkh
 */
public class PatchedDefaultAutoFlushEventListener extends DefaultAutoFlushEventListener {
    private static final long serialVersionUID = -7413770767669684078L;
    protected static final Log LOG = LogFactory.getLog(PatchedDefaultAutoFlushEventListener.class);

    @Override
    protected void performExecutions(EventSource session) throws HibernateException {
        try {
            session.getJDBCContext().getConnectionManager().flushBeginning();
            session.getPersistenceContext().setFlushing(true);
            // we need to lock the collection caches before
            // executing entity inserts/updates in order to
            // account for bidi associations
            session.getActionQueue().prepareActions();
            session.getActionQueue().executeActions();
        }
        catch (HibernateException he) {
            LOG.error("Could not synchronize database state with session", he);
            throw he;
        }
        finally {
            session.getPersistenceContext().setFlushing(false);
            session.getJDBCContext().getConnectionManager().flushEnding();
        }
    }
}
