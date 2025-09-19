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
package io.gravitee.rest.api.management.rest;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.rest.mapper.ObjectMapperResolver;
import io.gravitee.rest.api.management.rest.resource.GraviteeManagementApplication;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class JerseySpringTest {

    protected static final String USER_NAME = "UnitTests";
    protected static final String ORGANIZATION = "fake-org";
    protected static final Principal PRINCIPAL = () -> USER_NAME;
    private JerseyTest _jerseyTest;

    private String orgBaseURL = "/organizations/DEFAULT";
    private String envBaseURL = orgBaseURL + "/environments/DEFAULT";
    protected HttpServletRequest httpServletRequest;

    protected abstract String contextPath();

    protected boolean followRedirect() {
        return true;
    }

    public HttpServletRequest httpServletRequest() {
        return httpServletRequest;
    }

    public final WebTarget envTarget() {
        return envTarget("");
    }

    public final WebTarget orgTarget() {
        return orgTarget("");
    }

    public final WebTarget envTarget(final String path) {
        return target(path, envBaseURL);
    }

    public final WebTarget orgTarget(final String path) {
        return target(path, orgBaseURL);
    }

    public final WebTarget rootTarget(final String path) {
        return target(path, "");
    }

    private final WebTarget target(final String path, final String baseURL) {
        return _jerseyTest.target(baseURL + "/" + contextPath() + path);
    }

    @Before
    public void setup() throws Exception {
        _jerseyTest.setUp();
    }

    @After
    public void tearDown() throws Exception {
        _jerseyTest.tearDown();
    }

    @Autowired
    public void setApplicationContext(final ApplicationContext context) {
        var followRedirect = this.followRedirect();

        _jerseyTest = new JerseyTest() {
            @Override
            protected Application configure() {
                // Find first available port.
                forceSet(TestProperties.CONTAINER_PORT, "0");

                ResourceConfig application = new GraviteeManagementApplication();

                application.property("contextConfig", context);
                decorate(application);

                return application;
            }

            @Override
            protected void configureClient(ClientConfig config) {
                super.configureClient(config);

                config.register(ObjectMapperResolver.class);
                config.register(MultiPartFeature.class);
                config.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
                config.property(ClientProperties.FOLLOW_REDIRECTS, followRedirect);
            }
        };
    }

    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(AuthenticationFilter.class);

        httpServletRequest = Mockito.spy(MockHttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        resourceConfig.register(
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(response).to(HttpServletResponse.class);
                    bind(httpServletRequest).to(HttpServletRequest.class);
                }
            }
        );
    }

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        UserDetails userDetails = new UserDetails(USER_NAME, "", Collections.emptyList());
                        userDetails.setOrganizationId(ORGANIZATION);

                        var principal = new UsernamePasswordAuthenticationToken(userDetails, new Object());
                        SecurityContextHolder.getContext().setAuthentication(principal);

                        return principal;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
                        return true;
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
