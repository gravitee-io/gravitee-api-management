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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ParameterService {
    /**
     * Find parameter for current context
     */
    String find(ExecutionContext executionContext, Key key, ParameterReferenceType referenceType);

    /**
     * Find parameter for a specific context
     */
    String find(ExecutionContext executionContext, Key key, String referenceId, ParameterReferenceType referenceType);

    /**
     * Find parameter as boolean for current context
     */
    boolean findAsBoolean(ExecutionContext executionContext, Key key, ParameterReferenceType referenceType);

    /**
     * Find parameter as boolean for a specific context
     */
    boolean findAsBoolean(ExecutionContext executionContext, Key key, String referenceId, ParameterReferenceType referenceType);

    /**
     * Find parameter as list for current context
     */
    List<String> findAll(ExecutionContext executionContext, Key key, ParameterReferenceType referenceType);

    /**
     * Find parameter as list for a specific context
     */
    List<String> findAll(Key key, String referenceId, ParameterReferenceType referenceType, ExecutionContext executionContext);

    /**
     * Find list of parameters for current context
     */
    Map<String, List<String>> findAll(ExecutionContext executionContext, List<Key> keys, ParameterReferenceType referenceType);

    /**
     * Find list of parameters for a specific context
     */
    Map<String, List<String>> findAll(
        List<Key> keys,
        String referenceId,
        ParameterReferenceType referenceType,
        ExecutionContext executionContext
    );

    /**
     * Find parameter as list with mapper for current context
     */
    <T> List<T> findAll(ExecutionContext executionContext, Key key, Function<String, T> mapper, ParameterReferenceType referenceType);

    /**
     * Find parameter as list with mapper for a specific context
     */
    <T> List<T> findAll(
        ExecutionContext executionContext,
        Key key,
        Function<String, T> mapper,
        String referenceId,
        ParameterReferenceType referenceType
    );

    /**
     * Find list of parameters with mapper for current context
     */
    <T> Map<String, List<T>> findAll(
        ExecutionContext executionContext,
        List<Key> keys,
        Function<String, T> mapper,
        ParameterReferenceType referenceType
    );

    /**
     * Find list of parameters with mapper for a specific context
     */
    <T> Map<String, List<T>> findAll(
        ExecutionContext executionContext,
        List<Key> keys,
        Function<String, T> mapper,
        String referenceId,
        ParameterReferenceType referenceType
    );

    /**
     * Find parameter as list with filter and mapper for current context
     */
    <T> List<T> findAll(
        ExecutionContext executionContext,
        Key key,
        Function<String, T> mapper,
        Predicate<String> filter,
        ParameterReferenceType referenceType
    );

    /**
     * Find parameter as list with filter and mapper for a specific context
     */
    <T> List<T> findAll(
        ExecutionContext executionContext,
        Key key,
        Function<String, T> mapper,
        Predicate<String> filter,
        String referenceId,
        ParameterReferenceType referenceType
    );

    /**
     * Find list of parameters with filter and mapper for current context
     */
    <T> Map<String, List<T>> findAll(
        ExecutionContext executionContext,
        List<Key> keys,
        Function<String, T> mapper,
        Predicate<String> filter,
        ParameterReferenceType referenceType
    );

    /**
     * Find list of parameters with filter and mapper for a specific context
     */
    <T> Map<String, List<T>> findAll(
        ExecutionContext executionContext,
        List<Key> keys,
        Function<String, T> mapper,
        Predicate<String> filter,
        String referenceId,
        ParameterReferenceType referenceType
    );

    /**
     * Save parameter for current context
     */
    Parameter save(ExecutionContext executionContext, Key key, String value, ParameterReferenceType referenceType);

    /**
     * Save parameter for a specific context
     */
    Parameter save(ExecutionContext executionContext, Key key, String value, String referenceId, ParameterReferenceType referenceType);

    /**
     * Save parameter as list for current context
     */
    Parameter save(ExecutionContext executionContext, Key key, List<String> value, ParameterReferenceType referenceType);

    /**
     * Save parameter as list for a specific context
     */
    Parameter save(
        ExecutionContext executionContext,
        Key key,
        List<String> value,
        String referenceId,
        ParameterReferenceType referenceType
    );

    /**
     * Save parameter as map for current context
     */
    Parameter save(ExecutionContext executionContext, Key key, Map<String, String> values, ParameterReferenceType referenceType);

    /**
     * Save parameter as map for a specific context
     */
    Parameter save(
        ExecutionContext executionContext,
        Key key,
        Map<String, String> values,
        String referenceId,
        ParameterReferenceType referenceType
    );

    /**
     * Returns whether a parameter is set at the exact given scope, without cascading to the
     * organization or system scope. Used to distinguish an environment-level override from a value
     * inherited from a broader scope.
     * <p>
     * Unlike {@code find}/{@code save}/{@code findAll}, this method takes no {@link ExecutionContext}
     * and therefore does not resolve a {@code null} {@code referenceId} from the current context:
     * {@code referenceId} must be a concrete, non-null reference id.
     */
    boolean existsOnScope(Key key, String referenceId, ParameterReferenceType referenceType);

    /**
     * Deletes the parameter set at the exact given scope, so the value falls back to a broader scope
     * of the cascade. No-op when no parameter is set at that scope. Invalidates the parameter cache.
     * <p>
     * Like the {@code save(..., null)} delete path, this writes no audit entry and publishes no
     * {@link Key} change event. It must therefore not be used to reset keys whose runtime state is
     * driven by such events (e.g. SMTP/CORS) without adding that propagation separately.
     */
    void delete(ExecutionContext executionContext, Key key, String referenceId, ParameterReferenceType referenceType);

    /**
     * Invalidate cache for a specific parameter
     */
    void invalidateCache(String key, String referenceId, String referenceType);
}
