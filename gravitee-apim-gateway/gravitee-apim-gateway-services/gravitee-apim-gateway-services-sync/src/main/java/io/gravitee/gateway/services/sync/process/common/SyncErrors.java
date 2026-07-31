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
package io.gravitee.gateway.services.sync.process.common;

import io.gravitee.repository.exceptions.TechnicalException;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Classification of synchronization failures shared by the repository and distributed sync paths.
 *
 * @author GraviteeSource Team
 */
public final class SyncErrors {

    private SyncErrors() {}

    /**
     * A failure is transient (retryable) when a repository {@link TechnicalException} appears anywhere in its
     * cause chain: the backing store was momentarily unreachable and a later attempt can succeed. Every other
     * failure is deterministic and would fail identically on retry, so it is treated as permanent.
     * <p>
     * The whole chain is scanned (not only the most-specific cause): a {@link TechnicalException} usually
     * wraps the underlying driver exception (Mongo/JDBC/...), so the innermost cause is not the one we test.
     */
    public static boolean isTransient(final Throwable throwable) {
        return ExceptionUtils.throwableOfType(throwable, TechnicalException.class) != null;
    }
}
