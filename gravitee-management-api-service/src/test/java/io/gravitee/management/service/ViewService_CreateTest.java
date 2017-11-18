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

import io.gravitee.management.model.NewViewEntity;
import io.gravitee.management.model.ViewEntity;
import io.gravitee.management.service.exceptions.DuplicateViewNameException;
import io.gravitee.management.service.impl.ViewServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_CREATED;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ViewService_CreateTest {

    @InjectMocks
    private ViewServiceImpl viewService = new ViewServiceImpl();

    @Mock
    private ViewRepository mockViewRepository;

    @Mock
    private AuditService mockAuditService;


    @Test
    public void shouldAcceptEmptyList() throws TechnicalException {
        List<ViewEntity> views = viewService.create(Collections.emptyList());

        assertNotNull("result is null", views);
        assertTrue("result is not empty", views.isEmpty());
        verify(mockAuditService, never()).createPortalAuditLog(any(), any(), any(), any(), any());
        verify(mockViewRepository, never()).create(any());
    }

    @Test
    public void shouldCreate2Views() throws TechnicalException {
        NewViewEntity v1 = new NewViewEntity();
        v1.setName("v1");
        NewViewEntity v2 = new NewViewEntity();
        v2.setName("v2");
        when(mockViewRepository.create(any())).thenReturn(new View());

        List<ViewEntity> views = viewService.create(Arrays.asList(v1, v2));

        assertNotNull("result is null", views);
        assertFalse("result is empty", views.isEmpty());
        assertEquals("result size is 2", 2, views.size());
        verify(mockAuditService, times(2)).createPortalAuditLog(any(), eq(VIEW_CREATED), any(), isNull(), any());
        verify(mockViewRepository, times(1)).create(argThat(new ArgumentMatcher<View>() {
            @Override
            public boolean matches(Object arg) {
                return arg instanceof View && ((View)arg).getName().equals("v1");
            }
        }));
        verify(mockViewRepository, times(1)).create(argThat(new ArgumentMatcher<View>() {
            @Override
            public boolean matches(Object arg) {
                return arg instanceof View && ((View)arg).getName().equals("v2");
            }
        }));
    }

    @Test(expected = DuplicateViewNameException.class)
    public void shouldNotCreateExistingView() throws TechnicalException {
        View v1 = new View();
        NewViewEntity nv1 = new NewViewEntity();
        v1.setName("v1");
        nv1.setName("v1");
        when(mockViewRepository.findAll()).thenReturn(Collections.singleton(v1));

        try {
            viewService.create(singletonList(nv1));
        } catch(DuplicateViewNameException e) {
            verify(mockViewRepository, never()).create(any());
            throw e;
        }
        Assert.fail("should throw DuplicateViewNameException");
    }
}
