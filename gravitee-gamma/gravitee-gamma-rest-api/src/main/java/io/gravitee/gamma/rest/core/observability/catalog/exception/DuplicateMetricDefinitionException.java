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
package io.gravitee.gamma.rest.core.observability.catalog.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * The environment already holds a record on this key; answered with {@code 400}. A type of its own so a write that
 * lost a creation race can tell it from the other rules and recover.
 *
 * @author GraviteeSource Team
 */
public class DuplicateMetricDefinitionException extends ValidationDomainException {

    public DuplicateMetricDefinitionException(String name) {
        super("'%s' is already declared in this environment".formatted(name), "observability.metric_definition.duplicate");
    }
}
