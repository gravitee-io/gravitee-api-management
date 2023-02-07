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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.impl.ApiCategoryServiceImpl;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiCategoryService_DeleteCategoryTest {

    ApiCategoryService apiCategoryService;

    @Spy
    private ApiRepository apiRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Before
    public void setUp() throws Exception {
        apiCategoryService =
            new ApiCategoryServiceImpl(
                apiRepository,
                categoryService,
                apiNotificationService,
                auditService,
                membershipService,
                roleService
            );
    }

    @Test
    public void shouldDeleteApiCategoryReferences() throws TechnicalException {
        final String categoryId = UuidString.generateRandom();

        Api firstOrphan = new Api();
        firstOrphan.setId(UuidString.generateRandom());
        firstOrphan.setCategories(new HashSet<>(Set.of(categoryId)));

        Api secondOrphan = new Api();
        secondOrphan.setId(UuidString.generateRandom());
        secondOrphan.setCategories(new HashSet<>(Set.of(UuidString.generateRandom(), categoryId)));

        when(apiRepository.search(new ApiCriteria.Builder().category(categoryId).build(), null, ApiFieldFilter.allFields()))
            .thenReturn(Stream.of(firstOrphan, secondOrphan));

        apiCategoryService.deleteCategoryFromAPIs(GraviteeContext.getExecutionContext(), categoryId);

        verify(apiRepository, times(1)).update(firstOrphan);
        verify(apiRepository, times(1)).update(firstOrphan);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(GraviteeContext.getExecutionContext(), firstOrphan);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(GraviteeContext.getExecutionContext(), secondOrphan);
        verify(auditService, times(1))
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(firstOrphan.getId()),
                eq(Collections.emptyMap()),
                eq(Api.AuditEvent.API_UPDATED),
                any(),
                any(),
                any()
            );
        verify(auditService, times(1))
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(secondOrphan.getId()),
                eq(Collections.emptyMap()),
                eq(Api.AuditEvent.API_UPDATED),
                any(),
                any(),
                any()
            );

        assertEquals(0, firstOrphan.getCategories().size());
        assertEquals(1, secondOrphan.getCategories().size());
    }
}
