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

import io.gravitee.apim.core.plugin.crud_service.PolicyPluginCrudService;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.rest.api.service.exceptions.PolicyNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PolicyPluginCrudServiceInMemory implements PolicyPluginCrudService, InMemoryAlternative<PolicyPlugin> {

    final List<PolicyPlugin> storage = new ArrayList<>();

    @Override
    public Optional<PolicyPlugin> get(String policyId) {
        if (policyId == null) {
            throw new TechnicalManagementException("policyId should not be null");
        }

        return storage.stream().filter(policyPlugin -> policyId.equals(policyPlugin.getId())).findFirst();
    }

    @Override
    public void initWith(List<PolicyPlugin> items) {
        storage.addAll(items.stream().toList());
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PolicyPlugin> storage() {
        return Collections.unmodifiableList(storage);
    }
}
