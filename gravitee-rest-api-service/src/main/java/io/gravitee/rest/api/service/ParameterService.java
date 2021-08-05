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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
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
    String find(Key key, ParameterReferenceType referenceType);

    /**
     * Find parameter for a specific context
     */
    String find(Key key, String referenceId, ParameterReferenceType referenceType);

    /**
     * Find parameter as boolean for current context
     */
    boolean findAsBoolean(Key key, ParameterReferenceType referenceType);

    /**
     * Find parameter as boolean for a specific context
     */
    boolean findAsBoolean(Key key, String referenceId, ParameterReferenceType referenceType);

    /**
     * Find parameter as list for current context
     */
    List<String> findAll(Key key, ParameterReferenceType referenceType);

    /**
     * Find parameter as list for a specific context
     */
    List<String> findAll(Key key, String referenceId, ParameterReferenceType referenceType);

    /**
     * Find list of parameters for current context
     */
    Map<String, List<String>> findAll(List<Key> keys, ParameterReferenceType referenceType);

    /**
     * Find list of parameters for a specific context
     */
    Map<String, List<String>> findAll(List<Key> keys, String referenceId, ParameterReferenceType referenceType);

    /**
     * Find parameter as list with mapper for current context
     */
    <T> List<T> findAll(Key key, Function<String, T> mapper, ParameterReferenceType referenceType);

    /**
     * Find parameter as list with mapper for a specific context
     */
    <T> List<T> findAll(Key key, Function<String, T> mapper, String referenceId, ParameterReferenceType referenceType);

    /**
     * Find list of parameters with mapper for current context
     */
    <T> Map<String, List<T>> findAll(List<Key> keys, Function<String, T> mapper, ParameterReferenceType referenceType);

    /**
     * Find list of parameters with mapper for a specific context
     */
    <T> Map<String, List<T>> findAll(List<Key> keys, Function<String, T> mapper, String referenceId, ParameterReferenceType referenceType);

    /**
     * Find parameter as list with filter and mapper for current context
     */
    <T> List<T> findAll(Key key, Function<String, T> mapper, Predicate<String> filter, ParameterReferenceType referenceType);

    /**
     * Find parameter as list with filter and mapper for a specific context
     */
    <T> List<T> findAll(
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
        List<Key> keys,
        Function<String, T> mapper,
        Predicate<String> filter,
        ParameterReferenceType referenceType
    );

    /**
     * Find list of parameters with filter and mapper for a specific context
     */
    <T> Map<String, List<T>> findAll(
        List<Key> keys,
        Function<String, T> mapper,
        Predicate<String> filter,
        String referenceId,
        ParameterReferenceType referenceType
    );

    /**
     * Save parameter for current context
     */
    Parameter save(Key key, String value, ParameterReferenceType referenceType);

    /**
     * Save parameter for a specific context
     */
    Parameter save(Key key, String value, String referenceId, ParameterReferenceType referenceType);

    /**
     * Save parameter as list for current context
     */
    Parameter save(Key key, List<String> value, ParameterReferenceType referenceType);

    /**
     * Save parameter as list for a specific context
     */
    Parameter save(Key key, List<String> value, String referenceId, ParameterReferenceType referenceType);

    /**
     * Save parameter as map for current context
     */
    Parameter save(Key key, Map<String, String> values, ParameterReferenceType referenceType);

    /**
     * Save parameter as map for a specific context
     */
    Parameter save(Key key, Map<String, String> values, String referenceId, ParameterReferenceType referenceType);
}
