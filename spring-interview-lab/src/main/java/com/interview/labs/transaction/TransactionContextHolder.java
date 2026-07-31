package com.interview.labs.transaction;

// Hand-rolled illustration of the same technique Spring's own
// TransactionSynchronizationManager uses internally: bind a value to the
// current thread so it's available anywhere in the call stack without being
// passed as a parameter. Always pair set() with clear() — on a pooled-thread
// server (Tomcat) a forgotten remove() leaks into whatever request reuses the thread next.
public final class TransactionContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private TransactionContextHolder() {
    }

    public static void set(String correlationId) {
        CONTEXT.set(correlationId);
    }

    public static String get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
