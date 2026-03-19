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
package io.gravitee.apim.core.application_certificate.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.rest.api.service.exceptions.ClientCertificateAuthorityException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateEmptyException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCertificateValidationDomainServiceTest {

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
        HrAL42RGyejAx8eraE8fH8Dq9iWn6q91WY60nesyOnZLkZz/c8mTvibCE97d767
        rJUJREeS4MHFOw/wHXN/JeLryedUGSR4pEllBS/QjUhiUysvM+02a1XuwP0qD/5v
        697tDuozn/i7N9O0ThbZNR9KlSSMqAJ1iWpijt7e7Rr/CqP/42HYOZSyuoYGiydA
        P5TTsFBjbDTs2XtPEjPkoZ2vzegKLcT7H/pBtNHdwNnEcLbgDLwMGwxWI6urkjx4
        uz/iY/SibzgTnuxgTjW03HFVOFq9w2Tv/4qFNJrCxt+aQwG4RjnS77zS4AFoJ6ZI
        YQvqCvXVVosYZWLZGkbQSc2iNS2Wr5dFqngl3py6kps8BcUDzF/J/9QLMLI3ZTUt
        P5EfACFOUJGjCiuDC02wG2mO44Y98bT3oIMdjH9haMd5eoEAxmFy+M4UVTa2YK6u
        Q6teMha+jg==
        -----END CERTIFICATE-----""";

    private final ClientCertificateValidationDomainService service = new ClientCertificateValidationDomainService();

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
        assertThatThrownBy(() -> service.validate("not-a-valid-pem")).isInstanceOf(ClientCertificateInvalidException.class);
    }

    @Test
    void should_throw_empty_exception_for_empty_pem() {
        String emptyPem = """
            -----BEGIN CERTIFICATE-----
            -----END CERTIFICATE-----
            """;

        assertThatThrownBy(() -> service.validate(emptyPem)).isInstanceOf(ClientCertificateEmptyException.class);
    }

    @Test
    void should_throw_authority_exception_for_ca_certificate() {
        assertThatThrownBy(() -> service.validate(CA_CERTIFICATE)).isInstanceOf(ClientCertificateAuthorityException.class);
    }
}
