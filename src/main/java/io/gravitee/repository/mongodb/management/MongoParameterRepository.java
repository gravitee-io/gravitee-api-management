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
package io.gravitee.repository.mongodb.management;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.repository.mongodb.management.internal.api.ParameterMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ParameterMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoParameterRepository implements ParameterRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoParameterRepository.class);

    @Autowired
    private ParameterMongoRepository internalParameterRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Parameter> findById(String parameterKey) throws TechnicalException {
        LOGGER.debug("Find parameter by key [{}]", parameterKey);
        final ParameterMongo parameter = internalParameterRepo.findById(parameterKey).orElse(null);
        LOGGER.debug("Find parameter by key [{}] - Done", parameterKey);
        return Optional.ofNullable(mapper.map(parameter, Parameter.class));
    }

    @Override
    public List<Parameter> findAll(List<String> keys) throws TechnicalException {
        LOGGER.debug("Find parameters by keys [{}]", keys);
        Iterable<ParameterMongo> all = internalParameterRepo.findAllById(keys);
        LOGGER.debug("Find parameters by keys [{}] - Done", keys);
        return StreamSupport.stream(all.spliterator(), false)
                .map(parameter -> mapper.map(parameter, Parameter.class))
                .collect(Collectors.toList());
    }
    
    @Override
    public Parameter create(Parameter parameter) throws TechnicalException {
        LOGGER.debug("Create parameter [{}]", parameter);
        ParameterMongo parameterMongo = mapper.map(parameter, ParameterMongo.class);
        ParameterMongo createdParameterMongo = internalParameterRepo.insert(parameterMongo);
        Parameter res = mapper.map(createdParameterMongo, Parameter.class);
        LOGGER.debug("Create parameter [{}] - Done", parameter);
        return res;
    }

    @Override
    public Parameter update(Parameter parameter) throws TechnicalException {
        if (parameter == null || parameter.getKey() == null) {
            throw new IllegalStateException("Parameter to update must have a key");
        }
        final ParameterMongo parameterMongo = internalParameterRepo.findById(parameter.getKey()).orElse(null);
        if (parameterMongo == null) {
            throw new IllegalStateException(String.format("No parameter found with name [%s]", parameter.getKey()));
        }
        try {
            parameterMongo.setValue(parameter.getValue());
            ParameterMongo parameterMongoUpdated = internalParameterRepo.save(parameterMongo);
            return mapper.map(parameterMongoUpdated, Parameter.class);
        } catch (Exception e) {
            LOGGER.error("An error occured when updating parameter", e);
            throw new TechnicalException("An error occured when updating parameter");
        }
    }

    @Override
    public void delete(String parameterKey) throws TechnicalException {
        try {
            internalParameterRepo.deleteById(parameterKey);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting parameter [{}]", parameterKey, e);
            throw new TechnicalException("An error occured when deleting parameter");
        }
    }

    @Override
    public List<Parameter> findAllByReferenceIdAndReferenceType(List<String> keys, String referenceId,
            ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("Find parameters by keys and env");
        Iterable<ParameterMongo> all;
        if( keys != null) {
            all = internalParameterRepo.findAllByReferenceIdAndReferenceType(keys, referenceId, referenceType.name());
        } else {
            all = internalParameterRepo.findAllByReferenceIdAndReferenceType(referenceId, referenceType.name());
        }
        LOGGER.debug("Find parameters by keys and env - Done");
        return StreamSupport.stream(all.spliterator(), false)
                .map(parameter -> mapper.map(parameter, Parameter.class))
                .collect(Collectors.toList());
    }

}
