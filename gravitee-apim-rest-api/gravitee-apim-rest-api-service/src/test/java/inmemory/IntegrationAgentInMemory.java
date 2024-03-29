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

import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntegrationAgentInMemory implements IntegrationAgent, InMemoryAlternative<IntegrationApi> {

    List<IntegrationApi> storage = new ArrayList<>();

    @Override
    public Flowable<IntegrationApi> fetchAllApis(Integration integration) {
        return Flowable.fromIterable(storage).filter(asset -> asset.integrationId().equals(integration.getId()));
    }

    @Override
    public void initWith(List<IntegrationApi> items) {
        this.storage.addAll(items);
    }

    @Override
    public void reset() {
        this.storage.clear();
    }

    @Override
    public List<IntegrationApi> storage() {
        return Collections.unmodifiableList(storage);
    }
}
