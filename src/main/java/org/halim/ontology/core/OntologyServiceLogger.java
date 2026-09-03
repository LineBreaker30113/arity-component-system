package org.halim.ontology.core;

/**
 * Minimal pluggable sink for the {@code LOGGING} cross-cutting concern
 * ({@link org.halim.ontology.core.segmentation.helper.BypassCrossCuttingConcerns.CrossCuttingConcern#LOGGING}).
 * Deliberately lightweight — no correlation IDs, breadcrumb files, or crash dumps; that's
 * {@code legacy.pd.CrashReporter}'s job, a separate diagnostics layer outside this package.
 */
public interface OntologyServiceLogger {

    void onInvoke(String serviceInterface, String methodName, Object[] args);
    void onSuccess(String serviceInterface, String methodName, Object result, long durationNanos);
    void onFailure(String serviceInterface, String methodName, Throwable failure, long durationNanos);

    OntologyServiceLogger NO_OP = new OntologyServiceLogger() {
        @Override public void onInvoke(String serviceInterface, String methodName, Object[] args) { }
        @Override public void onSuccess(String serviceInterface, String methodName, Object result, long durationNanos) { }
        @Override public void onFailure(String serviceInterface, String methodName, Throwable failure, long durationNanos) { }
    };

    static OntologyServiceLogger console() {
        return new OntologyServiceLogger() {
            @Override
            public void onInvoke(String serviceInterface, String methodName, Object[] args) {
                System.out.println("[ontology] -> " + serviceInterface + "#" + methodName);
            }

            @Override
            public void onSuccess(String serviceInterface, String methodName, Object result, long durationNanos) {
                System.out.println("[ontology] <- " + serviceInterface + "#" + methodName
                      + " (" + (durationNanos / 1_000) + "us)");
            }

            @Override
            public void onFailure(String serviceInterface, String methodName, Throwable failure, long durationNanos) {
                System.err.println("[ontology] !! " + serviceInterface + "#" + methodName
                      + " failed: " + failure);
            }
        };
    }
}
