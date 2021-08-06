/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class FlowRepositoryProxy extends AbstractProxy<FlowRepository> implements FlowRepository {

    @Override
    public List<Flow> findByReference(FlowReferenceType referenceType, String referenceId) throws TechnicalException {
        return target.findByReference(referenceType, referenceId);
    }

    @Override
    public void deleteByReference(FlowReferenceType referenceType, String referenceId) throws TechnicalException {
        target.deleteByReference(referenceType, referenceId);
    }

    @Override
    public Optional<Flow> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public Flow create(Flow item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Flow update(Flow item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }
}
