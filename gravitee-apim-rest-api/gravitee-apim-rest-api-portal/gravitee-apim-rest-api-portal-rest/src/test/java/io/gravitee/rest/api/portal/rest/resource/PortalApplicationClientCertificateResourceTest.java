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
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.core.application_certificate.use_case.GetClientCertificateUseCase;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.portal.rest.model.PortalClientCertificate;
import io.gravitee.rest.api.portal.rest.model.UpdatePortalClientCertificateInput;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PortalApplicationClientCertificateResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-application";
    private static final String CERT_ID = "my-cert-id";
    private static final String PLAN_ID = "plan-id";
    private static final String API_ID = "api-id";

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
        var certificate = buildCertificate(CERT_ID, "My Certificate");
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
        clientCertificateService.initWith(List.of(buildCertificate(CERT_ID, "Original Name")));
        var updateRequest = new UpdatePortalClientCertificateInput();
        updateRequest.setName("Updated Certificate Name");
        final Response response = target().request().put(Entity.json(updateRequest));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            var result = response.readEntity(PortalClientCertificate.class);
            soft.assertThat(result.getName()).isEqualTo("Updated Certificate Name");
        });
        assertThat(clientCertificateService.storage().getFirst().name()).isEqualTo("Updated Certificate Name");
    }

    @Test
    public void should_return_400_when_updating_certificate_with_invalid_dates() {
        clientCertificateService.initWith(List.of(buildCertificate(CERT_ID, "My Certificate")));
        var updateRequest = new UpdatePortalClientCertificateInput();
        updateRequest.setName("My Certificate");
        updateRequest.setStartsAt(OffsetDateTime.now().plus(2, ChronoUnit.DAYS));
        updateRequest.setEndsAt(OffsetDateTime.now().plus(1, ChronoUnit.DAYS));
        final Response response = target().request().put(Entity.json(updateRequest));
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void should_return_404_when_updating_non_existent_certificate() {
        var updateRequest = new UpdatePortalClientCertificateInput();
        updateRequest.setName("Updated Certificate Name");
        final Response response = target().request().put(Entity.json(updateRequest));
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void should_delete_client_certificate() {
        clientCertificateService.initWith(List.of(buildCertificate(CERT_ID, "My Certificate")));
        final Response response = target().request().delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        assertThat(clientCertificateService.storage()).isEmpty();
    }

    @Test
    public void should_return_400_when_deleting_last_active_certificate_with_mtls_subscriptions() {
        clientCertificateService.initWith(List.of(buildCertificate(CERT_ID, "My Certificate")));
        planCrudService.initWith(List.of(buildMtlsPlan()));
        subscriptionCrudService.initWith(List.of(buildAcceptedSubscription()));
        final Response response = target().request().delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(clientCertificateService.storage()).hasSize(1);
    }

    @Test
    public void should_return_404_when_deleting_non_existent_certificate() {
        final Response response = target().request().delete();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    private ClientCertificate buildCertificate(String id, String name) {
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

    private Plan buildMtlsPlan() {
        var planDefinition = io.gravitee.definition.model.v4.plan.Plan.builder()
            .id(PLAN_ID)
            .security(PlanSecurity.builder().type(PlanSecurityType.MTLS.name()).build())
            .build();
        return Plan.builder()
            .id(PLAN_ID)
            .apiId(API_ID)
            .definitionVersion(DefinitionVersion.V4)
            .planDefinitionHttpV4(planDefinition)
            .build();
    }

    private SubscriptionEntity buildAcceptedSubscription() {
        var now = ZonedDateTime.now();
        return SubscriptionEntity.builder()
            .id("sub-1")
            .applicationId(APPLICATION_ID)
            .planId(PLAN_ID)
            .apiId(API_ID)
            .status(SubscriptionEntity.Status.ACCEPTED)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
