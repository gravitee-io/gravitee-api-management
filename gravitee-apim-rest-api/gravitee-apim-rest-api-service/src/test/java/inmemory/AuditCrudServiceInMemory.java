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

import io.gravitee.apim.core.audit.crud_service.AuditCrudService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditCrudServiceInMemory implements AuditCrudService, InMemoryAlternative<AuditEntity> {

    final List<AuditEntity> storage = new ArrayList<>();

    @Override
    public void create(AuditEntity auditEntity) {
        storage.add(auditEntity);
    }

    @Override
    public void initWith(List<AuditEntity> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<AuditEntity> storage() {
        return Collections.unmodifiableList(storage);
    }
}
