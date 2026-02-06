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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.use_case.DeleteClientCertificateUseCase;
import io.gravitee.apim.core.application_certificate.use_case.GetClientCertificateUseCase;
import io.gravitee.apim.core.application_certificate.use_case.UpdateClientCertificateUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.UpdateClientCertificate;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

public class ApplicationClientCertificateResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-application";
    private static final String CERT_ID = "my-cert-id";

    @Override
    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/certificates/" + CERT_ID;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        reset(getClientCertificateUseCase, updateClientCertificateUseCase, deleteClientCertificateUseCase);
    }

    @Test
    public void should_get_client_certificate() {
        ClientCertificate certificate = createClientCertificate(CERT_ID, "My Certificate");

        when(getClientCertificateUseCase.execute(any(GetClientCertificateUseCase.Input.class))).thenReturn(
            new GetClientCertificateUseCase.Output(certificate)
        );

        final Response response = envTarget().request().get();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ClientCertificate result = response.readEntity(ClientCertificate.class);
            soft.assertThat(result.id()).isEqualTo(CERT_ID);
            soft.assertThat(result.name()).isEqualTo("My Certificate");
        });
        verify(getClientCertificateUseCase).execute(any(GetClientCertificateUseCase.Input.class));
    }

    @Test
    public void should_return_404_when_certificate_not_found() {
        when(getClientCertificateUseCase.execute(any(GetClientCertificateUseCase.Input.class))).thenThrow(
            new ClientCertificateNotFoundException(CERT_ID)
        );

        final Response response = envTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void should_update_client_certificate() {
        UpdateClientCertificate updateRequest = new UpdateClientCertificate(
            "Updated Certificate Name",
            new Date(),
            new Date(System.currentTimeMillis() + 86400000)
        );

        ClientCertificate updated = createClientCertificate(CERT_ID, "Updated Certificate Name");

        when(updateClientCertificateUseCase.execute(any(UpdateClientCertificateUseCase.Input.class))).thenReturn(
            new UpdateClientCertificateUseCase.Output(updated)
        );

        final Response response = envTarget().request().put(Entity.json(updateRequest));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ClientCertificate result = response.readEntity(ClientCertificate.class);
            soft.assertThat(result.name()).isEqualTo("Updated Certificate Name");
        });
        verify(updateClientCertificateUseCase).execute(any(UpdateClientCertificateUseCase.Input.class));
    }

    @Test
    public void should_not_update_client_certificate_without_name() {
        UpdateClientCertificate updateRequest = new UpdateClientCertificate(null, new Date(), null);

        final Response response = envTarget().request().put(Entity.json(updateRequest));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void should_return_404_when_updating_non_existent_certificate() {
        UpdateClientCertificate updateRequest = new UpdateClientCertificate("Updated Certificate Name", null, null);

        when(updateClientCertificateUseCase.execute(any(UpdateClientCertificateUseCase.Input.class))).thenThrow(
            new ClientCertificateNotFoundException(CERT_ID)
        );

        final Response response = envTarget().request().put(Entity.json(updateRequest));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void should_delete_client_certificate() {
        doNothing().when(deleteClientCertificateUseCase).execute(any(DeleteClientCertificateUseCase.Input.class));

        final Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        verify(deleteClientCertificateUseCase).execute(any(DeleteClientCertificateUseCase.Input.class));
    }

    @Test
    public void should_return_404_when_deleting_non_existent_certificate() {
        doThrow(new ClientCertificateNotFoundException(CERT_ID))
            .when(deleteClientCertificateUseCase)
            .execute(any(DeleteClientCertificateUseCase.Input.class));

        final Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
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
            null
        );
    }
}
