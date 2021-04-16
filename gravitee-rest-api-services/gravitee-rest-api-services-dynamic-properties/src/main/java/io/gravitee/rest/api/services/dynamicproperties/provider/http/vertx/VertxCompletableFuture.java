/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.services.dynamicproperties.provider.http.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.*;

/**
 * An implementation of {@link CompletableFuture} for Vert.x. It differs in the way to handle async calls:
 * <p>
 * * {@link VertxCompletableFuture} are attached to a Vert.x {@link Context}
 * * All operator methods returns {@link VertxCompletableFuture}
 * * <em*async</em> method not passing an {@link Executor} are executed on the attached {@link Context}
 * * All non async method are executed on the current Thread (so not necessary on the attached {@link Context}
 * <p>
 * The class also offer bridges methods with Vert.x {@link Future}, and regular {@link CompletableFuture}.
 *
 * @param <T> the expected type of result
 */
@SuppressWarnings("WeakerAccess")
public class VertxCompletableFuture<T> extends CompletableFuture<T> implements CompletionStage<T> {

    private final Executor executor;

    /**
     * The {@link Context} used by the future.
     */
    private final Context context;

    // ============= Constructors =============

    /**
     * Creates an instance of {@link VertxCompletableFuture}, using the current Vert.x context or create a new one.
     *
     * @param vertx the Vert.x instance
     */
    public VertxCompletableFuture(Vertx vertx) {
        this(Objects.requireNonNull(vertx).getOrCreateContext());
    }

    /**
     * Creates an instance of {@link VertxCompletableFuture}, using the given {@link Context}.
     *
     * @param context the context
     */
    public VertxCompletableFuture(Context context) {
        this.context = Objects.requireNonNull(context);
        this.executor = command -> context.runOnContext(v -> command.run());
    }

    /**
     * Creates a new {@link VertxCompletableFuture} from the given context and given {@link CompletableFuture}.
     * The created {@link VertxCompletableFuture} is completed successfully or not when the given completable future
     * completes successfully or not.
     *
     * @param context the context
     * @param future  the completable future
     */
    private VertxCompletableFuture(Context context, CompletableFuture<T> future) {
        this(context);
        Objects
            .requireNonNull(future)
            .whenComplete(
                (res, err) -> {
                    if (err != null) {
                        completeExceptionally(err);
                    } else {
                        complete(res);
                    }
                }
            );
    }

    /**
     * Creates a new {@link VertxCompletableFuture} using the current {@link Context}. This method
     * <strong>must</strong> be used from a Vert.x thread, or fails.
     */
    public VertxCompletableFuture() {
        this(Vertx.currentContext());
    }

    // ============= Factory methods (from) =============

    /**
     * Creates a new {@link VertxCompletableFuture} from the given {@link Vertx} instance and given
     * {@link CompletableFuture}. The returned future uses the current Vert.x context, or creates a new one.
     * <p>
     * The created {@link VertxCompletableFuture} is completed successfully or not when the given completable future
     * completes successfully or not.
     *
     * @param vertx  the Vert.x instance
     * @param future the future
     * @param <T>    the type of the result
     * @return the new {@link VertxCompletableFuture}
     */
    public static <T> VertxCompletableFuture<T> from(Vertx vertx, CompletableFuture<T> future) {
        return from(vertx.getOrCreateContext(), future);
    }

    /**
     * Creates a new {@link VertxCompletableFuture} from the given {@link Context} instance and given
     * {@link Future}. The returned future uses the current Vert.x context, or creates a new one.
     * <p>
     * The created {@link VertxCompletableFuture} is completed successfully or not when the given future
     * completes successfully or not.
     *
     * @param vertx  the Vert.x instance
     * @param future the Vert.x future
     * @param <T>    the type of the result
     * @return the new {@link VertxCompletableFuture}
     */
    public static <T> VertxCompletableFuture<T> from(Vertx vertx, Future<T> future) {
        return from(vertx.getOrCreateContext(), future);
    }

