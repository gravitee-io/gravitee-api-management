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
package io.gravitee.repository.mock.management;

import static io.gravitee.repository.management.QualityRuleRepositoryTest.DATE;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.QualityRuleRepository;
import io.gravitee.repository.management.model.QualityRule;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class QualityRuleRepositoryMock extends AbstractRepositoryMock<QualityRuleRepository> {

    public QualityRuleRepositoryMock() {
        super(QualityRuleRepository.class);
    }

    @Override
    protected void prepare(QualityRuleRepository qualityRuleRepository) throws Exception {
        final QualityRule qualityRule = mock(QualityRule.class);
        when(qualityRule.getId()).thenReturn("quality-rule1");
        when(qualityRule.getName()).thenReturn("QualityRule name");
        when(qualityRule.getDescription()).thenReturn("QualityRule description");
        when(qualityRule.getWeight()).thenReturn(10);
        when(qualityRule.getCreatedAt()).thenReturn(DATE);
        when(qualityRule.getUpdatedAt()).thenReturn(DATE);

        final QualityRule qualityRule2 = mock(QualityRule.class);
        when(qualityRule2.getId()).thenReturn("quality-rule2");
        when(qualityRule2.getName()).thenReturn("Description in english");
        when(qualityRule2.getDescription()).thenReturn("Description must be in english");

        final QualityRule qualityRule2Updated = mock(QualityRule.class);
        when(qualityRule2Updated.getName()).thenReturn("New name");
        when(qualityRule2Updated.getDescription()).thenReturn("New description");
        when(qualityRule2Updated.getWeight()).thenReturn(5);
        when(qualityRule2Updated.getCreatedAt()).thenReturn(DATE);
        when(qualityRule2Updated.getUpdatedAt()).thenReturn(DATE);

        final QualityRule qualityRule3 = mock(QualityRule.class);
        when(qualityRule3.getId()).thenReturn("quality-rule3");
        when(qualityRule3.getName()).thenReturn("Api-key plan");
        when(qualityRule3.getDescription()).thenReturn("A plan api-key is published");
        when(qualityRule3.getWeight()).thenReturn(3);
        when(qualityRule3.getCreatedAt()).thenReturn(DATE);
        when(qualityRule3.getUpdatedAt()).thenReturn(DATE);

        final Set<QualityRule> qualityRules = newSet(qualityRule, qualityRule2, qualityRule3);
        final Set<QualityRule> qualityRulesAfterDelete = newSet(qualityRule, qualityRule2);
        final Set<QualityRule> qualityRulesAfterAdd = newSet(qualityRule, qualityRule2, mock(QualityRule.class), mock(QualityRule.class));

        when(qualityRuleRepository.findAll())
            .thenReturn(qualityRules, qualityRulesAfterAdd, qualityRules, qualityRulesAfterDelete, qualityRules);

        when(qualityRuleRepository.create(any(QualityRule.class))).thenReturn(qualityRule);

        when(qualityRuleRepository.findById("new-qualityRule")).thenReturn(of(qualityRule));
        when(qualityRuleRepository.findById("quality-rule1")).thenReturn(of(qualityRule2), of(qualityRule2Updated));

        when(qualityRuleRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
