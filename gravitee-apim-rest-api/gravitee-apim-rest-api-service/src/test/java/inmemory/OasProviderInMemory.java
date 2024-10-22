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

import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.service_provider.OasProvider;
import java.util.List;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OasProviderInMemory implements OasProvider, InMemoryAlternative<String> {

    private List<String> storage;

    @Override
    public String decorateSpecification(ApiSpecGen apiSpecGen, String spec) {
        return storage
            .stream()
            .filter(str -> str.contains("description: " + apiSpecGen.description()))
            .filter(str -> str.contains("title: " + apiSpecGen.name()))
            .filter(str -> str.contains("version: " + apiSpecGen.version()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void initWith(List<String> items) {
        reset();
        storage = items;
    }

    @Override
    public void reset() {
        storage = null;
    }

    @Override
    public List<String> storage() {
        return storage;
    }
}
