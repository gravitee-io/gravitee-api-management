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
package io.gravitee.rest.api.service.impl;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuditService_SearchTest {

    private static final String USER_NAME = "myUser";
    private static final String API_ID = "id-api";

    @InjectMocks
    private AuditServiceImpl auditService = new AuditServiceImpl();

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private Api api;

    @Test
    public void should_search() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Audit auditFound = new Audit();
        auditFound.setId("auditId");
        auditFound.setUser(USER_NAME);
        auditFound.setReferenceType(Audit.AuditReferenceType.API);
        auditFound.setReferenceId(API_ID);

        Page<Audit> pageFound = new Page<>(Arrays.asList(auditFound), 2, 1, 2);

        when(
            auditRepository.search(
                eq(
                    new AuditCriteria.Builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentIds(singletonList(executionContext.getEnvironmentId()))
                        .build()
                ),
                any()
            )
        )
            .thenReturn(pageFound);
        when(userService.findById(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(new UserEntity());
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        AuditQuery query = new AuditQuery();
        query.setPage(2);
        query.setSize(1);
        final Page<AuditEntity> auditPage = auditService.search(executionContext, query);

        assertNotNull(auditPage);
        assertEquals(1, auditPage.getContent().size());
        assertEquals(auditFound.getId(), auditPage.getContent().get(0).getId());
        assertEquals(2, auditPage.getPageNumber());
        assertEquals(1, auditPage.getPageElements());
        assertEquals(2, auditPage.getTotalElements());
    }
}
