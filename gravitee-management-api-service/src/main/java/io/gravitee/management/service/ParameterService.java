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
package io.gravitee.management.service;

import io.gravitee.management.model.parameters.Key;
import io.gravitee.repository.management.model.Parameter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ParameterService {

    String find(Key key);
    boolean findAsBoolean(Key key);
    List<String> findAll(Key key);
    Map<String, List<String>> findAll(List<Key> keys);

    <T> List<T> findAll(Key key, Function<String, T> mapper);
    <T> Map<String, List<T>> findAll(List<Key> keys, Function<String, T> mapper);

    <T> List<T> findAll(Key key, Function<String, T> mapper, Predicate<String> filter);
    <T> Map<String, List<T>> findAll(List<Key> keys, Function<String, T> mapper, Predicate<String> filter);

    Parameter save(Key key, String value);
    Parameter save(Key key, List<String> value);
    Parameter save(Key key, Map<String, String> values);
}
