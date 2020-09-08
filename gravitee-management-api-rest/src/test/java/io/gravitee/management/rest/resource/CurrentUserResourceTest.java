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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.UserEntity;
import java.util.Collections;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.AfterClass;
import org.mockito.Mockito;

/**
 * Unit tests for <code>CurrentUserResource</code> class.
 * @author Raphaël CALABRO (ddaeke-github [at] yahoo [dot] fr)
 * @author GraviteeSource Team
 */
public class CurrentUserResourceTest extends AbstractResourceTest {

    private static final String ID = "040f6a20-9fc2-429f-8f6a-209fc2629f8d";

    @AfterClass
    public static void afterClass() {
        // Clean up Spring security context.
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String contextPath() {
        return "/user";
    }

    @Test
    public void shouldBeAbleToGetCurrentUser() {
        Mockito.reset(userService);

        final UserDetails userDetails = new UserDetails(USER_NAME, "PASSWORD", Collections.emptyList());
        assertThat(userDetails.getPassword()).isNotNull();

        setCurrentUserDetails(userDetails);

        final Response response = target().request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }

    @Test
    public void shouldBeAbleToGetCurrentUserEvenIfItsPasswordIsErased() {
        Mockito.reset(userService);

        final UserDetails userDetails = new UserDetails(USER_NAME, "PASSWORD", Collections.emptyList());
        userDetails.eraseCredentials();
        assertThat(userDetails.getPassword()).isNull();

        setCurrentUserDetails(userDetails);

        final Response response = target().request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }

    @Test
    public void shouldBeAbleToDeleteCurrentUser() {
        Mockito.reset(userService);

        final Authentication authentication = mock(Authentication.class);
        final UserDetails userDetails = new UserDetails(USER_NAME, "PASSWORD", Collections.emptyList());

        when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        final Response response = target().request().delete();

        verify(userService, times(1)).delete(USER_NAME);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
    }

    private void setCurrentUserDetails(final UserDetails userDetails) {
        final Authentication authentication = mock(Authentication.class);
        final UserEntity userEntity = new UserEntity();
        userEntity.setId(ID);
        userEntity.setRoles(Collections.emptySet());

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userService.findByIdWithRoles(USER_NAME)).thenReturn(userEntity);

        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

}
