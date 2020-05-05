package no.nav.aura.gc.spring;

import javax.inject.Inject;

import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.util.Effect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Class to be able to use new transactions when using spring transactions with proxy.
 */
@Component
public class SpringTransactionalWrapper {

    private final static Logger log = LoggerFactory.getLogger(SpringTransactionalWrapper.class);

    private TransactionTemplate transactionTemplate;

    private boolean skipTransaction = false;

    private SpringTransactionalWrapper() {
    }

    public static SpringTransactionalWrapper mockWrapper() {
        SpringTransactionalWrapper wrapper = new SpringTransactionalWrapper();
        wrapper.skipTransaction = true;
        return wrapper;
    }

    @Inject
    public SpringTransactionalWrapper(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 
     * Do some things in database with a comment. This will run in a seperate transaction
     * 
     */
    @SuppressWarnings("serial")
    public void doInTransactionWithEntityComment(final String comment, final Effect effect) {
        if (skipTransaction) {
            log.warn("Running without extra transactions. This should only apply in tests");
            effect.apply(null);
            return;
        }
        EntityCommenter.applyComment(comment, new Effect() {
            @Override
            public void perform() {
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {

                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        effect.apply(null);
                    }
                });
            }
        });
    }
}
