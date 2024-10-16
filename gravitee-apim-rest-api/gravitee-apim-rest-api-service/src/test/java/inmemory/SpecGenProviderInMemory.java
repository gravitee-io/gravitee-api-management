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

import io.gravitee.apim.core.specgen.model.ApiSpecGenOperation;
import io.gravitee.apim.core.specgen.model.ApiSpecGenRequestReply;
import io.gravitee.apim.core.specgen.service_provider.SpecGenProvider;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SpecGenProviderInMemory implements SpecGenProvider, InMemoryAlternative<Single<ApiSpecGenRequestReply>> {

    private List<Single<ApiSpecGenRequestReply>> storage;

    @Override
    public Single<ApiSpecGenRequestReply> performRequest(String apiId, ApiSpecGenOperation operation) {
        return storage.get(0);
    }

    @Override
    public void initWith(List<Single<ApiSpecGenRequestReply>> items) {
        reset();
        storage = items;
    }

    @Override
    public void reset() {
        storage = null;
    }

    @Override
    public List<Single<ApiSpecGenRequestReply>> storage() {
        return storage;
    }
}
