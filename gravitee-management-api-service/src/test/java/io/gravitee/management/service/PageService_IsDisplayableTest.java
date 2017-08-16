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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.service.impl.PageServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_IsDisplayableTest {

    public static final String USERNAME = "johndoe";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private MembershipService membershipServiceMock;


    /**
     * *************************
     * Tests for published pages
     * *************************
     */
    @Test
    public void shouldBeDisplayablePublicApiAndPublishedPageAsUnauthenticated() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PUBLIC).when(apiEntityMock).getVisibility();

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, null);

        assertTrue(displayable);
        verify(membershipServiceMock, never()).getMember(any(), any(), any(), any());
    }

    @Test
    public void shouldBeDisplayablePublicApiAndPublishedPageAsAuthenticated() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PUBLIC).when(apiEntityMock).getVisibility();

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, USERNAME);

        assertTrue(displayable);
        verify(membershipServiceMock, never()).getMember(any(), any(), any(), any());
    }

    @Test
    public void shouldNotBeDisplayablePrivateApiAndPublishedPageAsUnauthenticated() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PRIVATE).when(apiEntityMock).getVisibility();

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, null);

        assertFalse(displayable);
        verify(membershipServiceMock, never()).getMember(any(), any(), any(), any());
    }

    @Test
    public void shouldNotBeDisplayablePrivateApiAndPublishedPageAsNotApiMember() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PRIVATE).when(apiEntityMock).getVisibility();
        doReturn(null).when(membershipServiceMock).getMember(eq(MembershipReferenceType.API), any(), eq(USERNAME), eq(RoleScope.API));

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, USERNAME);

        assertFalse(displayable);
        verify(membershipServiceMock, times(1)).getMember(eq(MembershipReferenceType.API), any(), eq(USERNAME), eq(RoleScope.API));
        verify(membershipServiceMock, never()).getMember(eq(MembershipReferenceType.GROUP), any(), any(), eq(RoleScope.API));
    }

    @Test
    public void shouldBeDisplayablePrivateApiAndPublishedPageAsApiMember() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PRIVATE).when(apiEntityMock).getVisibility();
        doReturn(mock(MemberEntity.class)).when(membershipServiceMock).getMember(eq(MembershipReferenceType.API), any(), eq(USERNAME), eq(RoleScope.API));

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, USERNAME);

        assertTrue(displayable);
        verify(membershipServiceMock, times(1)).getMember(eq(MembershipReferenceType.API), any(), eq(USERNAME), eq(RoleScope.API));
        verify(membershipServiceMock, never()).getMember(eq(MembershipReferenceType.GROUP), any(), any(), eq(RoleScope.API));
    }

    @Test
    public void shouldBeDisplayablePrivateApiAndPublishedPageAsGroupApiMember() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PRIVATE).when(apiEntityMock).getVisibility();
        when(apiEntityMock.getGroups()).thenReturn(Collections.singleton("groupid"));
        doReturn(null).when(membershipServiceMock).getMember(eq(MembershipReferenceType.API), any(), eq(USERNAME), eq(RoleScope.API));
        doReturn(mock(MemberEntity.class)).when(membershipServiceMock).getMember(eq(MembershipReferenceType.GROUP), any(), eq(USERNAME), eq(RoleScope.API));

        boolean displayable = pageService.isDisplayable(apiEntityMock, true, USERNAME);

        assertTrue(displayable);
        verify(membershipServiceMock, times(1)).getMember(eq(MembershipReferenceType.API), any(), eq(USERNAME), eq(RoleScope.API));
        verify(membershipServiceMock, times(1)).getMember(eq(MembershipReferenceType.GROUP), any(), eq(USERNAME), eq(RoleScope.API));
    }


    /**
     * *****************************
     * Tests for not published pages
     * *****************************
     */
    @Test
    public void shouldNotBeDisplayablePublicApiAndUnpublishedPageAsUnauthenticated() throws TechnicalException {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(Visibility.PUBLIC).when(apiEntityMock).getVisibility();

        boolean displayable = pageService.isDisplayable(apiEntityMock, false, null);

        assertFalse(displayable);
        verify(membershipServiceMock, never()).getMember(any(), any(), any(), any());
    }
}
