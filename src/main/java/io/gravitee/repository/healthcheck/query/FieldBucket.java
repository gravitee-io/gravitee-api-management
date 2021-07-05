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
package io.gravitee.repository.healthcheck.query;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FieldBucket<T> {

    private final String name;

    private List<Bucket<T>> values;

    public FieldBucket(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Bucket<T>> getValues() {
        return values;
    }

    public void setValues(List<Bucket<T>> values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldBucket fieldBucket = (FieldBucket) o;

        return name.equals(fieldBucket.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
