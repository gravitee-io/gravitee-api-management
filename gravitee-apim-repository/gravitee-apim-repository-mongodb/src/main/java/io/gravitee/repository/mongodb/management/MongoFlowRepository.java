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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.mongodb.management.internal.flow.FlowMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.FlowMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoFlowRepository implements FlowRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private FlowMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Flow> findById(String id) throws TechnicalException {
        logger.debug("Find flow by ID [{}]", id);

        FlowMongo flow = internalRepository.findById(id).orElse(null);
        Flow res = map(flow);

        logger.debug("Find flow by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public Flow create(Flow flow) throws TechnicalException {
        logger.debug("Create flow [{}]", flow.getId());
        Flow createdFlow = map(internalRepository.insert(map(flow)));
        logger.debug("Create flow [{}] - Done", createdFlow.getId());
        return createdFlow;
    }

    @Override
    public Flow update(Flow flow) throws TechnicalException {
        if (flow == null) {
            throw new IllegalStateException("Group must not be null");
        }

        internalRepository
            .findById(flow.getId())
            .orElseThrow(() -> new IllegalStateException(String.format("No flow found with id [%s]", flow.getId())));

        logger.debug("Update flow [{}]", flow.getName());
        return map(internalRepository.save(map(flow)));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("Delete flow [{}]", id);
        internalRepository.deleteById(id);
        logger.debug("Delete flow [{}] - Done", id);
    }

    @Override
    public List<Flow> findByReference(FlowReferenceType referenceType, String referenceId) {
        final List<FlowMongo> flows = internalRepository.findAll(referenceType.name(), referenceId);
        return flows.stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, FlowReferenceType referenceType) throws TechnicalException {
        logger.debug("Delete flows by reference [{},{}]", referenceId, referenceType);
        try {
            final var flows = internalRepository
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .stream()
                .map(FlowMongo::getId)
                .toList();
            logger.debug("Delete flows by reference [{},{}] - Done", referenceId, referenceType);
            return flows;
        } catch (Exception ex) {
            logger.error("Failed to delete flows by refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete flows by reference");
        }
    }

    @Override
    public void deleteAllById(Collection<String> ids) {
        logger.debug("Delete flows [{}]", ids);
        internalRepository.deleteAllById(ids);
        logger.debug("Delete flows [{}] - Done", ids);
    }

    @Override
    public Set<Flow> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }

    private FlowMongo map(Flow flow) {
        return mapper.map(flow);
    }

    private Flow map(FlowMongo flow) {
        return mapper.map(flow);
    }
}
