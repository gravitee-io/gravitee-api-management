/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.handlers.api.v4;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.CompletableTransformer;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * Handy class allowing to chain reactor steps easily.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class CompletableReactorChain extends Completable {

    private Completable completable;

    public CompletableReactorChain(Completable completable) {
        this.completable = completable;
    }

    /**
     * Simply chain the current {@link Completable} with the specified other {@link Completable} using {@link Completable#compose(CompletableTransformer)}.
     *
     * @param other the other {@link Completable} to compose with.
     * @return the current chain for fluent usage.
     */
    public CompletableReactorChain chainWith(Completable other) {
        this.completable = completable.compose(upstream -> upstream.andThen(other));
        return this;
    }

    /**
     * Same as {@link #chainWith(Completable)} but with a {@link CompletableTransformer} instead.
     *
     * @param transformer the {@link CompletableTransformer} to compose with.
     * @return the current chain for fluent usage.
     */
    public CompletableReactorChain chainWith(CompletableTransformer transformer) {
        this.completable = completable.compose(transformer);
        return this;
    }

    /**
     * Same as {@link #chainWith(Completable)} but with a condition to decide whether chain or not.
     *
     * @param other the other {@link Completable} to compose with.
     * @param condition <code>true</code> to chain, <code>false</code> to skip chaining.
     * @return the current chain for fluent usage.
     */
    public CompletableReactorChain chainWithIf(Completable other, boolean condition) {
        return condition ? chainWith(other) : this;
    }

    /**
     * Simply chain the current {@link Completable} with the specified {@link io.reactivex.rxjava3.functions.Function} using {@link Completable#onErrorResumeNext(io.reactivex.rxjava3.functions.Function)}.
     *
     * @param onError the {@link io.reactivex.rxjava3.functions.Function} to pass to @link Completable#onErrorResumeNext(io.reactivex.rxjava3.functions.Function)}.
     * @return the current chain for fluent usage.
     */
    public CompletableReactorChain chainWithOnError(io.reactivex.rxjava3.functions.Function<Throwable, Completable> onError) {
        completable = completable.onErrorResumeNext(onError);
        return this;
    }

    @Override
    protected void subscribeActual(@NonNull CompletableObserver observer) {
        completable.subscribe(observer);
    }
}
