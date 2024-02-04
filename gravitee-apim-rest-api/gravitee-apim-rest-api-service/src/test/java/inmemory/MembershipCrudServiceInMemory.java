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

import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MembershipCrudServiceInMemory implements MembershipCrudService, InMemoryAlternative<Membership> {

    final ArrayList<Membership> storage = new ArrayList<>();

    @Override
    public Membership create(Membership entity) {
        storage.add(entity);
        return entity;
    }

    @Override
    public void initWith(List<Membership> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Membership> storage() {
        return Collections.unmodifiableList(storage);
    }
}
