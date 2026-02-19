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
package io.gravitee.apim.infra.domain_service.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import io.gravitee.apim.core.application.domain_service.ValidateApplicationSettingsDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.application.TlsSettings;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.impl.configuration.application.ApplicationTypeServiceImpl;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ValidateApplicationSettingsDomainServiceImplTest {

    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);

    private final ParameterService parameterService = mock(ParameterService.class);

    private final ApplicationTypeService applicationTypeService = new ApplicationTypeServiceImpl();

    private ValidateApplicationSettingsDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        reset(applicationRepository, parameterService);
        cut = new ValidateApplicationSettingsDomainServiceImpl(applicationRepository, applicationTypeService, parameterService);
    }

    @Test
    void should_replace_null_redirect_uris_with_empty_list() {
        var givenOauthSettings = OAuthClientSettings.builder()
            .applicationType("BACKEND_TO_BACKEND")
            .redirectUris(null)
            .grantTypes(List.of("client_credentials"))
            .build();

        var expectedOauthSettings = givenOauthSettings.toBuilder().redirectUris(List.of()).responseTypes(List.of()).build();

        var givenSettings = ApplicationSettings.builder().oauth(givenOauthSettings).build();

        var expectedSettings = givenSettings.toBuilder().oauth(expectedOauthSettings).build();

        var input = new ValidateApplicationSettingsDomainService.Input(
            AuditInfo.builder().organizationId("test").environmentId("test").build(),
            "app-id",
            givenSettings
        );

        var result = cut.validateAndSanitize(input);

        assertThat(result.value()).isNotEmpty().hasValue(input.sanitized(expectedSettings));
    }

    @Test
    void should_set_response_types() {
        var givenOauthSettings = OAuthClientSettings.builder()
            .applicationType("BROWSER")
            .redirectUris(List.of("https://app.example.com"))
            .grantTypes(List.of("authorization_code"))
            .build();

        var expectedOauthSettings = givenOauthSettings.toBuilder().responseTypes(List.of("code")).build();

        var givenSettings = ApplicationSettings.builder().oauth(givenOauthSettings).build();

        var expectedSettings = givenSettings.toBuilder().oauth(expectedOauthSettings).build();

        var input = new ValidateApplicationSettingsDomainService.Input(
            AuditInfo.builder().organizationId("test").environmentId("test").build(),
            "app-id",
            givenSettings
        );

        var result = cut.validateAndSanitize(input);

        assertThat(result.value()).isNotEmpty().hasValue(input.sanitized(expectedSettings));
    }

    @Nested
    class TlsValidation {

        private static final String VALID_PEM = """
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

        private static final String VALID_CA_PEM = """
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

        private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("test").environmentId("test").build();

        private ValidateApplicationSettingsDomainService.Input inputWithTls(TlsSettings tls) {
            var settings = ApplicationSettings.builder().app(new SimpleApplicationSettings()).tls(tls).build();
            return new ValidateApplicationSettingsDomainService.Input(AUDIT_INFO, "app-id", settings);
        }

        @Test
        void should_reject_when_both_single_and_list_certificates_are_set() {
            var tls = TlsSettings.builder()
                .clientCertificate(VALID_PEM)
                .clientCertificates(List.of(new CreateClientCertificate("cert1", null, null, VALID_PEM)))
                .build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isPresent();
            assertThat(result.severe().get()).anyMatch(e -> e.getMessage().contains("cannot set both"));
        }

        @Test
        void should_warn_when_deprecated_single_certificate_is_used() {
            var tls = TlsSettings.builder().clientCertificate(VALID_PEM).build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isPresent();
            assertThat(result.warning().get()).anyMatch(e -> e.getMessage().contains("deprecated"));
        }

        @Test
        void should_accept_valid_certificate_list() {
            var tls = TlsSettings.builder()
                .clientCertificates(
                    List.of(
                        new CreateClientCertificate("cert1", null, null, VALID_PEM),
                        new CreateClientCertificate("cert2", null, null, VALID_PEM)
                    )
                )
                .build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isEmpty();
        }

        @Test
        void should_reject_unparsable_certificate_in_list() {
            var tls = TlsSettings.builder()
                .clientCertificates(List.of(new CreateClientCertificate("bad", null, null, "not-a-pem")))
                .build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isPresent();
            assertThat(result.severe().get()).anyMatch(e -> e.getMessage().contains("certificate is empty"));
        }

        @Test
        void should_reject_invalid_pem_certificate_in_list() {
            var tls = TlsSettings.builder()
                .clientCertificates(
                    List.of(
                        new CreateClientCertificate(
                            "bad-pem",
                            null,
                            null,
                            "-----BEGIN CERTIFICATE-----\nYmFkLWRhdGE=\n-----END CERTIFICATE-----"
                        )
                    )
                )
                .build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isPresent();
            assertThat(result.severe().get()).anyMatch(e -> e.getMessage().contains("not a valid PEM"));
        }

        @Test
        void should_reject_ca_certificate_in_list() {
            var tls = TlsSettings.builder()
                .clientCertificates(List.of(new CreateClientCertificate("ca-cert", null, null, VALID_CA_PEM)))
                .build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isPresent();
            assertThat(result.severe().get()).anyMatch(e -> e.getMessage().contains("CA certificate"));
        }

        @Test
        void should_reject_when_starts_at_is_after_ends_at() {
            var startsAt = new Date(System.currentTimeMillis() + 86400000L); // tomorrow
            var endsAt = new Date(System.currentTimeMillis() - 86400000L); // yesterday

            var tls = TlsSettings.builder()
                .clientCertificates(List.of(new CreateClientCertificate("bad-dates", startsAt, endsAt, VALID_PEM)))
                .build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isPresent();
            assertThat(result.severe().get()).anyMatch(e -> e.getMessage().contains("startsAt must be before endsAt"));
        }

        @Test
        void should_warn_when_ends_at_is_after_certificate_expiration() {
            // The VALID_PEM cert expires at 2121-09-25, so set endsAt way past that
            var endsAt = new Date(5000000000000L); // year 2128

            var tls = TlsSettings.builder()
                .clientCertificates(List.of(new CreateClientCertificate("late-end", null, endsAt, VALID_PEM)))
                .build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isPresent();
            assertThat(result.warning().get()).anyMatch(e -> e.getMessage().contains("endsAt is after certificate expiration"));
        }

        @Test
        void should_accept_tls_settings_with_no_certificates() {
            var tls = TlsSettings.builder().build();

            var result = cut.validateAndSanitize(inputWithTls(tls));

            assertThat(result.severe()).isEmpty();
            assertThat(result.warning()).isEmpty();
        }
    }
}
