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

import static io.gravitee.repository.management.model.Api.AuditEvent.API_ROLLBACKED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.api.RollbackApiEntity;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_RollbackTest {

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private AuditService auditService;

    @Mock
    private ApiDuplicatorService apiDuplicatorService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void rollback_should_create_rollback_api_audit_log() {
        RollbackApiEntity rollbackApiEntity = new RollbackApiEntity();

        apiService.rollback(GraviteeContext.getExecutionContext(), "my-api-id", rollbackApiEntity);

        verify(auditService, times(1))
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq("my-api-id"),
                anyMap(),
                same(API_ROLLBACKED),
                any(),
                isNull(),
                isNull()
            );
    }

    @Test
    public void rollback_should_import_api_using_apiduplicator_service() throws JsonProcessingException {
        RollbackApiEntity rollbackApiEntity = new RollbackApiEntity();
        when(objectMapper.writeValueAsString(rollbackApiEntity)).thenReturn("my-serialized-api");

        apiService.rollback(GraviteeContext.getExecutionContext(), "my-api-id", rollbackApiEntity);

        verify(apiDuplicatorService, times(1))
            .updateWithImportedDefinition(GraviteeContext.getExecutionContext(), "my-api-id", "my-serialized-api");
    }
}
