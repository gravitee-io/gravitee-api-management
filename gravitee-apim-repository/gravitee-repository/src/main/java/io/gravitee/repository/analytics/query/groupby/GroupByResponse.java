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
package io.gravitee.repository.analytics.query.groupby;

import io.gravitee.repository.analytics.query.response.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupByResponse implements Response {

    private String field;

    private final List<Bucket> values = new ArrayList<>();

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public List<Bucket> getValues() {
        return values;
    }

    public List<Bucket> values() {
        return values;
    }

    public static class Bucket {
        private final String name;
        private final long value;

        public Bucket(String name, long value) {
            this.name = name;
            this.value = value;
        }

        public String name() {
            return name;
        }

        public long value() {
            return value;
        }
    }
}
