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
package io.gravitee.repository.config.mock;

import static io.gravitee.repository.management.ApiQualityRuleRepositoryTest.DATE;
import static io.gravitee.repository.management.ApiQualityRuleRepositoryTest.NEW_DATE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.model.ApiQualityRule;

import java.util.List;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiQualityRuleRepositoryMock extends AbstractRepositoryMock<ApiQualityRuleRepository> {

    public ApiQualityRuleRepositoryMock() {
        super(ApiQualityRuleRepository.class);
    }

    @Override
    void prepare(ApiQualityRuleRepository apiQualityRuleRepository) throws Exception {
        final ApiQualityRule apiQualityRule = mock(ApiQualityRule.class);
        when(apiQualityRule.getApi()).thenReturn("api1");
        when(apiQualityRule.getQualityRule()).thenReturn("quality-rule1");
        when(apiQualityRule.isChecked()).thenReturn(true);
        when(apiQualityRule.getCreatedAt()).thenReturn(DATE);
        when(apiQualityRule.getUpdatedAt()).thenReturn(DATE);

        final ApiQualityRule apiQualityRule2 = mock(ApiQualityRule.class);
        when(apiQualityRule2.getApi()).thenReturn("api2");
        when(apiQualityRule2.getQualityRule()).thenReturn("quality-rule1");
        when(apiQualityRule2.isChecked()).thenReturn(true);
        when(apiQualityRule2.getCreatedAt()).thenReturn(DATE);
        when(apiQualityRule2.getUpdatedAt()).thenReturn(DATE);

        final ApiQualityRule apiQualityRule2Updated = mock(ApiQualityRule.class);
        when(apiQualityRule2Updated.getApi()).thenReturn("api2");
        when(apiQualityRule2Updated.getQualityRule()).thenReturn("quality-rule1");
        when(apiQualityRule2Updated.isChecked()).thenReturn(false);
        when(apiQualityRule2Updated.getCreatedAt()).thenReturn(NEW_DATE);
        when(apiQualityRule2Updated.getUpdatedAt()).thenReturn(NEW_DATE);

        final List<ApiQualityRule> apiQualityRules = asList(apiQualityRule, apiQualityRule2, mock(ApiQualityRule.class));
        final List<ApiQualityRule> apiQualityRulesAfterDelete = asList(apiQualityRule, apiQualityRule2);

        when(apiQualityRuleRepository.findByApi("api1"))
            .thenReturn(apiQualityRules, apiQualityRulesAfterDelete, apiQualityRules, emptyList());
        when(apiQualityRuleRepository.findByQualityRule("quality-rule2")).thenReturn(apiQualityRules, emptyList());

        final ApiQualityRule api2QualityRule = mock(ApiQualityRule.class);
        when(api2QualityRule.getApi()).thenReturn("api2");
        when(api2QualityRule.getQualityRule()).thenReturn("quality-rule1");
        when(api2QualityRule.isChecked()).thenReturn(true);
        when(api2QualityRule.getCreatedAt()).thenReturn(DATE);
        when(api2QualityRule.getUpdatedAt()).thenReturn(DATE);

        final List<ApiQualityRule> api2QualityRules = singletonList(api2QualityRule);
        final List<ApiQualityRule> api2QualityRulesAfterAdd = asList(api2QualityRule, apiQualityRule2);
        when(apiQualityRuleRepository.findByApi("api2")).thenReturn(api2QualityRules, api2QualityRulesAfterAdd, api2QualityRules);

        when(apiQualityRuleRepository.create(any(ApiQualityRule.class))).thenReturn(apiQualityRule);

        when(apiQualityRuleRepository.findById("api2", "new-apiQualityRule")).thenReturn(of(apiQualityRule));
        when(apiQualityRuleRepository.findById("api2", "quality-rule1")).thenReturn(of(apiQualityRule2), of(apiQualityRule2Updated));

        when(apiQualityRuleRepository.update(argThat(o -> o == null || "unknown".equals(o.getApi()))))
            .thenThrow(new IllegalStateException());
    }
}
