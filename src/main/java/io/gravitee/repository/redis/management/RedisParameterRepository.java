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
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class RedisParameterRepository implements ParameterRepository {
    @Override
    public Optional<Parameter> findById(String s) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public Parameter create(Parameter item) throws TechnicalException {
        return null;
    }

    @Override
    public Parameter update(Parameter item) throws TechnicalException {
        return null;
    }

    @Override
    public void delete(String s) throws TechnicalException {

    }
}
