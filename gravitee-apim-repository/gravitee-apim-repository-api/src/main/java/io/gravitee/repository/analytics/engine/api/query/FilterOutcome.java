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
package io.gravitee.repository.analytics.engine.api.query;

/** What a search path does with a top-level filter condition. */
public enum FilterOutcome {
    /** The condition restricts the result. */
    APPLIED,
    /** The path cannot carry the condition and skips it: the result is not filtered on that field. */
    IGNORED,
    /** The path cannot carry the condition and answers nothing at all. */
    EMPTIES,
}
