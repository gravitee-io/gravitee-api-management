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
package io.gravitee.repository.analytics.query.response.histogram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Bucket {

    private final String name;
    private List<Bucket> buckets;
    private Map<String, List<Data>> data;

    public Bucket(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    // Lazy loading to avoid useless structure initialization
    public Map<String, List<Data>> data() {
        if (data == null) {
            data = new HashMap<>();
        }
        return data;
    }

    // Lazy loading to avoid useless structure initialization
    public List<Bucket> buckets() {
        if (buckets == null) {
            buckets = new ArrayList<>();
        }
        return buckets;
    }
}
