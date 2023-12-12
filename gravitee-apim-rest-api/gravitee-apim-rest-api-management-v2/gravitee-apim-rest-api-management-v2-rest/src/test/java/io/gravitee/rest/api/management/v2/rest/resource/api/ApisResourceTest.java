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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.core.model.ApiFixtures.aTcpApiV4;
import static io.gravitee.common.http.HttpStatusCode.ACCEPTED_202;
import static org.mockito.Mockito.doReturn;

import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiHosts;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiHostsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApisResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Autowired
    private ApiQueryServiceInMemory apiQueryServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    public void setup() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        doReturn(environment).when(environmentService).findById(ENVIRONMENT);
        doReturn(environment).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);
    }

    @Nested
    class verifyHosts {

        WebTarget verifyHostsTarget;

        @BeforeEach
        void setUp() {
            final Api tcpApi = aTcpApiV4().toBuilder().id("tcp-1").environmentId(ENVIRONMENT).build();
            apiQueryServiceInMemory.initWith(List.of(tcpApi));

            verifyHostsTarget = rootTarget().path("_verify/hosts");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void should_catch_invalid_host_exception(List<String> hosts) {
            // given
            VerifyApiHosts verifyApiHosts = new VerifyApiHosts();
            verifyApiHosts.setApiId("tcp-1");
            verifyApiHosts.setHosts(hosts);

            // when
            final Response response = verifyHostsTarget.request().post(Entity.json(verifyApiHosts));

            // then
            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(VerifyApiHostsResponse.class)
                .isEqualTo(
                    VerifyApiHostsResponse.builder().ok(false).reason("At least one host is required for the TCP listener.").build()
                );
        }

        @Test
        void should_catch_duplicated_host_exception() {
            // given
            VerifyApiHosts verifyApiHosts = new VerifyApiHosts();
            verifyApiHosts.setApiId("tcp-1");
            verifyApiHosts.setHosts(List.of("tcp.example.com", "tcp.example.com", "tcp-2.example.com", "tcp-2.example.com"));

            // when
            final Response response = verifyHostsTarget.request().post(Entity.json(verifyApiHosts));

            // then
            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(VerifyApiHostsResponse.class)
                .isEqualTo(
                    VerifyApiHostsResponse
                        .builder()
                        .ok(false)
                        .reason("Duplicated hosts detected: 'tcp.example.com, tcp-2.example.com'. Please ensure each host is unique.")
                        .build()
                );
        }

        @Test
        void should_catch_host_already_exist_exception() {
            // given
            VerifyApiHosts verifyApiHosts = new VerifyApiHosts();
            verifyApiHosts.setApiId("tcp-2");
            verifyApiHosts.setHosts(List.of("foo.example.com", "tcp.example.com", "bar.example.com"));

            // when
            final Response response = verifyHostsTarget.request().post(Entity.json(verifyApiHosts));

            // then
            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(VerifyApiHostsResponse.class)
                .isEqualTo(
                    VerifyApiHostsResponse.builder().ok(false).reason("Hosts [foo.example.com, bar.example.com] already exists").build()
                );
        }

        @Test
        void should_validate_hosts() {
            // given
            VerifyApiHosts verifyApiHosts = new VerifyApiHosts();
            verifyApiHosts.setApiId("tcp-2");
            verifyApiHosts.setHosts(List.of("tcp-1.example.com", "tcp-2.example.com"));

            // when
            final Response response = verifyHostsTarget.request().post(Entity.json(verifyApiHosts));

            // then
            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(VerifyApiHostsResponse.class)
                .isEqualTo(VerifyApiHostsResponse.builder().ok(true).build());
        }
    }
}
