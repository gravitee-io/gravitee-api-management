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
package io.gravitee.gamma.module.platform.infra.service_provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.am.sdk.management.api.DefaultApi;
import io.gravitee.am.sdk.management.api.DomainApi;
import io.gravitee.am.sdk.management.model.Domain;
import io.gravitee.am.sdk.management.model.DomainPage;
import io.gravitee.am.sdk.management.model.Entrypoint;
import io.gravitee.am.sdk.management.model.Environment;
import io.gravitee.gamma.module.platform.infra.service_provider.AmSdkClientFactory.AmApis;
import io.vertx.core.Future;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The APIM org is only the key the connection is stored under; AM must always be addressed with the
 * connection's own organization. When the two differ (any cloud org) AM answers 403 to the APIM one.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmSdkDirectoryClientTest {

    private static final String APIM_ORG = "aca99aca-86ef-4cf8-a99a-ca86efacf876";
    private static final String AM_ORG = "DEFAULT";
    private static final String AM_ENV = "am-env";

    private DefaultApi defaults;
    private DomainApi domains;
    private AmSdkDirectoryClient client;

    @BeforeEach
    void setUp() {
        defaults = mock(DefaultApi.class);
        domains = mock(DomainApi.class);
        AmSdkClientFactory clientFactory = mock(AmSdkClientFactory.class);
        when(clientFactory.forOrg(APIM_ORG)).thenReturn(new AmApis(null, AM_ORG, defaults, domains));
        client = new AmSdkDirectoryClient(clientFactory);
    }

    @Test
    void should_list_environments_from_the_am_organization() {
        when(defaults.listEnvironments(AM_ORG)).thenReturn(Future.succeededFuture(List.of(environment("env-1", "Env one"))));

        var environments = client.listEnvironments(APIM_ORG);

        assertThat(environments)
            .singleElement()
            .satisfies(env -> assertThat(env.id()).isEqualTo("env-1"));
    }

    @Test
    void should_list_domains_from_the_am_organization() {
        when(domains.listDomains(AM_ORG, AM_ENV, null, null, "*gamma*")).thenReturn(
            Future.succeededFuture(new DomainPage().data(List.of(domain())))
        );

        var found = client.listDomains(APIM_ORG, AM_ENV, "gamma");

        assertThat(found)
            .singleElement()
            .satisfies(d -> assertThat(d.id()).isEqualTo("domain-1"));
    }

    @Test
    void should_get_a_domain_from_the_am_organization() {
        when(domains.findDomain(AM_ORG, AM_ENV, "domain-1")).thenReturn(Future.succeededFuture(domain()));

        var found = client.getDomain(APIM_ORG, AM_ENV, "domain-1");

        assertThat(found.id()).isEqualTo("domain-1");
    }

    @Test
    void should_list_domain_entrypoints_from_the_am_organization() {
        when(domains.getDomainEntrypoints(AM_ORG, AM_ENV, "domain-1")).thenReturn(
            Future.succeededFuture(List.of(new Entrypoint().id("ep-1").name("Default").url("https://gw").defaultEntrypoint(true)))
        );

        var entrypoints = client.listDomainEntrypoints(APIM_ORG, AM_ENV, "domain-1");

        assertThat(entrypoints)
            .singleElement()
            .satisfies(ep -> {
                assertThat(ep.id()).isEqualTo("ep-1");
                assertThat(ep.defaultEntrypoint()).isTrue();
            });
    }

    private static Environment environment(String id, String name) {
        return new Environment().id(id).name(name);
    }

    private static Domain domain() {
        return new Domain().id("domain-1").name("Domain one").hrid("domain-one");
    }
}
