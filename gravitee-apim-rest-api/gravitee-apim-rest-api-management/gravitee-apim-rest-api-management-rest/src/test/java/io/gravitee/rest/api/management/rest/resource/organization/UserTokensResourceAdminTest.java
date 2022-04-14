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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.NewTokenEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Date;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserTokensResourceAdminTest extends AbstractResourceTest {

    public static final String USER_ID = "user-id";
    public static final String TOKEN_ID = "token-id";

    @Override
    protected String contextPath() {
        return "users/" + USER_ID + "/tokens";
    }

    @Before
    public void setUp() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        reset(tokenService);
    }

    @Test
    public void shouldGetTokens() {
        when(tokenService.findByUser(USER_ID)).thenReturn(List.of(fakeToken("token1"), fakeToken("token2")));

        final Response response = envTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        final List<TokenEntity> tokenEntities = response.readEntity(new GenericType<List<TokenEntity>>() {});
        assertThat(tokenEntities).hasSize(2);
    }

    @Test
    public void shouldNotCreateTokenWhenNoName() {
        NewTokenEntity newToken = new NewTokenEntity();

        when(tokenService.create(GraviteeContext.getExecutionContext(), newToken, USER_ID)).thenReturn(new TokenEntity());
        final Response response = envTarget().request().post(Entity.json(newToken));

        assertThat(response.getStatus()).isEqualTo(400);
        verify(tokenService, never()).create(GraviteeContext.getExecutionContext(), newToken, USER_ID);
    }

    @Test
    public void shouldNotCreateTokenWhenNameTooShort() {
        NewTokenEntity newToken = new NewTokenEntity();
        newToken.setName("");

        when(tokenService.create(GraviteeContext.getExecutionContext(), newToken, USER_ID)).thenReturn(new TokenEntity());
        final Response response = envTarget().request().post(Entity.json(newToken));

        assertThat(response.getStatus()).isEqualTo(400);
        verify(tokenService, never()).create(GraviteeContext.getExecutionContext(), newToken, USER_ID);
    }

    @Test
    public void shouldNotCreateTokenWhenNameTooLong() {
        NewTokenEntity newToken = new NewTokenEntity();
        newToken.setName("It's a name greater than 64 chars in order to test the validation part");

        when(tokenService.create(GraviteeContext.getExecutionContext(), newToken, USER_ID)).thenReturn(new TokenEntity());
        final Response response = envTarget().request().post(Entity.json(newToken));

        assertThat(response.getStatus()).isEqualTo(400);
        verify(tokenService, never()).create(GraviteeContext.getExecutionContext(), newToken, USER_ID);
    }

    @Test
    public void shouldCreateToken() {
        NewTokenEntity newToken = new NewTokenEntity();
        newToken.setName("My Token");

        when(tokenService.create(GraviteeContext.getExecutionContext(), newToken, USER_ID)).thenReturn(new TokenEntity());
        final Response response = envTarget().request().post(Entity.json(newToken));

        assertThat(response.getStatus()).isEqualTo(201);
        verify(tokenService, times(1)).create(GraviteeContext.getExecutionContext(), newToken, USER_ID);
    }

    @Test
    public void shouldRevokeToken() {
        final Response response = envTarget().path(TOKEN_ID).request().delete();

        assertThat(response.getStatus()).isEqualTo(204);
        verify(tokenService, times(1)).revoke(GraviteeContext.getExecutionContext(), TOKEN_ID);
    }

    private TokenEntity fakeToken(String name) {
        final TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setName(name);
        tokenEntity.setCreatedAt(new Date());
        return tokenEntity;
    }
}
