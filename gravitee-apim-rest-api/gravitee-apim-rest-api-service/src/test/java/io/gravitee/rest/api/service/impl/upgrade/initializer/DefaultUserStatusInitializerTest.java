/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultUserStatusInitializerTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserService userService;

    @Mock
    Page<UserEntity> userPage;

    @InjectMocks
    private final DefaultUserStatusInitializer initializer = new DefaultUserStatusInitializer();

    @Test
    public void shouldUpdateUserStatus() throws TechnicalException {
        when(organizationRepository.findAll()).thenReturn(Set.of(organization()));
        when(userService.search(any(ExecutionContext.class), any(UserCriteria.class), any())).thenReturn(userPage);
        when(userPage.getContent()).thenReturn(List.of(new UserEntity()));
        initializer.initialize();
        verify(userService, times(1)).update(
            any(ExecutionContext.class),
            any(),
            argThat(user -> user.getStatus().equals(UserStatus.ACTIVE.name()))
        );
    }

    @Test
    public void shouldNotUpdateUserStatus() throws TechnicalException {
        when(organizationRepository.findAll()).thenReturn(Set.of(organization()));
        when(userService.search(any(ExecutionContext.class), any(UserCriteria.class), any())).thenReturn(userPage);
        when(userPage.getContent()).thenReturn(List.of(activeUser()));
        initializer.initialize();
        verify(userService, never()).update(any(ExecutionContext.class), any(), any());
    }

    private static UserEntity activeUser() {
        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE.name());
        return userEntity;
    }

    private static Organization organization() {
        Organization organization = new Organization();
        organization.setId("DEFAULT");
        return organization;
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(InitializerOrder.DEFAULT_USER_STATUS_INITIALIZER, initializer.getOrder());
    }
}
