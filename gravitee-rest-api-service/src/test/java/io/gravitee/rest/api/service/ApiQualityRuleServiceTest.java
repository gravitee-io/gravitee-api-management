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
package io.gravitee.rest.api.service;

import com.google.common.collect.ImmutableMap;
import io.gravitee.rest.api.model.quality.*;
import io.gravitee.rest.api.service.exceptions.ApiQualityRuleNotFoundException;
import io.gravitee.rest.api.service.impl.ApiQualityRuleServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.model.ApiQualityRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;

import static io.gravitee.repository.management.model.Audit.AuditProperties.API_QUALITY_RULE;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiQualityRuleServiceTest {

    private static final String QUALITY_RULE_ID = "id-qr";
    private static final String API_ID = "id-api";

    @InjectMocks
    private ApiQualityRuleService apiQualityRuleService = new ApiQualityRuleServiceImpl();

    @Mock
    private ApiQualityRuleRepository apiQualityRuleRepository;
    @Mock
    private AuditService auditService;

    @Test
    public void shouldFindByApi() throws TechnicalException {
        final ApiQualityRule aqr = mock(ApiQualityRule.class);
        when(aqr.getApi()).thenReturn(API_ID);
        when(aqr.getQualityRule()).thenReturn(QUALITY_RULE_ID);
        when(aqr.isChecked()).thenReturn(true);
        when(aqr.getCreatedAt()).thenReturn(new Date(1));
        when(aqr.getUpdatedAt()).thenReturn(new Date(2));
        when(apiQualityRuleRepository.findByApi(API_ID)).thenReturn(singletonList(aqr));

        final List<ApiQualityRuleEntity> apiQualityRules = apiQualityRuleService.findByApi(API_ID);
        final ApiQualityRuleEntity apiQualityRuleEntity = apiQualityRules.iterator().next();
        assertEquals(API_ID, apiQualityRuleEntity.getApi());
        assertEquals(QUALITY_RULE_ID, apiQualityRuleEntity.getQualityRule());
        assertTrue(apiQualityRuleEntity.isChecked());
        assertEquals(new Date(1), apiQualityRuleEntity.getCreatedAt());
        assertEquals(new Date(2), apiQualityRuleEntity.getUpdatedAt());
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final NewApiQualityRuleEntity newApiQualityRuleEntity = new NewApiQualityRuleEntity();
        newApiQualityRuleEntity.setApi(API_ID);
        newApiQualityRuleEntity.setQualityRule(QUALITY_RULE_ID);
        newApiQualityRuleEntity.setChecked(true);

        final ApiQualityRule createdApiQualityRule = new ApiQualityRule();
        createdApiQualityRule.setApi(API_ID);
        createdApiQualityRule.setQualityRule(QUALITY_RULE_ID);
        createdApiQualityRule.setChecked(true);
        createdApiQualityRule.setCreatedAt(new Date());
        createdApiQualityRule.setUpdatedAt(new Date());
        when(apiQualityRuleRepository.create(any())).thenReturn(createdApiQualityRule);

        final ApiQualityRuleEntity apiQualityRuleEntity = apiQualityRuleService.create(newApiQualityRuleEntity);

        assertEquals(API_ID, apiQualityRuleEntity.getApi());
        assertEquals(QUALITY_RULE_ID, apiQualityRuleEntity.getQualityRule());
        assertTrue(apiQualityRuleEntity.isChecked());
        assertNotNull(apiQualityRuleEntity.getCreatedAt());
        assertNotNull(apiQualityRuleEntity.getUpdatedAt());


        final ApiQualityRule apiQualityRule = new ApiQualityRule();
        apiQualityRule.setApi(API_ID);
        apiQualityRule.setQualityRule(QUALITY_RULE_ID);
        apiQualityRule.setChecked(true);

        verify(apiQualityRuleRepository, times(1)).create(argThat(argument ->
                API_ID.equals(argument.getApi()) &&
                        QUALITY_RULE_ID.equals(argument.getQualityRule()) &&
                        argument.isChecked() &&
                        argument.getCreatedAt() != null &&
                        argument.getUpdatedAt() != null));
        verify(auditService, times(1)).createEnvironmentAuditLog(
                eq(ImmutableMap.of(API_QUALITY_RULE, API_ID)),
                eq(ApiQualityRule.AuditEvent.API_QUALITY_RULE_CREATED),
                any(Date.class),
                isNull(),
                any());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        final UpdateApiQualityRuleEntity updateApiQualityRuleEntity = new UpdateApiQualityRuleEntity();
        updateApiQualityRuleEntity.setApi(API_ID);
        updateApiQualityRuleEntity.setQualityRule(QUALITY_RULE_ID);
        updateApiQualityRuleEntity.setChecked(true);

        final ApiQualityRule updatedApiQualityRule = new ApiQualityRule();
        updatedApiQualityRule.setApi(API_ID);
        updatedApiQualityRule.setQualityRule(QUALITY_RULE_ID);
        updatedApiQualityRule.setChecked(true);
        updatedApiQualityRule.setCreatedAt(new Date());
        updatedApiQualityRule.setUpdatedAt(new Date());
        when(apiQualityRuleRepository.update(any())).thenReturn(updatedApiQualityRule);
        when(apiQualityRuleRepository.findById(API_ID, QUALITY_RULE_ID)).thenReturn(of(updatedApiQualityRule));

        final ApiQualityRuleEntity apiQualityRuleEntity = apiQualityRuleService.update(updateApiQualityRuleEntity);

        assertEquals(API_ID, apiQualityRuleEntity.getApi());
        assertEquals(QUALITY_RULE_ID, apiQualityRuleEntity.getQualityRule());
        assertTrue(apiQualityRuleEntity.isChecked());
        assertNotNull(apiQualityRuleEntity.getCreatedAt());
        assertNotNull(apiQualityRuleEntity.getUpdatedAt());


        final ApiQualityRule apiQualityRule = new ApiQualityRule();
        apiQualityRule.setApi(API_ID);
        apiQualityRule.setQualityRule(QUALITY_RULE_ID);
        apiQualityRule.setChecked(true);

        verify(apiQualityRuleRepository, times(1)).update(argThat(argument ->
                API_ID.equals(argument.getApi()) &&
                        QUALITY_RULE_ID.equals(argument.getQualityRule()) &&
                        argument.isChecked() &&
                        argument.getCreatedAt() != null &&
                        argument.getUpdatedAt() != null));
        verify(auditService, times(1)).createEnvironmentAuditLog(
                eq(ImmutableMap.of(API_QUALITY_RULE, API_ID)),
                eq(ApiQualityRule.AuditEvent.API_QUALITY_RULE_UPDATED),
                any(Date.class),
                any(),
                any());
    }

    @Test(expected = ApiQualityRuleNotFoundException.class)
    public void shouldNotUpdate() throws TechnicalException {
        final UpdateApiQualityRuleEntity updateApiQualityRuleEntity = new UpdateApiQualityRuleEntity();
        updateApiQualityRuleEntity.setApi(API_ID);
        updateApiQualityRuleEntity.setQualityRule(QUALITY_RULE_ID);

        when(apiQualityRuleRepository.findById(API_ID, QUALITY_RULE_ID)).thenReturn(empty());

        apiQualityRuleService.update(updateApiQualityRuleEntity);
    }
}
