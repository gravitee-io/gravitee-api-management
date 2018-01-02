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
package io.gravitee.management.model.pagedresult;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class Metadata {
    private Map<String, Map<String, Object>> metadata = new HashMap<>();

    public Map<String, Map<String, Object>> getMetadata() {
        return metadata;
    }

    public boolean containsKey(String key) {
        return metadata.containsKey(key);
    }

    public void put(String key, String valueKey, Object valueValue) {
        if (containsKey(key)) {
            metadata.get(key).put(valueKey, valueValue);
        } else {
            HashMap<String, Object> value = new HashMap<>();
            value.put(valueKey, valueValue);
            metadata.put(key, value);
        }
    }
}
