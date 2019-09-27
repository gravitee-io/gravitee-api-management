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
package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.model.ApiQualityRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiQualityRuleRepositoryProxy extends AbstractProxy<ApiQualityRuleRepository> implements ApiQualityRuleRepository {

    @Override
    public Optional<ApiQualityRule> findById(String api, String qualityRule) throws TechnicalException {
        return target.findById(api, qualityRule);
    }

    @Override
    public List<ApiQualityRule> findByQualityRule(String qualityRule) throws TechnicalException {
        return target.findByQualityRule(qualityRule);
    }

    @Override
    public List<ApiQualityRule> findByApi(String api) throws TechnicalException {
        return target.findByApi(api);
    }

    @Override
    public ApiQualityRule create(ApiQualityRule item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public ApiQualityRule update(ApiQualityRule item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public void delete(String api, String qualityRule) throws TechnicalException {
        target.delete(api, qualityRule);
    }

    @Override
    public void deleteByQualityRule(String qualityRule) throws TechnicalException {
        target.deleteByQualityRule(qualityRule);
    }

    @Override
    public void deleteByApi(String api) throws TechnicalException {
        target.deleteByApi(api);
    }
}
