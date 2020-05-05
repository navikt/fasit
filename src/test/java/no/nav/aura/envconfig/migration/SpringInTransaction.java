package no.nav.aura.envconfig.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public abstract class SpringInTransaction<R> {

    private static final Logger logger = LoggerFactory.getLogger(SpringInTransaction.class);

    private PlatformTransactionManager transactionManager;

    public SpringInTransaction(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public abstract R run();

    public R perform() {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            R r = run();
            transactionManager.commit(status);
            return r;
        } catch (Exception e) {
            try {
                transactionManager.rollback(status);
            } catch (IllegalStateException | SecurityException e1) {
                logger.error("Error rolling back", e1);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
