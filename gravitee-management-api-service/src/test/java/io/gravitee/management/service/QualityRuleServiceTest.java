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
package io.gravitee.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.ImmutableMap;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.PlanStatus;
import io.gravitee.management.model.quality.*;
import io.gravitee.management.service.exceptions.ApiNotDeletableException;
import io.gravitee.management.service.exceptions.ApiQualityRuleNotFoundException;
import io.gravitee.management.service.exceptions.ApiRunningStateException;
import io.gravitee.management.service.exceptions.QualityRuleNotFoundException;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.management.service.impl.QualityRuleServiceImpl;
import io.gravitee.management.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.QualityRuleRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.QualityRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static io.gravitee.repository.management.model.Audit.AuditProperties.QUALITY_RULE;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class QualityRuleServiceTest {

    private static final String QUALITY_RULE_ID = "id-qr";

    @InjectMocks
    private QualityRuleService qualityRuleService = new QualityRuleServiceImpl();

    @Mock
    private ApiQualityRuleRepository apiQualityRuleRepository;
    @Mock
    private QualityRuleRepository qualityRuleRepository;
    @Mock
    private AuditService auditService;

    @Test
    public void shouldFindById() throws TechnicalException {
        final QualityRule qualityRule = mock(QualityRule.class);
        when(qualityRule.getId()).thenReturn(QUALITY_RULE_ID);
        when(qualityRule.getName()).thenReturn("NAME");
        when(qualityRule.getDescription()).thenReturn("DESC");
        when(qualityRule.getWeight()).thenReturn(1);
        when(qualityRule.getCreatedAt()).thenReturn(new Date(1));
        when(qualityRule.getUpdatedAt()).thenReturn(new Date(2));
        when(qualityRuleRepository.findById(QUALITY_RULE_ID)).thenReturn(of(qualityRule));

        final QualityRuleEntity qualityRuleEntity = qualityRuleService.findById(QUALITY_RULE_ID);
        assertEquals(QUALITY_RULE_ID, qualityRuleEntity.getId());
        assertEquals("NAME", qualityRuleEntity.getName());
        assertEquals("DESC", qualityRuleEntity.getDescription());
        assertEquals(1, qualityRuleEntity.getWeight());
        assertEquals(new Date(1), qualityRuleEntity.getCreatedAt());
        assertEquals(new Date(2), qualityRuleEntity.getUpdatedAt());
    }

    @Test(expected = QualityRuleNotFoundException.class)
    public void shouldNotFindById() throws TechnicalException {
        final QualityRule qualityRule = mock(QualityRule.class);
        when(qualityRuleRepository.findById(QUALITY_RULE_ID)).thenReturn(empty());

        qualityRuleService.findById(QUALITY_RULE_ID);
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        final QualityRule qualityRule = mock(QualityRule.class);
        when(qualityRule.getId()).thenReturn(QUALITY_RULE_ID);
        when(qualityRule.getName()).thenReturn("NAME");
        when(qualityRule.getDescription()).thenReturn("DESC");
        when(qualityRule.getWeight()).thenReturn(1);
        when(qualityRule.getCreatedAt()).thenReturn(new Date(1));
        when(qualityRule.getUpdatedAt()).thenReturn(new Date(2));
        when(qualityRuleRepository.findAll()).thenReturn(singleton(qualityRule));

        final List<QualityRuleEntity> qualityRules = qualityRuleService.findAll();
        final QualityRuleEntity qualityRuleEntity = qualityRules.iterator().next();
        assertEquals(QUALITY_RULE_ID, qualityRuleEntity.getId());
        assertEquals("NAME", qualityRuleEntity.getName());
        assertEquals("DESC", qualityRuleEntity.getDescription());
        assertEquals(1, qualityRuleEntity.getWeight());
        assertEquals(new Date(1), qualityRuleEntity.getCreatedAt());
        assertEquals(new Date(2), qualityRuleEntity.getUpdatedAt());
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final NewQualityRuleEntity newQualityRuleEntity = new NewQualityRuleEntity();
        newQualityRuleEntity.setName("NAME");
        newQualityRuleEntity.setDescription("DESC");
        newQualityRuleEntity.setWeight(1);

        final QualityRule createdQualityRule = new QualityRule();
        createdQualityRule.setId(QUALITY_RULE_ID);
        createdQualityRule.setName("NAME");
        createdQualityRule.setDescription("DESC");
        createdQualityRule.setWeight(1);
        createdQualityRule.setCreatedAt(new Date());
        createdQualityRule.setUpdatedAt(new Date());
        when(qualityRuleRepository.create(any())).thenReturn(createdQualityRule);

        final QualityRuleEntity qualityRuleEntity = qualityRuleService.create(newQualityRuleEntity);

        assertNotNull(qualityRuleEntity.getId());
        assertEquals("NAME", qualityRuleEntity.getName());
        assertEquals("DESC", qualityRuleEntity.getDescription());
        assertEquals(1, qualityRuleEntity.getWeight());
        assertNotNull(qualityRuleEntity.getCreatedAt());
        assertNotNull(qualityRuleEntity.getUpdatedAt());


        final QualityRule qualityRule = new QualityRule();
        qualityRule.setName("NAME");
        qualityRule.setDescription("DESC");
        qualityRule.setWeight(1);

        verify(qualityRuleRepository, times(1)).create(argThat(argument ->
                "NAME".equals(argument.getName()) &&
                        "DESC".equals(argument.getDescription()) &&
                        Integer.valueOf(1).equals(argument.getWeight()) &&
                        !argument.getId().isEmpty() &&
                        argument.getCreatedAt() != null &&
                        argument.getUpdatedAt() != null));
        verify(auditService, times(1)).createPortalAuditLog(
                eq(ImmutableMap.of(QUALITY_RULE, QUALITY_RULE_ID)),
                eq(QualityRule.AuditEvent.QUALITY_RULE_CREATED),
                any(Date.class),
                isNull(),
                any());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        final UpdateQualityRuleEntity updateQualityRuleEntity = new UpdateQualityRuleEntity();
        updateQualityRuleEntity.setId(QUALITY_RULE_ID);
        updateQualityRuleEntity.setName("NAME");
        updateQualityRuleEntity.setDescription("DESC");
        updateQualityRuleEntity.setWeight(1);

        final QualityRule updatedQualityRule = new QualityRule();
        updatedQualityRule.setId(QUALITY_RULE_ID);
        updatedQualityRule.setName("NAME");
        updatedQualityRule.setDescription("DESC");
        updatedQualityRule.setWeight(1);
        updatedQualityRule.setCreatedAt(new Date());
        updatedQualityRule.setUpdatedAt(new Date());
        when(qualityRuleRepository.update(any())).thenReturn(updatedQualityRule);
        when(qualityRuleRepository.findById(QUALITY_RULE_ID)).thenReturn(of(updatedQualityRule));

        final QualityRuleEntity qualityRuleEntity = qualityRuleService.update(updateQualityRuleEntity);

        assertNotNull(qualityRuleEntity.getId());
        assertEquals("NAME", qualityRuleEntity.getName());
        assertEquals("DESC", qualityRuleEntity.getDescription());
        assertEquals(1, qualityRuleEntity.getWeight());
        assertNotNull(qualityRuleEntity.getCreatedAt());
        assertNotNull(qualityRuleEntity.getUpdatedAt());


        final QualityRule qualityRule = new QualityRule();
        qualityRule.setName("NAME");
        qualityRule.setDescription("DESC");
        qualityRule.setWeight(1);

        verify(qualityRuleRepository, times(1)).update(argThat(argument ->
                "NAME".equals(argument.getName()) &&
                        "DESC".equals(argument.getDescription()) &&
                        Integer.valueOf(1).equals(argument.getWeight()) &&
                        QUALITY_RULE_ID.equals(argument.getId()) &&
                        argument.getCreatedAt() == null &&
                        argument.getUpdatedAt() != null));
        verify(auditService, times(1)).createPortalAuditLog(
                eq(ImmutableMap.of(QUALITY_RULE, QUALITY_RULE_ID)),
                eq(QualityRule.AuditEvent.QUALITY_RULE_UPDATED),
                any(Date.class),
                any(),
                any());
    }

    @Test(expected = QualityRuleNotFoundException.class)
    public void shouldNotUpdate() throws TechnicalException {
        final UpdateQualityRuleEntity updateQualityRuleEntity = new UpdateQualityRuleEntity();
        updateQualityRuleEntity.setId(QUALITY_RULE_ID);

        when(qualityRuleRepository.findById(QUALITY_RULE_ID)).thenReturn(empty());

        qualityRuleService.update(updateQualityRuleEntity);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        final QualityRule qualityRule = mock(QualityRule.class);
        when(qualityRuleRepository.findById(QUALITY_RULE_ID)).thenReturn(of(qualityRule));

        qualityRuleService.delete(QUALITY_RULE_ID);

        verify(qualityRuleRepository, times(1)).delete(QUALITY_RULE_ID);
        verify(auditService, times(1)).createPortalAuditLog(
                eq(ImmutableMap.of(QUALITY_RULE, QUALITY_RULE_ID)),
                eq(QualityRule.AuditEvent.QUALITY_RULE_DELETED),
                any(Date.class),
                isNull(),
                eq(qualityRule));
        verify(apiQualityRuleRepository, times(1)).deleteByQualityRule(QUALITY_RULE_ID);
    }
}
