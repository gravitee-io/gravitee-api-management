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
package io.gravitee.repository.analytics.query;

import java.util.Set;

public class TermsFilter {

    private final String field;

    private final Set<String> values;

    public TermsFilter(String filter, Set<String> values) {
        this.field = filter;
        this.values = values;
    }

    public String field() {
        return this.field;
    }

    public Set<String> values() {
        return this.values;
    }
}
