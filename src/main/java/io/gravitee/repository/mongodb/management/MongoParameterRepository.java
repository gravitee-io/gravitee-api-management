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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.mongodb.management.internal.api.ParameterMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ParameterMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
        final ParameterMongo parameter = internalParameterRepo.findOne(parameterKey);
        LOGGER.debug("Find parameter by key [{}] - Done", parameterKey);
        return Optional.ofNullable(mapper.map(parameter, Parameter.class));
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
        final ParameterMongo parameterMongo = internalParameterRepo.findOne(parameter.getKey());
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
            internalParameterRepo.delete(parameterKey);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting parameter [{}]", parameterKey, e);
            throw new TechnicalException("An error occured when deleting parameter");
        }
    }
}
