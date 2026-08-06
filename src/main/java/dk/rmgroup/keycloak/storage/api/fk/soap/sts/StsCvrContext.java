package dk.rmgroup.keycloak.storage.api.fk.soap.sts;

import java.util.function.Supplier;

public final class StsCvrContext {

    private static final ThreadLocal<String> CURRENT_CVR = new ThreadLocal<>();

    private StsCvrContext() {
    }

    public static String getCurrentCvr() {
        return CURRENT_CVR.get();
    }

    public static void setCurrentCvr(String cvr) {
        if (cvr == null || cvr.isBlank()) {
            CURRENT_CVR.remove();
            return;
        }

        CURRENT_CVR.set(cvr);
    }

    public static void clear() {
        CURRENT_CVR.remove();
    }

    public static <T> T withCvr(String cvr, Supplier<T> supplier) {
        String previousCvr = CURRENT_CVR.get();
        try {
            setCurrentCvr(cvr);
            return supplier.get();
        } finally {
            if (previousCvr == null) {
                CURRENT_CVR.remove();
            } else {
                CURRENT_CVR.set(previousCvr);
            }
        }
    }

    public static void withCvr(String cvr, Runnable runnable) {
        withCvr(cvr, () -> {
            runnable.run();
            return null;
        });
    }
}