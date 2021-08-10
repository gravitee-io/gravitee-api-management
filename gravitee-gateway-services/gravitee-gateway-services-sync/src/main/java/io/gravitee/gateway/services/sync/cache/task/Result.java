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
package io.gravitee.gateway.services.sync.cache.task;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class Result<T> {

    private final T value;
    private final Throwable throwable;

    public Result(T value) {
        this.value = value;
        this.throwable = null;
    }

    public Result(Throwable throwable) {
        this.throwable = throwable;
        this.value = null;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value);
    }

    public static <T> Result<T> failure(Throwable value) {
        return new Result<>(value);
    }

    public T result() {
        return value;
    }

    public Throwable cause() {
        return throwable;
    }

    public boolean succeeded() {
        return value != null;
    }

    public boolean failed() {
        return throwable != null;
    }
}
