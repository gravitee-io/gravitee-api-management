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
package io.gravitee.rest.api.management.v2.rest.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.v2.rest.UserDetails;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.io.IOException;
import java.util.Collections;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeContextRequestFilterTest {

    protected GraviteeContextRequestFilter cut;

    @Mock
    protected ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

    @Mock
    protected SecurityContext securityContext = mock(SecurityContext.class);

    @Mock
    protected EnvironmentService environmentService = mock(EnvironmentService.class);

    MultivaluedHashMap<String, String> pathParameters;

    private static final String USER_NAME = "user";

    private static final String ENVIRONMENT_ID = "ENV_ID";

    private static final String ORGANIZATION_ID = "ORG_ID";

    @Before
    public void before() {
        cut = new GraviteeContextRequestFilter(environmentService);
        when(containerRequestContext.getSecurityContext()).thenReturn(securityContext);

        UserDetails userDetails = new UserDetails(USER_NAME, "", Collections.emptyList());
        userDetails.setOrganizationId(ORGANIZATION_ID);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(userDetails, new Object());

        when(securityContext.getUserPrincipal()).thenReturn(principal);

        UriInfo uriInfo = mock(UriInfo.class);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        pathParameters = new MultivaluedHashMap<>();
        when(uriInfo.getPathParameters()).thenReturn(pathParameters);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldFindOnlyOrgInSecurityContext() throws IOException {
        cut.filter(containerRequestContext);

        Assertions.assertThat(GraviteeContext.getCurrentEnvironment()).isNull();
        Assertions.assertThat(GraviteeContext.getCurrentOrganization()).isEqualTo(ORGANIZATION_ID);
    }

    @Test
    public void shouldAllowAnonymousUsers() throws IOException {
        when(securityContext.getUserPrincipal()).thenReturn(null);

        cut.filter(containerRequestContext);

        Assertions.assertThat(GraviteeContext.getCurrentEnvironment()).isNull();
        Assertions.assertThat(GraviteeContext.getCurrentOrganization()).isNull();
    }

    @Test
    public void shouldFindOnlyOrgInURL() throws IOException {
        pathParameters.put("orgId", Collections.singletonList(ORGANIZATION_ID));

        cut.filter(containerRequestContext);

        Assertions.assertThat(GraviteeContext.getCurrentEnvironment()).isNull();
        Assertions.assertThat(GraviteeContext.getCurrentOrganization()).isEqualTo(ORGANIZATION_ID);
    }

    @Test
    public void shouldFailIfNoOrganization() {
        UserDetails userDetails = new UserDetails(USER_NAME, "", Collections.emptyList());
        userDetails.setOrganizationId(null);

        var principal = new UsernamePasswordAuthenticationToken(userDetails, new Object());

        when(securityContext.getUserPrincipal()).thenReturn(principal);

        Assert.assertThrows(IllegalStateException.class, () -> cut.filter(containerRequestContext));
    }

    @Test
    public void shouldFailIfOrganizationParamDoesNotMatch() {
        pathParameters.put("orgId", Collections.singletonList("test-org"));

        Assert.assertThrows(BadRequestException.class, () -> cut.filter(containerRequestContext));
    }

    @Test
    public void shouldFindOrgandEnvInURL() throws IOException {
        pathParameters.put("orgId", Collections.singletonList(ORGANIZATION_ID));
        pathParameters.put("envId", Collections.singletonList("env-param"));

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId("env-param");
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION_ID, "env-param")).thenReturn(environmentEntity);
        cut.filter(containerRequestContext);

        Assertions.assertThat(GraviteeContext.getCurrentEnvironment()).isEqualTo("env-param");
        Assertions.assertThat(GraviteeContext.getCurrentOrganization()).isEqualTo(ORGANIZATION_ID);
    }

    @Test
    public void shouldFailIfEnvironmentServiceFails() {
        pathParameters.put("orgId", Collections.singletonList(ORGANIZATION_ID));
        pathParameters.put("envId", Collections.singletonList(ENVIRONMENT_ID));

        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION_ID, ENVIRONMENT_ID)).thenThrow(new IllegalStateException());

        Assert.assertThrows(IllegalStateException.class, () -> cut.filter(containerRequestContext));
    }

    @Test
    public void shouldFailIfNoEnvironmentFound() {
        pathParameters.put("orgId", Collections.singletonList(ORGANIZATION_ID));
        pathParameters.put("envId", Collections.singletonList(ENVIRONMENT_ID));

        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION_ID, ENVIRONMENT_ID))
            .thenThrow(new EnvironmentNotFoundException(ENVIRONMENT_ID));

        Assert.assertThrows(EnvironmentNotFoundException.class, () -> cut.filter(containerRequestContext));
    }
}