    /**
     * Creates a {@link VertxCompletableFuture} from the given {@link Context} and {@link CompletableFuture}.
     * <p>
     * The created {@link VertxCompletableFuture} is completed successfully or not when the given future
     * completes successfully or not. The completion is called on the given {@link Context}, immediately if it is
     * already executing on the right context, asynchronously if not.
     *
     * @param context the context
     * @param future  the future
     * @param <T>     the type of result
     * @return the creation {@link VertxCompletableFuture}
     */
    public static <T> VertxCompletableFuture<T> from(Context context, CompletableFuture<T> future) {
        VertxCompletableFuture<T> res = new VertxCompletableFuture<>(Objects.requireNonNull(context));
        Objects
            .requireNonNull(future)
            .whenComplete(
                (result, error) -> {
                    if (context == Vertx.currentContext()) {
                        res.complete(result, error);
                    } else {
                        res.context.runOnContext(v -> res.complete(result, error));
                    }
                }
            );
        return res;
    }

    /**
     * Creates a new {@link VertxCompletableFuture} from the given {@link Context} instance and given
     * {@link Future}. The returned future uses the current Vert.x context, or creates a new one.
     * <p>
     * The created {@link VertxCompletableFuture} is completed successfully or not when the given future
     * completes successfully or not. The created {@link VertxCompletableFuture} is completed successfully or not
     * when the given future completes successfully or not. The completion is called on the given {@link Context},
     * immediately if it is already executing on the right context, asynchronously if not.
     *
     * @param context the context
     * @param future  the Vert.x future
     * @param <T>     the type of the result
     * @return the new {@link VertxCompletableFuture}
     */
    public static <T> VertxCompletableFuture<T> from(Context context, Future<T> future) {
        VertxCompletableFuture<T> res = new VertxCompletableFuture<>(Objects.requireNonNull(context));
        Objects
            .requireNonNull(future)
            .setHandler(
                ar -> {
                    if (context == Vertx.currentContext()) {
                        res.completeFromAsyncResult(ar);
                    } else {
                        res.context.runOnContext(v -> res.completeFromAsyncResult(ar));
                    }
                }
            );
        return res;
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a task running in the current Vert.x
     * {@link Context} with the value obtained by calling the given Supplier.
     * <p>
     * This method is different from {@link CompletableFuture#supplyAsync(Supplier)} as it does not use a fork join
     * executor, but use the Vert.x context.
     *
     * @param vertx    the Vert.x instance
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param <T>      the function's return type
     * @return the new CompletableFuture
     */
    public static <T> VertxCompletableFuture<T> supplyAsync(Vertx vertx, Supplier<T> supplier) {
        return supplyAsync(Objects.requireNonNull(vertx).getOrCreateContext(), supplier);
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a task running in the
     * current Vert.x {@link Context} after it runs the given action.
     * <p>
     * This method is different from {@link CompletableFuture#supplyAsync(Supplier)} as it does not use a fork join
     * executor, but use the Vert.x context.
     *
     * @param vertx    the Vert.x instance
     * @param runnable the action to run before completing the returned CompletableFuture
     * @return the new CompletableFuture
     */
    public static VertxCompletableFuture<Void> runAsync(Vertx vertx, Runnable runnable) {
        return runAsync(Objects.requireNonNull(vertx).getOrCreateContext(), runnable);
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a task running in the current Vert.x
     * {@link Context} with the value obtained by calling the given Supplier.
     * <p>
     * This method is different from {@link CompletableFuture#supplyAsync(Supplier)} as it does not use a fork join
     * executor, but use the Vert.x context.
     *
     * @param context  the context in which the supplier is executed.
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param <T>      the function's return type
     * @return the new CompletableFuture
     */
    public static <T> VertxCompletableFuture<T> supplyAsync(Context context, Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        VertxCompletableFuture<T> future = new VertxCompletableFuture<>(Objects.requireNonNull(context));
        context.runOnContext(
            v -> {
                try {
                    future.complete(supplier.get());
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }
        );
        return future;
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a task running in the
     * current Vert.x {@link Context} after it runs the given action.
     * <p>
     * This method is different from {@link CompletableFuture#runAsync(Runnable)} as it does not use a fork join
     * executor, but use the Vert.x context.
     *
     * @param context  the context
     * @param runnable the action to run before completing the returned CompletableFuture
     * @return the new CompletableFuture
     */
    public static VertxCompletableFuture<Void> runAsync(Context context, Runnable runnable) {
        Objects.requireNonNull(runnable);
        VertxCompletableFuture<Void> future = new VertxCompletableFuture<>(context);
        context.runOnContext(
            v -> {
                try {
                    runnable.run();
                    future.complete(null);
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }
        );
        return future;
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a task running in the worker thread pool of
     * Vert.x
     * <p>
     * This method is different from {@link CompletableFuture#supplyAsync(Supplier)} as it does not use a fork join
     * executor, but the worker thread pool.
     *
     * @param vertx    the Vert.x instance
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param <T>      the function's return type
     * @return the new CompletableFuture
     */
    public static <T> VertxCompletableFuture<T> supplyBlockingAsync(Vertx vertx, Supplier<T> supplier) {
        return supplyBlockingAsync(Objects.requireNonNull(vertx).getOrCreateContext(), supplier);
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a action running in the worker thread pool of
     * Vert.x
     * <p>
     * This method is different from {@link CompletableFuture#runAsync(Runnable)} as it does not use a fork join
     * executor, but the worker thread pool.
     *
     * @param vertx    the Vert.x instance
     * @param runnable the action, when its execution completes, it completes the returned CompletableFuture. If the
     *                 execution throws an exception, the returned CompletableFuture is completed exceptionally.
     * @return the new CompletableFuture
     */
    public static VertxCompletableFuture<Void> runBlockingAsync(Vertx vertx, Runnable runnable) {
        return runBlockingAsync(Objects.requireNonNull(vertx).getOrCreateContext(), runnable);
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a action running in the worker thread pool of
     * Vert.x
     * <p>
     * This method is different from {@link CompletableFuture#runAsync(Runnable)} as it does not use a fork join
     * executor, but the worker thread pool.
     *
     * @param context  the Vert.x context
     * @param runnable the action, when its execution completes, it completes the returned CompletableFuture. If the
     *                 execution throws an exception, the returned CompletableFuture is completed exceptionally.
     * @return the new CompletableFuture
     */
    public static VertxCompletableFuture<Void> runBlockingAsync(Context context, Runnable runnable) {
        Objects.requireNonNull(runnable);
        VertxCompletableFuture<Void> future = new VertxCompletableFuture<>(Objects.requireNonNull(context));
        context.executeBlocking(
            fut -> {
                try {
                    runnable.run();
                    future.complete(null);
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            },
            null
        );
        return future;
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a task running in the worker thread pool of
     * Vert.x
     * <p>
     * This method is different from {@link CompletableFuture#supplyAsync(Supplier)} as it does not use a fork join
     * executor, but the worker thread pool.
     *
     * @param context  the context in which the supplier is executed.
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param <T>      the function's return type
     * @return the new CompletableFuture
     */
    public static <T> VertxCompletableFuture<T> supplyBlockingAsync(Context context, Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        VertxCompletableFuture<T> future = new VertxCompletableFuture<>(context);
        context.<T>executeBlocking(
            fut -> {
                try {
                    fut.complete(supplier.get());
                } catch (Throwable e) {
                    fut.fail(e);
                }
            },
            ar -> {
                if (ar.failed()) {
                    future.completeExceptionally(ar.cause());
                } else {
                    future.complete(ar.result());
                }
            }
        );
        return future;
    }

    // ============= Wrapping methods =============

    /**
     * Creates a Vert.x {@link Future} from the given {@link CompletableFuture} (that can be a
     * {@link VertxCompletableFuture}).
     *
     * @param future the future
     * @param <T>    the type of the result
     * @return the Vert.x future completed or failed when the given {@link CompletableFuture} completes or fails.
     */
    public static <T> Future<T> toFuture(CompletableFuture<T> future) {
        Future<T> fut = Future.future();
        Objects
            .requireNonNull(future)
            .whenComplete(
                (res, err) -> {
                    if (err != null) {
                        fut.fail(err);
                    } else {
                        fut.complete(res);
                    }
                }
            );
        return fut;
    }

    // ============= Parallel composition methods =============

    /**
     * Returns a new CompletableFuture that is completed when all of the given CompletableFutures complete.  If any of
     * the given CompletableFutures complete exceptionally, then the returned CompletableFuture also does so, with a
     * CompletionException holding this exception as its cause.  Otherwise, the results, if any, of the given
     * CompletableFutures are not reflected in the returned CompletableFuture, but may be obtained by inspecting them
     * individually. If no CompletableFutures are provided, returns a CompletableFuture completed with the value
     * {@code null}.
     * <p>
     * <p>Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code CompletableFuture.allOf(c1, c2, c3).join();}.
     * <p>
     * Unlike the original {@link CompletableFuture#allOf(CompletableFuture[])} this method invokes the dependent
     * stages into the Vert.x context.
     *
     * @param vertx   the Vert.x instance to retrieve the context
     * @param futures the CompletableFutures
     * @return a new CompletableFuture that is completed when all of the given CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are {@code null}
     */
    public static VertxCompletableFuture<Void> allOf(Vertx vertx, CompletableFuture<?>... futures) {
        CompletableFuture<Void> all = CompletableFuture.allOf(futures);
        return VertxCompletableFuture.from(vertx, all);
    }

    /**
     * Returns a new CompletableFuture that is completed when all of the given CompletableFutures complete.  If any of
     * the given CompletableFutures complete exceptionally, then the returned CompletableFuture also does so, with a
     * CompletionException holding this exception as its cause.  Otherwise, the results, if any, of the given
     * CompletableFutures are not reflected in the returned CompletableFuture, but may be obtained by inspecting them
     * individually. If no CompletableFutures are provided, returns a CompletableFuture completed with the value
     * {@code null}.
     * <p>
     * <p>Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code CompletableFuture.allOf(c1, c2, c3).join();}.
     * <p>
     * Unlike the original {@link CompletableFuture#allOf(CompletableFuture[])} this method invokes the dependent
     * stages into the Vert.x context.
     *
     * @param context the context
     * @param futures the CompletableFutures
     * @return a new CompletableFuture that is completed when all of the given CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are {@code null}
     */
    public static VertxCompletableFuture<Void> allOf(Context context, CompletableFuture<?>... futures) {
        CompletableFuture<Void> all = CompletableFuture.allOf(futures);
        return VertxCompletableFuture.from(context, all);
    }

    /**
     * Returns a new CompletableFuture that is completed when any of the given CompletableFutures complete, with the
     * same result. Otherwise, if it completed exceptionally, the returned CompletableFuture also does so, with a
     * CompletionException holding this exception as its cause.  If no CompletableFutures are provided, returns an
     * incomplete CompletableFuture.
     * <p>
     * Unlike the original {@link CompletableFuture#allOf(CompletableFuture[])} this method invokes the dependent
     * stages into the Vert.x context.
     *
     * @param vertx   the Vert.x instance to retrieve the context
     * @param futures the CompletableFutures
     * @return a new CompletableFuture that is completed with the result or exception of any of the given
     * CompletableFutures when one completes
     * @throws NullPointerException if the array or any of its elements are {@code null}
     */
    public static VertxCompletableFuture<Object> anyOf(Vertx vertx, CompletableFuture<?>... futures) {
        CompletableFuture<Object> all = CompletableFuture.anyOf(futures);
        return VertxCompletableFuture.from(vertx, all);
    }

    /**
     * Returns a new CompletableFuture that is completed when any of the given CompletableFutures complete, with the
     * same result. Otherwise, if it completed exceptionally, the returned CompletableFuture also does so, with a
     * CompletionException holding this exception as its cause.  If no CompletableFutures are provided, returns an
     * incomplete CompletableFuture.
     * <p>
     * Unlike the original {@link CompletableFuture#allOf(CompletableFuture[])} this method invokes the dependent
     * stages into the Vert.x context.
     *
     * @param context the context
     * @param futures the CompletableFutures
     * @return a new CompletableFuture that is completed with the result or exception of any of the given
     * CompletableFutures when one completes
     * @throws NullPointerException if the array or any of its elements are {@code null}
     */
    public static VertxCompletableFuture<Object> anyOf(Context context, CompletableFuture<?>... futures) {
        CompletableFuture<Object> all = CompletableFuture.anyOf(futures);
        return VertxCompletableFuture.from(context, all);
    }

    // ============= with context methods =============

    /**
     * Creates a new {@link VertxCompletableFuture} using the current context. This method is used to switch between
     * Vert.x contexts.
     *
     * @return the created {@link VertxCompletableFuture}
     */
    public VertxCompletableFuture<T> withContext() {
        Context context = Objects.requireNonNull(Vertx.currentContext());
        return withContext(context);
    }

    /**
     * Creates a new {@link VertxCompletableFuture} using the current context or creates a new one. This method is used
     * to switch between Vert.x contexts.
     *
     * @return the created {@link VertxCompletableFuture}
     */
    public VertxCompletableFuture<T> withContext(Vertx vertx) {
        return withContext(Objects.requireNonNull(vertx).getOrCreateContext());
    }

    /**
     * Creates a new {@link VertxCompletableFuture} using the given context. This method is used to switch between
     * Vert.x contexts.
     *
     * @return the created {@link VertxCompletableFuture}
     */
    public VertxCompletableFuture<T> withContext(Context context) {
        VertxCompletableFuture<T> future = new VertxCompletableFuture<>(Objects.requireNonNull(context));
        whenComplete(
            (res, err) -> {
                if (err != null) {
                    future.completeExceptionally(err);
                } else {
                    future.complete(res);
                }
            }
        );
        return future;
    }

    /**
     * @return the context associated with the current {@link VertxCompletableFuture}.
     */
    public Context context() {
        return context;
    }

    // ============= Composite Future implementation =============

    @Override
    public <U> VertxCompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return new VertxCompletableFuture<>(context, super.thenApply(fn));
    }

    @Override
    public <U> VertxCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return new VertxCompletableFuture<>(context, super.thenApplyAsync(fn, executor));
    }

    @Override
    public VertxCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return new VertxCompletableFuture<>(context, super.thenAcceptAsync(action, executor));
    }

    @Override
    public VertxCompletableFuture<Void> thenRun(Runnable action) {
        return new VertxCompletableFuture<>(context, super.thenRun(action));
    }

    @Override
    public VertxCompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return new VertxCompletableFuture<>(context, super.thenRunAsync(action, executor));
    }

    @Override
    public <U, V> VertxCompletableFuture<V> thenCombine(
        CompletionStage<? extends U> other,
        BiFunction<? super T, ? super U, ? extends V> fn
    ) {
        return new VertxCompletableFuture<>(context, super.thenCombine(other, fn));
    }

    @Override
    public <U> VertxCompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return new VertxCompletableFuture<>(context, super.thenAcceptBoth(other, action));
    }

    @Override
    public <U> VertxCompletableFuture<Void> thenAcceptBothAsync(
        CompletionStage<? extends U> other,
        BiConsumer<? super T, ? super U> action,
        Executor executor
    ) {
        return new VertxCompletableFuture<>(context, super.thenAcceptBothAsync(other, action, executor));
    }

    @Override
    public VertxCompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return new VertxCompletableFuture<>(context, super.runAfterBoth(other, action));
    }

    @Override
    public VertxCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return new VertxCompletableFuture<>(context, super.runAfterBothAsync(other, action, executor));
    }

    @Override
    public <U> VertxCompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return new VertxCompletableFuture<>(context, super.applyToEither(other, fn));
    }

    @Override
    public <U> VertxCompletableFuture<U> applyToEitherAsync(
        CompletionStage<? extends T> other,
        Function<? super T, U> fn,
        Executor executor
    ) {
        return new VertxCompletableFuture<>(context, super.applyToEitherAsync(other, fn, executor));
    }

    @Override
    public VertxCompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return new VertxCompletableFuture<>(context, super.acceptEither(other, action));
    }

    @Override
    public VertxCompletableFuture<Void> acceptEitherAsync(
        CompletionStage<? extends T> other,
        Consumer<? super T> action,
        Executor executor
    ) {
        return new VertxCompletableFuture<>(context, super.acceptEitherAsync(other, action, executor));
    }

    @Override
    public VertxCompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return new VertxCompletableFuture<>(context, super.runAfterEither(other, action));
    }

    @Override
    public VertxCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return new VertxCompletableFuture<>(context, super.runAfterEitherAsync(other, action, executor));
    }

    @Override
    public <U> VertxCompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return new VertxCompletableFuture<>(context, super.thenCompose(fn));
    }

    @Override
    public VertxCompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return new VertxCompletableFuture<>(context, super.whenComplete(action));
    }

    @Override
    public VertxCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return new VertxCompletableFuture<>(context, super.whenCompleteAsync(action, executor));
    }

    @Override
    public <U> VertxCompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return new VertxCompletableFuture<>(context, super.handle(fn));
    }

    @Override
    public <U> VertxCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return new VertxCompletableFuture<>(context, super.handleAsync(fn, executor));
    }

    @Override
    public <U> VertxCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return new VertxCompletableFuture<>(context, super.thenApplyAsync(fn, executor));
    }

    @Override
    public VertxCompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return new VertxCompletableFuture<>(context, super.thenAccept(action));
    }

    @Override
    public VertxCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return new VertxCompletableFuture<>(context, super.thenAcceptAsync(action, executor));
    }

