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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.redis.management.internal.ParameterRedisRepository;
import io.gravitee.repository.redis.management.model.RedisParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class RedisParameterRepository implements ParameterRepository {

    @Autowired
    private ParameterRedisRepository parameterRedisRepository;

    @Override
    public Optional<Parameter> findById(final String parameterKey) throws TechnicalException {
        final RedisParameter redisParameter = parameterRedisRepository.findById(parameterKey);
        return Optional.ofNullable(convert(redisParameter));
    }

    @Override
    public Parameter create(final Parameter parameter) throws TechnicalException {
        final RedisParameter redisParameter = parameterRedisRepository.saveOrUpdate(convert(parameter));
        return convert(redisParameter);
    }

    @Override
    public Parameter update(final Parameter parameter) throws TechnicalException {
        if (parameter == null || parameter.getKey() == null) {
            throw new IllegalStateException("Parameter to update must have a key");
        }

        final RedisParameter redisParameter = parameterRedisRepository.findById(parameter.getKey());

        if (redisParameter == null) {
            throw new IllegalStateException(String.format("No parameter found with key [%s]", parameter.getKey()));
        }

        final RedisParameter redisParameterUpdated = parameterRedisRepository.saveOrUpdate(convert(parameter));
        return convert(redisParameterUpdated);
    }

    @Override
    public void delete(final String parameterId) throws TechnicalException {
        parameterRedisRepository.delete(parameterId);
    }

    private Parameter convert(final RedisParameter redisParameter) {
        if (redisParameter == null) {
            return null;
        }
        final Parameter parameter = new Parameter();
        parameter.setKey(redisParameter.getKey());
        parameter.setValue(redisParameter.getValue());
        return parameter;
    }

    private RedisParameter convert(final Parameter parameter) {
        if (parameter == null) {
            return null;
        }
        final RedisParameter redisParameter = new RedisParameter();
        redisParameter.setKey(parameter.getKey());
        redisParameter.setValue(parameter.getValue());
        return redisParameter;
    }
}
