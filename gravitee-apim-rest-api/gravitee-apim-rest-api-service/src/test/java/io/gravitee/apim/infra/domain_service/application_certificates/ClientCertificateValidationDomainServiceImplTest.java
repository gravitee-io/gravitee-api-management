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
package io.gravitee.apim.infra.domain_service.application_certificates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ClientCertificateCrudServiceInMemory;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAlreadyUsedException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAuthorityException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateDateBoundsInvalidException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateEmptyException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCertificateValidationDomainServiceImplTest {

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

    private static final String CA_CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIIC8DCCAdigAwIBAgIJAIDWbQ+HkjdNMA0GCSqGSIb3DQEBCwUAMC0xDzANBgNV
        BAMMBlRlc3RDQTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCRlIwHhcNMjYwMzIz
        MjEzMDAwWhcNMjcwMzIzMjEzMDAwWjAtMQ8wDQYDVQQDDAZUZXN0Q0ExDTALBgNV
        BAoMBFRlc3QxCzAJBgNVBAYTAkZSMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB
        CgKCAQEA49xfDHXiTyXz6LpiFbNWbwqjqv5u5l4p3DiTXiUbDOBKIn5Ufm00KcEb
        peIoLi2I7fucoTki6gIQsQE+CF968uj1KmB8G5sp/P0M4y+8BN3tKddn5FL/uG0Z
        uYMD0WP7qid5Ul1+xF3ZYsUjZtgONTGLlgq9YcJdMlfp1GRbZiEY1diiXNGisw2F
        S9Q3rbzKlp2lfgxKxj/AsNgvodymLh9HTK0E7NxrKB4ZGClOpvODVyptEqilAtmj
        4jrSi30Qr440COJ/jSzFarqfzo79dLGWloFsUVWgBCFnwMJGMBlR2IK7B93ZH6E0
        cy06tZ6Bvdr5+xrulCtjHWLYr4/QJwIDAQABoxMwETAPBgNVHRMBAf8EBTADAQH/
        MA0GCSqGSIb3DQEBCwUAA4IBAQCaTWEd1RFtYYd/aNfOVUJaUWl3UAjLlN/YjiFy
        Nrc++By1F7DvHPjXRud2vX5tXrUagXO0jRaTMUiSb8IqWAfWdTsSd9kcjLoc72Pz
        Vhn+clJ6gFcNmanlXjjbjdYbwVZZEYiuW1MaysUfIT7UM2qcvNjmc3bT/4V2XziT
        wNUyKVRzMAsbaJvWeRa/NwDLS4vNddqTDbqCvTlTGvXdb7clANxg8GtdNefD0cFP
        ghAA8wBjbsZGrpMCNrX8wx++sqkYbBfkr1suOavMW5gj9GbGzeXBKfpM0m8OfDAJ
        joaLG86qfsvjR/MjEwNCKxzRiAPbV8ehxUaUqAYPLlhwge8t
        -----END CERTIFICATE-----
        """;

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
    private final ClientCertificateValidationDomainServiceImpl service = new ClientCertificateValidationDomainServiceImpl(
        clientCertificateCrudService
    );

    @BeforeEach
    void setUp() {
        clientCertificateCrudService.reset();
    }

    @Nested
    class Validate {

        @Test
        void should_return_certificate_info_for_valid_pem() {
            var result = service.validate(PEM_CERTIFICATE);

            assertThat(result).isNotNull();
            assertThat(result.certificateExpiration()).isNotNull();
            assertThat(result.subject()).contains("unit-tests");
            assertThat(result.issuer()).contains("unit-tests");
            assertThat(result.fingerprint()).isNotBlank();
        }

        @Test
        void should_throw_invalid_exception_for_malformed_pem() {
            String malformedPem = """
                -----BEGIN CERTIFICATE-----
                This one is invalid
                -----END CERTIFICATE-----
                """;
            assertThatThrownBy(() -> service.validate(malformedPem)).isInstanceOf(ClientCertificateInvalidException.class);
        }

        @Test
        void should_throw_empty_exception_when_no_certificate_can_be_extracted() {
            assertThatThrownBy(() -> service.validate("no certificate header, so not parsed as an exception by the library.")).isInstanceOf(
                ClientCertificateEmptyException.class
            );
        }

        @Test
        void should_throw_authority_exception_for_ca_certificate() {
            assertThatThrownBy(() -> service.validate(CA_CERTIFICATE)).isInstanceOf(ClientCertificateAuthorityException.class);
        }
    }

    @Nested
    class ValidateForCreation {

        @Test
        void should_validate_and_return_certificate_info() {
            var cert = new ClientCertificate("Test", PEM_CERTIFICATE, null, null);

            var result = service.validateForCreation(cert, "env-1");

            assertThat(result).isNotNull();
            assertThat(result.fingerprint()).isNotBlank();
            assertThat(result.subject()).contains("unit-tests");
            assertThat(result.issuer()).contains("unit-tests");
            assertThat(result.certificateExpiration()).isNotNull();
        }

        @Test
        void should_throw_when_fingerprint_already_exists() {
            var info = service.validate(PEM_CERTIFICATE);
            clientCertificateCrudService.initWith(
                List.of(
                    new ClientCertificate(
                        "existing-id",
                        "cross-id",
                        "app-1",
                        "Existing",
                        null,
                        null,
                        new Date(),
                        new Date(),
                        PEM_CERTIFICATE,
                        null,
                        null,
                        null,
                        info.fingerprint(),
                        "env-1",
                        ClientCertificateStatus.ACTIVE
                    )
                )
            );

            var cert = new ClientCertificate("New Cert", PEM_CERTIFICATE, null, null);

            assertThatThrownBy(() -> service.validateForCreation(cert, "env-1")).isInstanceOf(ClientCertificateAlreadyUsedException.class);
        }

        @Test
        void should_throw_when_starts_at_is_after_ends_at() {
            var startsAt = Date.from(Instant.now().plus(2, ChronoUnit.DAYS));
            var endsAt = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
            var cert = new ClientCertificate("Bad Dates", PEM_CERTIFICATE, startsAt, endsAt);

            assertThatThrownBy(() -> service.validateForCreation(cert, "env-1")).isInstanceOf(
                ClientCertificateDateBoundsInvalidException.class
            );
        }

        @Test
        void should_accept_null_date_bounds() {
            var cert = new ClientCertificate("No Dates", PEM_CERTIFICATE, null, null);

            var result = service.validateForCreation(cert, "env-1");

            assertThat(result).isNotNull();
        }
    }
}
