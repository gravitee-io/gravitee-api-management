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

import io.gravitee.apim.core.integration.service_provider.A2aAgentFetcher;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.reactivex.rxjava3.core.Single;
import jakarta.ws.rs.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;

public class A2aAgentFetcherInMemory implements A2aAgentFetcher, InMemoryAlternative<FederatedAgent> {

    Map<String, FederatedAgent> federatedAgents = new HashMap<>();

    @Deprecated // not relevant for here
    @Override
    public void initWith(List<FederatedAgent> items) {
        throw new NotImplementedException();
    }

    public void add(String url, FederatedAgent item) {
        federatedAgents.put(url, item);
    }

    public void initWithMap(Map<String, FederatedAgent> items) {
        reset();
        federatedAgents.putAll(items);
    }

    @Override
    public void reset() {
        federatedAgents.clear();
    }

    @Override
    public List<FederatedAgent> storage() {
        return federatedAgents.values().stream().toList();
    }

    @Override
    public Single<FederatedAgent> fetchAgentCard(String url) {
        var agent = federatedAgents.get(url);
        return agent != null ? Single.just(agent) : Single.error(new NotFoundException("Agent not found"));
    }
}
