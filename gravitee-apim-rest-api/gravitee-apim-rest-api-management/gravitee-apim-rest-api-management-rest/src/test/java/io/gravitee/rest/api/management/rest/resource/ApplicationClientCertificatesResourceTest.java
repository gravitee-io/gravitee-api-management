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
package io.gravitee.rest.api.management.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.model.common.Pageable;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ApplicationClientCertificatesResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-application";

    @Override
    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/certificates";
    }

    @Test
    public void should_list_client_certificates() {
        ClientCertificate cert1 = createClientCertificate("cert-1", "Certificate 1");
        ClientCertificate cert2 = createClientCertificate("cert-2", "Certificate 2");
        Page<ClientCertificate> page = new Page<>(List.of(cert1, cert2), 1, 2, 2);

        when(clientCertificateService.findByApplicationId(eq(APPLICATION_ID), any(Pageable.class))).thenReturn(page);

        final Response response = envTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        verify(clientCertificateService).findByApplicationId(eq(APPLICATION_ID), any(Pageable.class));
    }

    @Test
    public void should_create_client_certificate() {
        CreateClientCertificate createRequest = new CreateClientCertificate(
            "My Certificate",
            new Date(),
            null,
            "-----BEGIN CERTIFICATE-----\nMIIBkTCB+wIJAKHBfpE...\n-----END CERTIFICATE-----"
        );

        ClientCertificate created = createClientCertificate("new-cert-id", "My Certificate");

        when(clientCertificateService.create(eq(APPLICATION_ID), any(CreateClientCertificate.class))).thenReturn(created);

        final Response response = envTarget().request().post(Entity.json(createRequest));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
            soft.assertThat(response.getHeaderString("Location")).contains("new-cert-id");
        });
        verify(clientCertificateService).create(eq(APPLICATION_ID), any(CreateClientCertificate.class));
    }

    @Test
    public void should_not_create_client_certificate_without_name() {
        CreateClientCertificate createRequest = new CreateClientCertificate(
            null,
            null,
            null,
            "-----BEGIN CERTIFICATE-----\nMIIBkTCB+wIJAKHBfpE...\n-----END CERTIFICATE-----"
        );

        final Response response = envTarget().request().post(Entity.json(createRequest));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void should_not_create_client_certificate_without_certificate() {
        CreateClientCertificate createRequest = new CreateClientCertificate("My Certificate", null, null, null);

        final Response response = envTarget().request().post(Entity.json(createRequest));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
    }

    private ClientCertificate createClientCertificate(String id, String name) {
        return new ClientCertificate(
            id,
            null,
            APPLICATION_ID,
            name,
            null,
            null,
            new Date(),
            new Date(),
            "-----BEGIN CERTIFICATE-----\nMIIBkTCB+wIJAKHBfpE...\n-----END CERTIFICATE-----",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
