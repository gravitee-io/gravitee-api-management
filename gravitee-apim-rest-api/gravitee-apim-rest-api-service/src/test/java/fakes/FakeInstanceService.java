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
package fakes;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.InstanceEntity;
import io.gravitee.rest.api.model.InstanceListItem;
import io.gravitee.rest.api.model.InstanceQuery;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

public class FakeInstanceService implements InstanceService {

    public Page<InstanceListItem> instanceListItem;
    public InstanceEntity instanceEntity;
    public List<InstanceEntity> instances;

    @Override
    public Page<InstanceListItem> search(ExecutionContext executionContext, InstanceQuery query) {
        return instanceListItem;
    }

    @Override
    public InstanceEntity findById(ExecutionContext executionContext, String instance) {
        return instanceEntity;
    }

    @Override
    public InstanceEntity findByEvent(ExecutionContext executionContext, String event) {
        return instanceEntity;
    }

    @Override
    public List<InstanceEntity> findAllStarted(ExecutionContext executionContext) {
        return instances;
    }
}
