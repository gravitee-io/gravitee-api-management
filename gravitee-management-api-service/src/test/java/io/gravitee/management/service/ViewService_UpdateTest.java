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

import io.gravitee.management.model.UpdateViewEntity;
import io.gravitee.management.model.ViewEntity;
import io.gravitee.management.service.impl.ViewServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_UPDATED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ViewService_UpdateTest {

    @InjectMocks
    private ViewServiceImpl viewService = new ViewServiceImpl();

    @Mock
    private ViewRepository mockViewRepository;

    @Mock
    private AuditService mockAuditService;

    @Test
    public void shouldDoNothingWithEmptyList() throws TechnicalException {
        List<ViewEntity> list = viewService.update(emptyList());

        assertTrue(list.isEmpty());
        verify(mockViewRepository, never()).findById(any());
        verify(mockViewRepository, never()).update(any());
        verify(mockAuditService, never()).createPortalAuditLog(any(), eq(VIEW_UPDATED), any(), any(), any());
    }

    @Test
    public void shouldNotUpdateUnknownView() throws TechnicalException {
        UpdateViewEntity mockView = mock(UpdateViewEntity.class);
        when(mockView.getId()).thenReturn("unknown");
        when(mockViewRepository.findById("unknown")).thenReturn(Optional.empty());

        List<ViewEntity> list = viewService.update(singletonList(mockView));

        assertTrue(list.isEmpty());
        verify(mockViewRepository, times(1)).findById(any());
        verify(mockViewRepository, never()).update(any());
        verify(mockAuditService, never()).createPortalAuditLog(any(), eq(VIEW_UPDATED), any(), any(), any());
    }

    @Test
    public void shouldUpdateView() throws TechnicalException {
        UpdateViewEntity mockView = mock(UpdateViewEntity.class);
        when(mockView.getId()).thenReturn("known");
        when(mockViewRepository.findById("known")).thenReturn(Optional.of(new View()));
        View updatedView = mock(View.class);
        when(updatedView.getId()).thenReturn("view-id");
        when(updatedView.getName()).thenReturn("view-name");
        when(updatedView.getDescription()).thenReturn("view-description");
        when(updatedView.isDefaultView()).thenReturn(true);
        when(updatedView.getOrder()).thenReturn(1);
        when(updatedView.isHidden()).thenReturn(true);
        when(updatedView.getUpdatedAt()).thenReturn(new Date(1234567890L));
        when(updatedView.getCreatedAt()).thenReturn(new Date(9876543210L));
        when(mockViewRepository.update(any())).thenReturn(updatedView);

        List<ViewEntity> list = viewService.update(singletonList(mockView));

        assertFalse(list.isEmpty());
        assertEquals("one element", 1, list.size());
        assertEquals("Id", "view-id", list.get(0).getId());
        assertEquals("Name", "view-name", list.get(0).getName());
        assertEquals("Description", "view-description", list.get(0).getDescription());
        assertEquals("Total APIs", 0, list.get(0).getTotalApis());
        assertEquals("default View", true, list.get(0).isDefaultView());
        assertEquals("Order", 1, list.get(0).getOrder());
        assertEquals("Hidden", true, list.get(0).isHidden());
        assertEquals("UpdatedAt", new Date(1234567890L), list.get(0).getUpdatedAt());
        assertEquals("CreatedAt", new Date(9876543210L), list.get(0).getCreatedAt());
        verify(mockViewRepository, times(1)).findById(any());
        verify(mockViewRepository, times(1)).update(any());
        verify(mockAuditService, times(1)).createPortalAuditLog(any(), eq(VIEW_UPDATED), any(), any(), any());

    }
}

