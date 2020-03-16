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
import io.gravitee.repository.management.api.QualityRuleRepository;
import io.gravitee.repository.management.model.QualityRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class QualityRuleRepositoryProxy extends AbstractProxy<QualityRuleRepository> implements QualityRuleRepository {

    @Override
    public QualityRule create(QualityRule qualityRule) throws TechnicalException {
        return target.create(qualityRule);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }

    @Override
    public Optional<QualityRule> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public QualityRule update(QualityRule qualityRule) throws TechnicalException {
        return target.update(qualityRule);
    }

    @Override
    public Set<QualityRule> findAll() throws TechnicalException {
        return target.findAll();
    }
}
