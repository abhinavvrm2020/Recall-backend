package com.quizapp.common.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class Futures {
    private Futures() {}

    public static <T> CompletableFuture<T> supply(Supplier<T> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public static CompletableFuture<Void> run(Runnable runnable, Executor executor) {
        return CompletableFuture.runAsync(runnable, executor);
    }

    @SafeVarargs
    public static CompletableFuture<Void> all(CompletableFuture<?>... futures) {
        return CompletableFuture.allOf(futures);
    }

    public static <T> List<T> joinAll(List<CompletableFuture<T>> futures) {
        return futures.stream().map(CompletableFuture::join).toList();
    }
}
