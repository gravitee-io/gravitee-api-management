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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import io.gravitee.rest.api.model.UpdateViewEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.impl.ViewServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_DELETED;
import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_UPDATED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ViewService_DeleteTest {

    @InjectMocks
    private ViewServiceImpl viewService = new ViewServiceImpl();

    @Mock
    private ViewRepository mockViewRepository;

    @Mock
    private AuditService mockAuditService;

    @Mock
    private ApiService mockApiService;

    @Test
    public void shouldNotDeleteUnknownView() throws TechnicalException {
        when(mockViewRepository.findById("unknown", "DEFAULT")).thenReturn(Optional.empty());

        viewService.delete("unknown");

        verify(mockViewRepository, times(1)).findById(any(), any());
        verify(mockViewRepository, never()).delete(any(), any());
        verify(mockAuditService, never()).createPortalAuditLog(any(), eq(VIEW_UPDATED), any(), any(), any());
        verify(mockApiService, never()).deleteViewFromAPIs(eq("unknown"));
    }

    @Test
    public void shouldDeleteView() throws TechnicalException {
        when(mockViewRepository.findById("known", "DEFAULT")).thenReturn(Optional.of(new View()));

        viewService.delete("known");

        verify(mockViewRepository, times(1)).findById(eq("known"), eq("DEFAULT"));
        verify(mockViewRepository, times(1)).delete(eq("known"), eq("DEFAULT"));
        verify(mockAuditService, times(1)).createPortalAuditLog(any(), eq(VIEW_DELETED), any(), any(), any());
        verify(mockApiService, times(1)).deleteViewFromAPIs(eq("known"));
    }
}

