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

import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.apim.core.workflow.model.Workflow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkflowCrudServiceInMemory implements WorkflowCrudService, InMemoryAlternative<Workflow> {

    final ArrayList<Workflow> storage = new ArrayList<>();

    @Override
    public Workflow create(Workflow entity) {
        storage.add(entity);
        return entity;
    }

    @Override
    public Collection<Workflow> findByApiId(String apiId) {
        return storage
            .stream()
            .filter(w -> w.getReferenceType() == Workflow.ReferenceType.API && apiId.equals(w.getReferenceId()))
            .toList();
    }

    @Override
    public void initWith(List<Workflow> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Workflow> storage() {
        return Collections.unmodifiableList(storage);
    }
}