    @Override
    public VertxCompletableFuture<Void> thenRunAsync(Runnable action) {
        return new VertxCompletableFuture<>(context, super.thenRunAsync(action, executor));
    }

    @Override
    public <U, V> VertxCompletableFuture<V> thenCombineAsync(
        CompletionStage<? extends U> other,
        BiFunction<? super T, ? super U, ? extends V> fn
    ) {
        return new VertxCompletableFuture<>(context, super.thenCombineAsync(other, fn, executor));
    }

    @Override
    public <U> VertxCompletableFuture<Void> thenAcceptBothAsync(
        CompletionStage<? extends U> other,
        BiConsumer<? super T, ? super U> action
    ) {
        return new VertxCompletableFuture<>(context, super.thenAcceptBothAsync(other, action, executor));
    }

    @Override
    public VertxCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return new VertxCompletableFuture<>(context, super.runAfterBothAsync(other, action, executor));
    }

    @Override
    public <U> VertxCompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return new VertxCompletableFuture<>(context, super.applyToEitherAsync(other, fn, executor));
    }

    @Override
    public VertxCompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return new VertxCompletableFuture<>(context, super.acceptEitherAsync(other, action, executor));
    }

    @Override
    public VertxCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return new VertxCompletableFuture<>(context, super.runAfterEitherAsync(other, action, executor));
    }

    @Override
    public <U> VertxCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return new VertxCompletableFuture<>(context, super.thenComposeAsync(fn, executor));
    }

    @Override
    public <U> VertxCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return new VertxCompletableFuture<>(context, super.thenComposeAsync(fn, executor));
    }

    public <U, V> VertxCompletableFuture<V> thenCombineAsync(
        CompletionStage<? extends U> other,
        BiFunction<? super T, ? super U, ? extends V> fn,
        Executor executor
    ) {
        return new VertxCompletableFuture<>(context, super.thenCombineAsync(other, fn, executor));
    }

    @Override
    public VertxCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return new VertxCompletableFuture<>(context, super.whenCompleteAsync(action, executor));
    }

    @Override
    public <U> VertxCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return new VertxCompletableFuture<>(context, super.handleAsync(fn, executor));
    }

    @Override
    public VertxCompletableFuture<T> toCompletableFuture() {
        return this;
    }

    // ============= other instance methods =============

    /**
     * Creates a new {@link Future} object completed / failed when the current {@link VertxCompletableFuture} is
     * completed successfully or not.
     *
     * @return the {@link Future}.
     */
    public Future<T> toFuture() {
        return VertxCompletableFuture.toFuture(this);
    }

    private void complete(T result, Throwable error) {
        if (error == null) {
            super.complete(result);
        } else {
            super.completeExceptionally(error);
        }
    }

    private void completeFromAsyncResult(AsyncResult<T> ar) {
        if (ar.succeeded()) {
            super.complete(ar.result());
        } else {
            super.completeExceptionally(ar.cause());
        }
    }
}
