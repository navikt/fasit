package no.nav.aura.envconfig.auditing;

import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.util.Effect;

public final class EntityCommenter {

    private static ThreadLocal<String> currentComment = new ThreadLocal<>();
    private static ThreadLocal<NavUser> onBehalfOfUser = new ThreadLocal<>();

    private EntityCommenter() {
    }

    @Deprecated
    public static void setComment(String comment) {
        currentComment.set(comment);
    }

    public static void applyComment(String comment, Effect effect) {
        applyComment(comment, null, effect);
    }

    public static void applyComment(String comment, NavUser onBehalfOf, Effect effect) {
        currentComment.set(comment);
        if (onBehalfOf != null) {
            onBehalfOfUser.set(onBehalfOf);
        }
        try {
            effect.perform();
        } finally {
            currentComment.remove();
            onBehalfOfUser.remove();
        }
    }

    public static String getComment() {
        return currentComment.get();
    }

    public static NavUser getOnBehalfOfUser() {
        return onBehalfOfUser.get();
    }

    public static String getOnBehalfUserOrRealUser(ApplicationInstance appInstance) {
        NavUser onBehalfOfUser = EntityCommenter.getOnBehalfOfUser();
        return onBehalfOfUser != null ? onBehalfOfUser.getDisplayName() : appInstance.getUpdatedBy();
    }
}
