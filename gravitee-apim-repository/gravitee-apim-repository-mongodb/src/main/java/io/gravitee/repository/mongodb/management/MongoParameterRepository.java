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
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.repository.mongodb.management.internal.api.ParameterMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ParameterMongo;
import io.gravitee.repository.mongodb.management.internal.model.ParameterPkMongo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoParameterRepository implements ParameterRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoParameterRepository.class);

    @Autowired
    private ParameterMongoRepository internalParameterRepo;

    @Override
    public Optional<Parameter> findById(String parameterKey, String referenceId, ParameterReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Find parameter by key [{}]", parameterKey);
        final ParameterMongo parameter = internalParameterRepo
            .findById(new ParameterPkMongo(parameterKey, referenceId, referenceType.name()))
            .orElse(null);
        LOGGER.debug("Find parameter by key [{}] - Done", parameterKey);
        return Optional.ofNullable(map(parameter));
    }

    @Override
    public List<Parameter> findByKeys(List<String> keys, String referenceId, ParameterReferenceType referenceType) {
        LOGGER.debug("Find parameters by keys [{}]", keys);
        List<ParameterPkMongo> pkList = keys
            .stream()
            .map(k -> new ParameterPkMongo(k, referenceId, referenceType.name()))
            .collect(Collectors.toList());
        Iterable<ParameterMongo> all = internalParameterRepo.findAllById(pkList);
        LOGGER.debug("Find parameters by keys [{}] - Done", keys);
        return StreamSupport.stream(all.spliterator(), false).map(this::map).collect(Collectors.toList());
    }

    @Override
    public Parameter create(Parameter parameter) throws TechnicalException {
        LOGGER.debug("Create parameter [{}]", parameter);
        ParameterMongo parameterMongo = map(parameter);
        ParameterMongo createdParameterMongo = internalParameterRepo.insert(parameterMongo);
        Parameter res = map(createdParameterMongo);
        LOGGER.debug("Create parameter [{}] - Done", parameter);
        return res;
    }

    @Override
    public Parameter update(Parameter parameter) throws TechnicalException {
        if (parameter == null || parameter.getKey() == null) {
            throw new IllegalStateException("Parameter to update must have a key");
        }
        final ParameterMongo parameterMongo = internalParameterRepo
            .findById(new ParameterPkMongo(parameter.getKey(), parameter.getReferenceId(), parameter.getReferenceType().name()))
            .orElse(null);
        if (parameterMongo == null) {
            throw new IllegalStateException(String.format("No parameter found with name [%s]", parameter.getKey()));
        }
        try {
            parameterMongo.setValue(parameter.getValue());
            ParameterMongo parameterMongoUpdated = internalParameterRepo.save(parameterMongo);
            return map(parameterMongoUpdated);
        } catch (Exception e) {
            LOGGER.error("An error occurred when updating parameter", e);
            throw new TechnicalException("An error occurred when updating parameter");
        }
    }

    @Override
    public void delete(String parameterKey, String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        try {
            internalParameterRepo.deleteById(new ParameterPkMongo(parameterKey, referenceId, referenceType.name()));
        } catch (Exception e) {
            LOGGER.error("An error occurred when deleting parameter [{}]", parameterKey, e);
            throw new TechnicalException("An error occurred when deleting parameter");
        }
    }

    @Override
    public List<Parameter> findAll(String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("Find parameters by keys and env");
        Iterable<ParameterMongo> all = internalParameterRepo.findAll(referenceId, referenceType.name());

        LOGGER.debug("Find parameters by keys and env - Done");
        return StreamSupport.stream(all.spliterator(), false).map(this::map).collect(Collectors.toList());
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, ParameterReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Delete parameter by refId: {}/{}", referenceId, referenceType);
        try {
            final var parameterMongos = internalParameterRepo
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .stream()
                .map(parameterMongo -> parameterMongo.getId().getKey())
                .toList();
            LOGGER.debug("Delete parameter by refId {}/{} - Done", referenceId, referenceType);

            return parameterMongos;
        } catch (Exception e) {
            LOGGER.error("Failed to delete parameter by refId: {}/{}", referenceId, referenceType, e);
            throw new TechnicalException("Failed to delete parameter by reference");
        }
    }

    private Parameter map(final ParameterMongo parameterMongo) {
        if (parameterMongo == null) {
            return null;
        }
        final Parameter parameter = new Parameter();
        parameter.setKey(parameterMongo.getId().getKey());
        parameter.setReferenceType(ParameterReferenceType.valueOf(parameterMongo.getId().getReferenceType()));
        parameter.setReferenceId(parameterMongo.getId().getReferenceId());
        parameter.setValue(parameterMongo.getValue());
        return parameter;
    }

    private ParameterMongo map(final Parameter parameter) {
        ParameterMongo parameterMongo = new ParameterMongo();
        parameterMongo.setId(new ParameterPkMongo(parameter.getKey(), parameter.getReferenceId(), parameter.getReferenceType().name()));
        parameterMongo.setValue(parameter.getValue());
        return parameterMongo;
    }

    @Override
    public Set<Parameter> findAll() throws TechnicalException {
        return internalParameterRepo.findAll().stream().map(this::map).collect(Collectors.toSet());
    }
}
