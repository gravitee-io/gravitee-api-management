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
package inmemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapStorage<K, V> {

    private Map<K, V> data = new HashMap<>();

    public MapStorage() {}

    public Map<K, V> unmodifiableData() {
        return Collections.unmodifiableMap(data);
    }

    Map<K, V> data() {
        return data;
    }

    void clear() {
        data.clear();
    }

    public static <K, V> MapStorage<K, V> from(Map<K, V> map) {
        final MapStorage<K, V> storage = new MapStorage<>();
        storage.data = map;
        return storage;
    }

    public static <K, V> MapStorage<K, V> of() {
        return MapStorage.from(Map.of());
    }
}
