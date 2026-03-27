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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.core.application_certificate.use_case.DeleteClientCertificateUseCase;
import io.gravitee.apim.core.application_certificate.use_case.GetClientCertificateUseCase;
import io.gravitee.apim.core.application_certificate.use_case.UpdateClientCertificateUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.portal.rest.model.PortalClientCertificate;
import io.gravitee.rest.api.portal.rest.model.UpdatePortalClientCertificateInput;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PortalApplicationClientCertificateResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-application";
    private static final String CERT_ID = "my-cert-id";

    @Override
    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/certificates/" + CERT_ID;
    }

    @BeforeEach
    public void init() {
        resetAllMocks();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void should_get_client_certificate() {
        var certificate = createClientCertificate(CERT_ID, "My Certificate");

        when(getClientCertificateUseCase.execute(any(GetClientCertificateUseCase.Input.class))).thenReturn(
            new GetClientCertificateUseCase.Output(certificate)
        );

        final Response response = target().request().get();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            var result = response.readEntity(PortalClientCertificate.class);
            soft.assertThat(result.getId()).isEqualTo(CERT_ID);
            soft.assertThat(result.getName()).isEqualTo("My Certificate");
        });
        verify(getClientCertificateUseCase).execute(any(GetClientCertificateUseCase.Input.class));
    }

    @Test
    public void should_return_404_when_certificate_not_found() {
        when(getClientCertificateUseCase.execute(any(GetClientCertificateUseCase.Input.class))).thenThrow(
            new ClientCertificateNotFoundException(CERT_ID)
        );

        final Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void should_update_client_certificate() {
        var updateRequest = new UpdatePortalClientCertificateInput();
        updateRequest.setName("Updated Certificate Name");

        ClientCertificate updated = createClientCertificate(CERT_ID, "Updated Certificate Name");

        when(updateClientCertificateUseCase.execute(any(UpdateClientCertificateUseCase.Input.class))).thenReturn(
            new UpdateClientCertificateUseCase.Output(updated)
        );

        final Response response = target().request().put(Entity.json(updateRequest));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            var result = response.readEntity(PortalClientCertificate.class);
            soft.assertThat(result.getName()).isEqualTo("Updated Certificate Name");
        });
        verify(updateClientCertificateUseCase).execute(any(UpdateClientCertificateUseCase.Input.class));
    }

    @Test
    public void should_return_404_when_updating_non_existent_certificate() {
        var updateRequest = new UpdatePortalClientCertificateInput();
        updateRequest.setName("Updated Certificate Name");

        when(updateClientCertificateUseCase.execute(any(UpdateClientCertificateUseCase.Input.class))).thenThrow(
            new ClientCertificateNotFoundException(CERT_ID)
        );

        final Response response = target().request().put(Entity.json(updateRequest));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void should_delete_client_certificate() {
        doNothing().when(deleteClientCertificateUseCase).execute(any(DeleteClientCertificateUseCase.Input.class));

        final Response response = target().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        verify(deleteClientCertificateUseCase).execute(any(DeleteClientCertificateUseCase.Input.class));
    }

    @Test
    public void should_return_404_when_deleting_non_existent_certificate() {
        doThrow(new ClientCertificateNotFoundException(CERT_ID))
            .when(deleteClientCertificateUseCase)
            .execute(any(DeleteClientCertificateUseCase.Input.class));

        final Response response = target().request().delete();

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
            ClientCertificateStatus.ACTIVE
        );
    }
}
