package no.nav.aura.envconfig.model.deletion;

public enum LifeCycleStatus {
    ALERTED, STOPPED, RUNNING, RESCUED;

    public static boolean isDeleteCandidate(LifeCycleStatus status) {
        return STOPPED == status || ALERTED == status;
    }

}