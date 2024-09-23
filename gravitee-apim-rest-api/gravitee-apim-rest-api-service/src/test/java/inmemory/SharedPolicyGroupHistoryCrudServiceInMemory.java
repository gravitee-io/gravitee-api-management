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

import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupHistoryCrudService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SharedPolicyGroupHistoryCrudServiceInMemory
    implements SharedPolicyGroupHistoryCrudService, InMemoryAlternative<SharedPolicyGroup> {

    final ArrayList<SharedPolicyGroup> storage = new ArrayList<>();

    @Override
    public SharedPolicyGroup create(SharedPolicyGroup sharedPolicyGroupEntity) {
        storage.add(sharedPolicyGroupEntity);
        return sharedPolicyGroupEntity;
    }

    @Override
    public void delete(String sharedPolicyGroupId) {
        storage.removeIf(spg -> spg.getId().equals(sharedPolicyGroupId));
    }

    @Override
    public void initWith(List<SharedPolicyGroup> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<SharedPolicyGroup> storage() {
        return Collections.unmodifiableList(storage);
    }
}
