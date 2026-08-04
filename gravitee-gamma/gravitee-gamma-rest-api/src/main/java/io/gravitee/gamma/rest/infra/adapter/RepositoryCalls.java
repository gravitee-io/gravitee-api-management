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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;

/**
 * Shared boundary helper for adapters fronting a platform repository SPI: turns the SPI's checked
 * {@link TechnicalException} into the unchecked {@link TechnicalDomainException} the core layer
 * expects, preserving the cause and attaching a caller-supplied context message.
 *
 * <p>Lives here rather than inside a single adapter so every {@code port/repository/}-backed adapter
 * maps SPI failures the same way — the alternative is each adapter repeating the same try/catch per
 * method.
 *
 * @author GraviteeSource Team
 */
public final class RepositoryCalls {

    private RepositoryCalls() {}

    /**
     * Runs a repository call, wrapping a {@link TechnicalException} into a
     * {@link TechnicalDomainException} described by {@code message}.
     */
    public static <T> T wrap(RepositoryCall<T> call, String message) {
        try {
            return call.execute();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(message, e);
        }
    }

    /**
     * Lets {@link #wrap} take a lambda that still declares the SPI's checked exception — a plain
     * {@link java.util.function.Supplier} could not, which would force the {@code try}/{@code catch}
     * back into every adapter method. Callers pass the repository call itself; translating the
     * failure is {@link #wrap}'s job, so implementations should let {@link TechnicalException}
     * propagate rather than handling it.
     */
    @FunctionalInterface
    public interface RepositoryCall<T> {
        T execute() throws TechnicalException;
    }
}
