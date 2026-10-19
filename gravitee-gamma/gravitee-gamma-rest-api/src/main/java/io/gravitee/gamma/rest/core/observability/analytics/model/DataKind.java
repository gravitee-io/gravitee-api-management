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
package io.gravitee.gamma.rest.core.observability.analytics.model;

/**
 * The data an analytics query reads, which decides the APIs whose calls it may count.
 *
 * @author GraviteeSource Team
 */
public enum DataKind {
    /** Figures over many calls: the APIs whose analytics the caller reads. */
    AGGREGATES,
    /** Calls identifying their caller: the APIs whose logs the caller reads. */
    PER_REQUEST,
}
