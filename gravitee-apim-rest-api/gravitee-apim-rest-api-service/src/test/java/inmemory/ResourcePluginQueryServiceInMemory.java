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

import io.gravitee.apim.core.plugin.model.ResourcePlugin;
import io.gravitee.apim.core.plugin.query_service.ResourcePluginQueryService;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResourcePluginQueryServiceInMemory implements ResourcePluginQueryService, InMemoryAlternative<ResourcePlugin> {

    private List<ResourcePlugin> storage = List.of();

    @Override
    public Set<ResourcePlugin> findAll() {
        return new HashSet<>(storage);
    }

    @Override
    public String getSchema(String resourceId) {
        return (
            String.format(
                """
                          {
                          "type": "object",
                          "properties": {
                            "name": {
                              "type": "string",
                              "description": "This is a schema for %s resource plugin."
                            }
                          }
                        }""",
                resourceId
            )
        );
    }

    @Override
    public void initWith(List<ResourcePlugin> items) {
        this.storage = items;
    }

    @Override
    public void reset() {
        this.storage = List.of();
    }

    @Override
    public List<ResourcePlugin> storage() {
        return Collections.unmodifiableList(storage);
    }
}
