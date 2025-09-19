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

import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.media.query_service.MediaQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MediaQueryServiceInMemory implements MediaQueryService, InMemoryAlternative<Media> {

    private final List<Media> storage = new ArrayList<>();

    @Override
    public List<Media> findAllByApiId(String apiId) {
        return storage
            .stream()
            .filter(media -> media.getApiId().equals(apiId))
            .toList();
    }

    @Override
    public void initWith(List<Media> items) {
        reset();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Media> storage() {
        return Collections.unmodifiableList(storage);
    }
}
