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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApiCrudServiceInMemory implements ApiCrudService, InMemoryAlternative<Api> {

    final ArrayList<Api> apis = new ArrayList<>();

    @Override
    public Api get(String id) {
        var foundApi = apis.stream().filter(api -> id.equals(api.getId())).findFirst();
        return foundApi.orElseThrow(() -> new ApiNotFoundException(id));
    }

    @Override
    public void create(Api api) {}

    @Override
    public void initWith(List<Api> items) {
        apis.clear();
        apis.addAll(items);
    }

    @Override
    public void reset() {
        apis.clear();
    }

    @Override
    public List<Api> storage() {
        return Collections.unmodifiableList(apis);
    }
}
