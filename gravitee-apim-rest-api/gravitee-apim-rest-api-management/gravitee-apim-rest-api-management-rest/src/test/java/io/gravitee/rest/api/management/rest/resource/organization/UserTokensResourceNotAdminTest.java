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
package io.gravitee.rest.api.management.rest.resource.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.security.Principal;
import java.util.Date;
import javax.annotation.Priority;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserTokensResourceNotAdminTest extends AbstractResourceTest {

    public static final String USER_ID = "user-id";
    public static final String TOKEN_ID = "token-id";

    @Override
    protected String contextPath() {
        return "users/" + USER_ID + "/tokens";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(UserTokensResourceNotAdminTest.AuthenticationFilter.class);
    }

    @Before
    public void setUp() {
        reset(tokenService);
    }

    @Test
    public void shouldNotGetTokens() {
        final Response response = envTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(403);
        verify(tokenService, never()).findByUser(USER_ID);
    }

    @Test
    public void shouldNotCreateToken() {
        NewTokenEntity newToken = new NewTokenEntity();
        final Response response = envTarget().request().post(Entity.json(newToken));

        assertThat(response.getStatus()).isEqualTo(403);
        verify(tokenService, never()).create(GraviteeContext.getExecutionContext(), newToken, USER_ID);
    }

    @Test
    public void shouldNotRevokeToken() {
        final Response response = envTarget().path(TOKEN_ID).request().delete();

        assertThat(response.getStatus()).isEqualTo(403);
        verify(tokenService, never()).revoke(GraviteeContext.getExecutionContext(), TOKEN_ID);
    }

    private TokenEntity fakeToken(String name) {
        final TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setName(name);
        tokenEntity.setCreatedAt(new Date());
        return tokenEntity;
    }

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return () -> USER_NAME;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return true;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "BASIC";
                    }
                }
            );
        }
    }
}
