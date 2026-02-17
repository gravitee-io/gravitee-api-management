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
package io.gravitee.apim.infra.crud_service.application_certificates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientCertificateRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAlreadyUsedException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAuthorityException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateDateBoundsInvalidException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateEmptyException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCertificateCrudServiceImplTest {

    private static final String CERTIFICATE_ID = "cert-id";
    private static final String APPLICATION_ID = "app-id";
    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";

    private static final String PEM_CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIIFxjCCA64CCQD9kAnHVVL02TANBgkqhkiG9w0BAQsFADCBozEsMCoGCSqGSIb3
        DQEJARYddW5pdC50ZXN0c0BncmF2aXRlZXNvdXJjZS5jb20xEzARBgNVBAMMCnVu
        aXQtdGVzdHMxFzAVBgNVBAsMDkdyYXZpdGVlU291cmNlMRcwFQYDVQQKDA5HcmF2
        aXRlZVNvdXJjZTEOMAwGA1UEBwwFTGlsbGUxDzANBgNVBAgMBkZyYW5jZTELMAkG
        A1UEBhMCRlIwIBcNMjExMDE5MTUyMDQxWhgPMjEyMTA5MjUxNTIwNDFaMIGjMSww
        KgYJKoZIhvcNAQkBFh11bml0LnRlc3RzQGdyYXZpdGVlc291cmNlLmNvbTETMBEG
        A1UEAwwKdW5pdC10ZXN0czEXMBUGA1UECwwOR3Jhdml0ZWVTb3VyY2UxFzAVBgNV
        BAoMDkdyYXZpdGVlU291cmNlMQ4wDAYDVQQHDAVMaWxsZTEPMA0GA1UECAwGRnJh
        bmNlMQswCQYDVQQGEwJGUjCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIB
        AOKxBeF33XOd5sVaHbavIGFU+DMTX+cqTbRiJQJqAlrrDeuPQ3YEfga7hpHHB3ev
        OjunNCBJp4p/6VsBhylqcqd8KU+xqQ/wvNsqzp/50ssMkud+0sbPFjjjxM1rDI9X
        JVCqGqa15jlKfylcOOggH6KAOugM4BquBjeTRH0mGv2MBgZvtKHAieW0gzPslXxp
        UZZZ+gvvSSLo7NkAv7awWKSoV+yMlXma0yX0ygAj14EK1AxhFLZFgWDm8Ex919ry
        rbcPV6tqUHjw7Us8cy8p/pqftOUnwyRQ4LmaSdqwESZmdU+GXNXq22sAB6rX0G7u
        tXmoXVwQVlD8kEb79JbbIEOfPvLATyr8VStCK5dSXyc/JuzDo7QCquQUdrGpWrSy
        wdKKbCbOWDStakmBTEkgB0Bqg6yWFrHjgj+rzNeWFvIoZA+sLV2UCrlhDQ8BUV9O
        PMdgGBMKu4TrdEezt1NqDHjvThC3c6quxixxmaO/K7YPncVzguypijw7U7yl8CkG
        DlUJ+rPddEgsQCf+1E6z/xIeh8sCEdLm6TN80Dsw1yTdwzhRO9KvVY/gjE/ZaUYL
        g8Z0Htjq6vvnMwvr4C/8ykRk9oMYlv3o52pXQEcsbiZYm7LCTwgCs6k7KEiaHUze
        ySEqlkqFC8PG2GzCC6dM50xYktbcmwC+mep7c6bTAsexAgMBAAEwDQYJKoZIhvcN
        AQELBQADggIBAIHpb9solYTIPszzgvw0S6BVBAGzARDNDSi/jj+4KXKlKxYvVvq+
        bTX7YE6rC/wFGpyCjwfoWzzIrfLiIcmVTfu1o13Y/B8IEP4WyiAYrGszLqbjy1wM
        cyfwaxYpP/XfIQgcP5idI6kAA7hbGrFrLijIcdfYhh4tr6dsjD81uNrsVhp+JcAV
        CPv2o5YeRSMFUJrImAU5s73yX/x6fb2nCUR6PIMiPm9gveIAuY2+L12NzIJUugwN
        EZjqCeOr52f/yDuA+pAvVCGnZSSdkVWUh02ZsPxM4TiRzmxSkM5ODb59XWHeoFT1
        yvKA2F7+WFAL2R8BhBoVlBp1hug33Mrsix7L6yG4G9Ljss9Y0pzEd4B+IFGbpMZN
        R4dqZGpKS0aiStnvnurXBVWwIcJ3kCaAl2OgXZO5ivi+iNIx8e5qtXqDCnnlpeGz
        1KVhzZaqND1I+X1JS6I/V/HiTsnuVdg5aBZPYbQI0QLSgB+0SOjmTlWzjyJEt0PS
        kyOEs4bB9CPf3JaWgB9aORczsgn/cz8S7kEc8JlXDflePiSl4QPWYbX05wY9l2lJ
        yzuug/vKMCWUq0cU2i8WSA02N0+tEm4hCNol04KLKa3MRAa/yOSmDIJ4z+2D/BSD
        FZHaYejhPQFZzv73SxOAu2QCaXH5vIBEDx4Mb+lvc4BukgeIT2Gyi2gg
        -----END CERTIFICATE-----
        """;

    @Mock
    private ClientCertificateRepository clientCertificateRepository;

    @InjectMocks
    private ClientCertificateCrudServiceImpl clientCertificateCrudService;

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_find_by_id() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate repositoryCertificate = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(repositoryCertificate));

        ClientCertificate result = clientCertificateCrudService.findById(CERTIFICATE_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CERTIFICATE_ID);
        assertThat(result.getName()).isEqualTo("Test Certificate");
        assertThat(result.getApplicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void should_throw_not_found_when_certificate_does_not_exist() throws TechnicalException {
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientCertificateCrudService.findById(CERTIFICATE_ID))
            .isInstanceOf(ClientCertificateNotFoundException.class)
            .hasMessageContaining(CERTIFICATE_ID);
    }

    @Test
    void should_throw_technical_management_exception_when_find_by_id_fails() throws TechnicalException {
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenThrow(new TechnicalException("Database error"));

        assertThatThrownBy(() -> clientCertificateCrudService.findById(CERTIFICATE_ID))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining(CERTIFICATE_ID);
    }

    @Test
    void should_create_certificate_with_active_status_when_no_dates() throws TechnicalException {
        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(false);
        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate created = clientCertificateCrudService.create(
            APPLICATION_ID,
            ClientCertificate.builder().name("New Certificate").certificate(PEM_CERTIFICATE).build()
        );

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCrossId()).isNotNull();
        assertThat(created.getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(created.getName()).isEqualTo("New Certificate");
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ClientCertificateStatus.ACTIVE);

        ArgumentCaptor<io.gravitee.repository.management.model.ClientCertificate> captor = ArgumentCaptor.forClass(
            io.gravitee.repository.management.model.ClientCertificate.class
        );
        verify(clientCertificateRepository).create(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE);
    }

    @Test
    void should_create_certificate_with_scheduled_status_when_starts_at_in_future() throws TechnicalException {
        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(false);
        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate created = clientCertificateCrudService.create(
            APPLICATION_ID,
            ClientCertificate.builder()
                .name("Scheduled Certificate")
                .startsAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .certificate(PEM_CERTIFICATE)
                .build()
        );

        assertThat(created.getStatus()).isEqualTo(ClientCertificateStatus.SCHEDULED);
    }

    @Test
    void should_create_certificate_with_active_with_end_status_when_ends_at_in_future() throws TechnicalException {
        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(false);
        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate created = clientCertificateCrudService.create(
            APPLICATION_ID,
            ClientCertificate.builder()
                .name("Certificate with end date")
                .endsAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .certificate(PEM_CERTIFICATE)
                .build()
        );

        assertThat(created.getStatus()).isEqualTo(ClientCertificateStatus.ACTIVE_WITH_END);
    }

    @Test
    void should_create_certificate_with_revoked_status_when_ends_at_in_past() throws TechnicalException {
        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(false);
        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate created = clientCertificateCrudService.create(
            APPLICATION_ID,
            ClientCertificate.builder()
                .name("Revoked Certificate")
                .endsAt(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
                .certificate(PEM_CERTIFICATE)
                .build()
        );

        assertThat(created.getStatus()).isEqualTo(ClientCertificateStatus.REVOKED);
    }

    @Test
    void should_raise_error_when_certificate_already_exists() throws TechnicalException {
        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(true);
        ClientCertificate toCreate = ClientCertificate.builder().name("Certificate").certificate(PEM_CERTIFICATE).build();
        assertThatCode(() -> clientCertificateCrudService.create(APPLICATION_ID, toCreate)).isInstanceOf(
            ClientCertificateAlreadyUsedException.class
        );
    }

    @Test
    void should_update_certificate() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate existingCert = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));
        when(clientCertificateRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate updated = clientCertificateCrudService.update(
            CERTIFICATE_ID,
            ClientCertificate.builder().name("Updated Name").build()
        );

        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(ClientCertificateStatus.ACTIVE);
    }

    @Test
    void should_update_certificate_status_to_scheduled_when_starts_at_in_future() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate existingCert = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));
        when(clientCertificateRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate updated = clientCertificateCrudService.update(
            CERTIFICATE_ID,
            ClientCertificate.builder().name("Updated Name").startsAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS))).build()
        );

        assertThat(updated.getStatus()).isEqualTo(ClientCertificateStatus.SCHEDULED);
    }

    @Test
    void should_update_certificate_status_to_active_with_end_when_ends_at_in_future() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate existingCert = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));
        when(clientCertificateRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate updated = clientCertificateCrudService.update(
            CERTIFICATE_ID,
            ClientCertificate.builder().name("Updated Name").endsAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS))).build()
        );

        assertThat(updated.getStatus()).isEqualTo(ClientCertificateStatus.ACTIVE_WITH_END);
    }

    @Test
    void should_update_certificate_status_to_revoked_when_ends_at_in_past() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate existingCert = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));
        when(clientCertificateRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate updated = clientCertificateCrudService.update(
            CERTIFICATE_ID,
            ClientCertificate.builder().name("Updated Name").endsAt(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))).build()
        );

        assertThat(updated.getStatus()).isEqualTo(ClientCertificateStatus.REVOKED);
    }

    @Test
    void should_throw_not_found_when_updating_non_existing_certificate() throws TechnicalException {
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.empty());

        ClientCertificate udpatedName = ClientCertificate.builder().name("Udpated Name").build();
        assertThatThrownBy(() -> clientCertificateCrudService.update(CERTIFICATE_ID, udpatedName)).isInstanceOf(
            ClientCertificateNotFoundException.class
        );
    }

    @Test
    void should_delete_certificate() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate existingCert = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));

        clientCertificateCrudService.delete(CERTIFICATE_ID);

        verify(clientCertificateRepository).delete(CERTIFICATE_ID);
    }

    @Test
    void should_throw_not_found_when_deleting_non_existing_certificate() throws TechnicalException {
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientCertificateCrudService.delete(CERTIFICATE_ID)).isInstanceOf(
            ClientCertificateNotFoundException.class
        );
    }

    @Test
    void should_find_by_application_id() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate repositoryCertificate = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        Page<io.gravitee.repository.management.model.ClientCertificate> repositoryPage = new Page<>(
            List.of(repositoryCertificate),
            0,
            1,
            1
        );
        when(clientCertificateRepository.findByApplicationId(eq(APPLICATION_ID), any(Pageable.class))).thenReturn(repositoryPage);

        io.gravitee.rest.api.model.common.Pageable pageable = new io.gravitee.rest.api.model.common.Pageable() {
            @Override
            public int getPageNumber() {
                return 1;
            }

            @Override
            public int getPageSize() {
                return 10;
            }
        };

        Page<ClientCertificate> result = clientCertificateCrudService.findByApplicationId(APPLICATION_ID, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1).extracting("id").containsExactly(CERTIFICATE_ID);
    }

    @Test
    void should_find_by_application_id_and_statuses() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate repositoryCertificate = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        when(
            clientCertificateRepository.findByApplicationIdAndStatuses(
                eq(APPLICATION_ID),
                any(io.gravitee.repository.management.model.ClientCertificateStatus[].class)
            )
        ).thenReturn(Set.of(repositoryCertificate));

        Set<ClientCertificate> result = clientCertificateCrudService.findByApplicationIdAndStatuses(
            APPLICATION_ID,
            ClientCertificateStatus.ACTIVE
        );

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getId()).isEqualTo(CERTIFICATE_ID);
    }

    @Test
    void should_find_by_application_ids_and_statuses() throws TechnicalException {
        String applicationId2 = "app-id-2";
        String certificateId2 = "cert-id-2";
        io.gravitee.repository.management.model.ClientCertificate repositoryCertificate1 = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        io.gravitee.repository.management.model.ClientCertificate repositoryCertificate2 = buildRepositoryClientCertificate(
            certificateId2,
            applicationId2,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE_WITH_END
        );
        when(
            clientCertificateRepository.findByApplicationIdsAndStatuses(
                eq(List.of(APPLICATION_ID, applicationId2)),
                any(io.gravitee.repository.management.model.ClientCertificateStatus[].class)
            )
        ).thenReturn(Set.of(repositoryCertificate1, repositoryCertificate2));

        Set<ClientCertificate> result = clientCertificateCrudService.findByApplicationIdsAndStatuses(
            List.of(APPLICATION_ID, applicationId2),
            ClientCertificateStatus.ACTIVE,
            ClientCertificateStatus.ACTIVE_WITH_END
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClientCertificate::getId).containsExactlyInAnyOrder(CERTIFICATE_ID, certificateId2);
    }

    @Test
    void should_throw_technical_management_exception_when_find_by_application_ids_and_statuses_fails() throws TechnicalException {
        when(
            clientCertificateRepository.findByApplicationIdsAndStatuses(
                any(),
                any(io.gravitee.repository.management.model.ClientCertificateStatus[].class)
            )
        ).thenThrow(new TechnicalException("Database error"));

        List<String> applicationId = List.of(APPLICATION_ID);
        assertThatThrownBy(() ->
            clientCertificateCrudService.findByApplicationIdsAndStatuses(applicationId, ClientCertificateStatus.ACTIVE)
        )
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("An error occurs while trying to find client certificates by application IDs and statuses");
    }

    @Test
    void should_find_by_statuses() throws TechnicalException {
        var repositoryCertificate1 = buildRepositoryClientCertificate(
            CERTIFICATE_ID,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        var repositoryCertificate2 = buildRepositoryClientCertificate(
            "cert-id-2",
            "app-id-2",
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE
        );
        when(
            clientCertificateRepository.findByStatuses(any(io.gravitee.repository.management.model.ClientCertificateStatus[].class))
        ).thenReturn(Set.of(repositoryCertificate1, repositoryCertificate2));

        var result = clientCertificateCrudService.findByStatuses(ClientCertificateStatus.ACTIVE);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClientCertificate::getId).containsExactlyInAnyOrder(CERTIFICATE_ID, "cert-id-2");
    }

    @Test
    void should_throw_technical_management_exception_when_find_by_statuses_fails() throws TechnicalException {
        when(
            clientCertificateRepository.findByStatuses(any(io.gravitee.repository.management.model.ClientCertificateStatus[].class))
        ).thenThrow(new TechnicalException("Database error"));

        assertThatThrownBy(() -> clientCertificateCrudService.findByStatuses(ClientCertificateStatus.ACTIVE))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("An error occurs while trying to find client certificates by statuses");
    }

    @Test
    void should_throw_invalid_exception_when_certificate_is_malformed() {
        String invalidCertificate = """
            -----BEGIN CERTIFICATE-----
            This one is invalid
            -----END CERTIFICATE-----
            """;

        ClientCertificate toCreate = ClientCertificate.builder().name("Invalid Certificate").certificate(invalidCertificate).build();
        assertThatThrownBy(() -> {
            clientCertificateCrudService.create(APPLICATION_ID, toCreate);
        }).isInstanceOf(ClientCertificateInvalidException.class);
    }

    @Test
    void should_throw_empty_exception_when_no_certificate_can_be_extracted() {
        String noCertificate = """
            no certificate header, so not parsed as an exception by the library.
            """;
        ClientCertificate toCreate = ClientCertificate.builder().name("Empty Certificate").certificate(noCertificate).build();
        assertThatThrownBy(() -> clientCertificateCrudService.create(APPLICATION_ID, toCreate)).isInstanceOf(
            ClientCertificateEmptyException.class
        );
    }

    @Test
    void should_throw_authority_exception_when_certificate_is_a_ca() {
        String caCertificate = """
            -----BEGIN CERTIFICATE-----
            MIIGAzCCA+ugAwIBAgIUcso7he1LovzeKw5od1lZD3vlNOAwDQYJKoZIhvcNAQEL
            BQAwgZAxKTAnBgkqhkiG9w0BCQEWGmNvbnRhY3RAZ3Jhdml0ZWVzb3VyY2UuY29t
            MRAwDgYDVQQDDAdBUElNX0NOMQ0wCwYDVQQLDARBUElNMRQwEgYDVQQKDAtBUElN
            X1Rlc3RlcjEOMAwGA1UEBwwFTGlsbGUxDzANBgNVBAgMBkZyYW5jZTELMAkGA1UE
            BhMCRlIwHhcNMjQwODI4MDY0NzMzWhcNMzQwODI2MDY0NzMzWjCBkDEpMCcGCSqG
            SIb3DQEJARYaY29udGFjdEBncmF2aXRlZXNvdXJjZS5jb20xEDAOBgNVBAMMB0FQ
            SU1fQ04xDTALBgNVBAsMBEFQSU0xFDASBgNVBAoMC0FQSU1fVGVzdGVyMQ4wDAYD
            VQQHDAVMaWxsZTEPMA0GA1UECAwGRnJhbmNlMQswCQYDVQQGEwJGUjCCAiIwDQYJ
            KoZIhvcNAQEBBQADggIPADCCAgoCggIBAMOEVa4niB+yfSz9+cxoydZTMoHVPUEJ
            6o4NT34pcGf4Q6+DwNmV3Lrk291rw4hhXnlzflw4AOEZEbbpVBCC304vfjSt+enE
            MP8AtuIAsAJXjKMNBO3saD+6fhLdyIdz3rjq+fMcIAcjGFGQqgQJoniLnrnDU3ee
            WX0XnRHFOB1iGfMZ2X+0PptKvKH8Pq33er6tCCCH2cA7Owc4+6herDtP4oQ+xSqY
            spEORK37iRg7Pm8NgA/GsfjBIDjyjBsYN+waNGuS02MR8znQfgk+DjZlc4+e93vK
            VJfTzgMdOG1a/imB1mdwZvO1l9nSlArJlfvItCzi+2dc6Us67Pp8XyCiHK/2I7nJ
            DBDs84o3SA4uWe6SXfOGulTma6ENPsKC5Oh8VbbZvubbgNqFRCY19yz66zhq4wH4
            7W90TGZelHez6Dk/cCnl3WRPljuEzRcqRiU8YWdMCVqAfjdgxdSiQCOWa+Ug7Hlz
            LUvRcCAS2i20oGePKJ1Zl9IJuoik8QCovzjPP4bGgySTzvlhuKB7kyxJ6EDmo1Ic
            k2HVr0VvLRV4O1gT2lGFSu2k0QquV6WeKoQni+/oZfMRv1LTc0m+r34PN+ZdL+01
            2Bkcs2lmdp5oTwbSjw8w76Yf1vtz0SSwaQDgss8t0dJZNPECnHL+wki5byyrsdBM
            bJbovk7g8HUJAgMBAAGjUzBRMB0GA1UdDgQWBBQ3IIhDN+2FihTlbDjDIPdXjIEz
            GDAfBgNVHSMEGDAWgBQ3IIhDN+2FihTlbDjDIPdXjIEzGDAPBgNVHRMBAf8EBTAD
            AQH/MA0GCSqGSIb3DQEBCwUAA4ICAQBCyUHgdsx5tG09ol5PoHULn7QpUNYqdRo2
            Go3Qy+VTl1PngnKzWcpzFgc4zf+gaQG55KelulqOSAr13GBL2Wd9u7diM5OQQ6pK
            dhxWu9i2U/7LMSASYpNgawHTVdZ6tLi5hPxL8WQxoEBtXGIynQNKI6z76AjZwRcr
            fgq+CB2Ai8jcJxWCcMfbABPAPSwK9bRAmuP95+K7CXiCOvVnHQFQT3xMw4yyZ2Qq
            HrAL42RGyiejAx8eraE8fH8Dq9iWn6q91WY60nesyOnZLkZz/c8mTvibCE97d767
            rJUJREeS4MHFOw/wHXN/JeLryedUGSR4pEllBS/QjUhiUysvM+02a1XuwP0qD/5v
            697tDuozn/i7N9O0ThbZNR9KlSSMqAJ1iWpijt7e7Rr/CqP/42HYOZSyuoYGiydA
            P5TTsFBjbDTs2XtPEjPkoZ2vzegKLcT7H/pBtNHdwNnEcLbgDLwMGwxWI6urkjx4
            uz/iY/SibzgTnuxgTjW03HFVOFq9w2Tv/4qFNJrCxt+aQwG4RjnS77zS4AFoJ6ZI
            YQvqCvXVVosYZWLZGkbQSc2iNS2Wr5dFqngl3py6kps8BcUDzF/J/9QLMLI3ZTUt
            P5EfACFOUJGjCiuDC02wG2mO44Y98bT3oIMdjH9haMd5eoEAxmFy+M4UVTa2YK6u
            Q6teMha+jg==
            -----END CERTIFICATE-----""";
        ClientCertificate createDto = ClientCertificate.builder().name("CA Certificate").certificate(caCertificate).build();

        assertThatThrownBy(() -> clientCertificateCrudService.create(APPLICATION_ID, createDto)).isInstanceOf(
            ClientCertificateAuthorityException.class
        );
    }

    @Test
    void should_create_certificate_with_scheduled_status_when_starts_at_and_ends_at_in_future() throws TechnicalException {
        ClientCertificate domainCertificate = ClientCertificate.builder()
            .name("Scheduled Certificate")
            .startsAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .endsAt(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
            .certificate(PEM_CERTIFICATE)
            .build();
        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(false);
        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate created = clientCertificateCrudService.create(APPLICATION_ID, domainCertificate);

        assertThat(created.getStatus()).isEqualTo(ClientCertificateStatus.SCHEDULED);

        ArgumentCaptor<io.gravitee.repository.management.model.ClientCertificate> captor = ArgumentCaptor.forClass(
            io.gravitee.repository.management.model.ClientCertificate.class
        );
        verify(clientCertificateRepository).create(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(io.gravitee.repository.management.model.ClientCertificateStatus.SCHEDULED);
    }

    @Test
    void should_return_most_recent_active_certificate_when_multiple_active_certificates_exist() throws TechnicalException {
        String oldCertificateId = "old-cert-id";
        String recentCertificateId = "recent-cert-id";

        io.gravitee.repository.management.model.ClientCertificate oldCertificate = buildRepositoryClientCertificateWithCreatedAt(
            oldCertificateId,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE,
            Date.from(Instant.now().minus(2, ChronoUnit.DAYS))
        );
        io.gravitee.repository.management.model.ClientCertificate recentCertificate = buildRepositoryClientCertificateWithCreatedAt(
            recentCertificateId,
            APPLICATION_ID,
            io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE_WITH_END,
            Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        );

        when(
            clientCertificateRepository.findByApplicationIdAndStatuses(
                APPLICATION_ID,
                io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE,
                io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE_WITH_END
            )
        ).thenReturn(Set.of(oldCertificate, recentCertificate));

        Optional<ClientCertificate> result = clientCertificateCrudService.findMostRecentActiveByApplicationId(APPLICATION_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(recentCertificateId);
    }

    @Test
    void should_return_empty_when_no_active_certificates_exist() throws TechnicalException {
        when(
            clientCertificateRepository.findByApplicationIdAndStatuses(
                APPLICATION_ID,
                io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE,
                io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE_WITH_END
            )
        ).thenReturn(Set.of());

        Optional<ClientCertificate> result = clientCertificateCrudService.findMostRecentActiveByApplicationId(APPLICATION_ID);

        assertThat(result).isEmpty();
    }

    public static Stream<Arguments> datesBounds() {
        return Stream.of(
            arguments(null, null, true),
            arguments(null, new Date(), true),
            arguments(new Date(), null, true),
            arguments(new Date(), Date.from(Instant.now().plus(1, ChronoUnit.MILLIS)), true),
            arguments(new Date(), Date.from(Instant.now().plus(1, ChronoUnit.DAYS)), true),
            arguments(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), new Date(), true),
            arguments(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), Date.from(Instant.now().plus(1, ChronoUnit.DAYS)), true),
            arguments(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)), new Date(), false),
            arguments(new Date(), Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), false)
        );
    }

    @MethodSource("datesBounds")
    @ParameterizedTest
    void should_validation_date_boundaries(Date startDate, Date endDate, boolean valid) throws TechnicalException {
        when(clientCertificateRepository.findById(any())).thenReturn(
            Optional.of(io.gravitee.repository.management.model.ClientCertificate.builder().build())
        );
        ClientCertificate clientCertificate = ClientCertificate.builder()
            .name("With dates")
            .startsAt(startDate)
            .endsAt(endDate)
            .certificate(PEM_CERTIFICATE)
            .build();
        if (valid) {
            assertThatCode(() -> clientCertificateCrudService.create(APPLICATION_ID, clientCertificate)).doesNotThrowAnyException();
            assertThatCode(() -> clientCertificateCrudService.update(APPLICATION_ID, clientCertificate)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> clientCertificateCrudService.create(APPLICATION_ID, clientCertificate)).isInstanceOf(
                ClientCertificateDateBoundsInvalidException.class
            );
            assertThatThrownBy(() -> clientCertificateCrudService.update(APPLICATION_ID, clientCertificate)).isInstanceOf(
                ClientCertificateDateBoundsInvalidException.class
            );
        }
    }

    private io.gravitee.repository.management.model.ClientCertificate buildRepositoryClientCertificate(
        String id,
        String applicationId,
        io.gravitee.repository.management.model.ClientCertificateStatus status
    ) {
        return buildRepositoryClientCertificateWithCreatedAt(id, applicationId, status, new Date());
    }

    private io.gravitee.repository.management.model.ClientCertificate buildRepositoryClientCertificateWithCreatedAt(
        String id,
        String applicationId,
        io.gravitee.repository.management.model.ClientCertificateStatus status,
        Date createdAt
    ) {
        return io.gravitee.repository.management.model.ClientCertificate.builder()
            .id(id)
            .crossId("cross-id")
            .applicationId(applicationId)
            .name("Test Certificate")
            .startsAt(null)
            .endsAt(null)
            .createdAt(createdAt)
            .updatedAt(new Date())
            .certificate(PEM_CERTIFICATE)
            .certificateExpiration(Date.from(Instant.now().plus(365, ChronoUnit.DAYS)))
            .subject("CN=localhost")
            .issuer("CN=localhost")
            .fingerprint("abc123")
            .environmentId(ENVIRONMENT_ID)
            .status(status)
            .build();
    }
}
