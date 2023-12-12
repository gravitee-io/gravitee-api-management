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
package io.gravitee.apim.integration.tests.tls;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.utils.TLSUtils;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import jakarta.validation.constraints.NotEmpty;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.net.ssl.SSLHandshakeException;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TestHelper {

    static final String RESPONSE_FROM_BACKEND = "response from backend";
    static final String WIREMOCK_ENDPOINT_URI = "/endpoint";
    static final String GATEWAY_HTTP_API_URI = "/test";
    static final String GATEWAY_TCP_API_URI = WIREMOCK_ENDPOINT_URI; // using backend URI as we don't have that notion gateway side
    static final String BRIGHT_SIDE_FQDN = "bright.side.force";
    static final String DARK_SIDE_FQDN = "dark.side.force";
    static final char[] PASSWORD = "secret".toCharArray();
    static final String CLIENT_CN = "client";

    static void assertApiCall(HttpClient newHttpClient, int port, String host, String uri) {
        newHttpClient
            .rxRequest(
                new RequestOptions()
                    .setMethod(HttpMethod.GET)
                    .setServer(SocketAddress.inetSocketAddress(port, "127.0.0.1")) // set the IP...
                    .setHost(host) // ...separately from the SNI host
                    .setURI(uri)
            )
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                // just asserting we get a response (hence no SSL errors)
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .flatMapCompletable(body -> {
                assertThat(body).hasToString(RESPONSE_FROM_BACKEND);
                return Completable.complete();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();
    }

    static void assertHandshakeError(Vertx vertx, String cert, String host, int port) {
        assertHandshakeError(vertx, cert, host, port, null);
    }

    static void assertHandshakeError(Vertx vertx, String cert, String host, int port, Object clientKeyStorePath) {
        var httpClient = createTrustedHttpClient(vertx, cert, clientKeyStorePath);
        httpClient
            .rxRequest(
                new RequestOptions()
                    .setMethod(HttpMethod.GET)
                    .setServer(SocketAddress.inetSocketAddress(port, "127.0.0.1")) // set the IP...
                    .setHost(host) // ...separately from the SNI host
                    .setURI("/foo") // set a 404 on purpose to really check SSLHandshake errors
            )
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(SSLHandshakeException.class);
    }

    static HttpClient createTrustedHttpClient(Vertx vertx, String cert) {
        return createTrustedHttpClient(vertx, cert, null);
    }

    static HttpClient createTrustedHttpClient(Vertx vertx, String cert, Object clientKeyStore) {
        var options = new HttpClientOptions().setSsl(true).setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(cert)));
        if (clientKeyStore instanceof Path clientKeyStorePath) {
            options.setPfxKeyCertOptions(
                new PfxOptions().setPath(clientKeyStorePath.toAbsolutePath().toString()).setPassword(new String(PASSWORD))
            );
        } else if (clientKeyStore instanceof PemGenResult.KeyPairLocation clientKeyPairLocation) {
            options.setPemKeyCertOptions(
                new PemKeyCertOptions()
                    .addCertPath(clientKeyPairLocation.certPath().toAbsolutePath().toString())
                    .addKeyPath(clientKeyPairLocation.keyPath().toAbsolutePath().toString())
            );
        }
        return vertx.createHttpClient(options);
    }

    static KeyStoreGenResult createNewPKCS12KeyStore(@Nonnull String commonName, String... commonNames) throws Exception {
        final Path path = Files.createTempFile("keystore_", ".p12");
        Map<String, TLSUtils.X509Cert> certs = new HashMap<>();
        Map<String, String> aliases = new HashMap<>();
        TLSUtils.X509Pair keyPair0 = TLSUtils.createKeyPair(commonName);
        certs.put(commonName, keyPair0.certificate());
        String alias0 = makeAlias(0);
        aliases.put(commonName, alias0);
        KeyStore keyStore = TLSUtils.createKeyStore(alias0, keyPair0, PASSWORD);

        if (commonNames != null) {
            for (int i = 0; i < commonNames.length; i++) {
                String currentCommonName = commonNames[i];
                TLSUtils.X509Pair keyPair = TLSUtils.createKeyPair(currentCommonName);
                certs.put(currentCommonName, keyPair.certificate());
                String alias = makeAlias(i + 1);
                aliases.put(currentCommonName, alias);
                TLSUtils.appendToKeyStore(keyStore, alias, keyPair, PASSWORD);
            }
        }

        try (var out = new FileOutputStream(path.toFile())) {
            keyStore.store(out, PASSWORD);
        }
        return new KeyStoreGenResult(keyStore, path, certs, aliases);
    }

    static PemGenResult createNewPEMs(@Nonnull @NotEmpty String... commonNames) throws Exception {
        Objects.requireNonNull(commonNames);
        Path keyPairsDir = Files.createTempDirectory("pems");

        Path certsDir = Files.createTempDirectory("certs");
        Map<String, PemGenResult.KeyPairLocation> locations = new HashMap<>();
        Map<String, PemGenResult.CertGen> certificates = new HashMap<>();
        for (int i = 0; i < commonNames.length; i++) {
            String commonName = commonNames[i];

            TLSUtils.X509Pair keyPair = TLSUtils.createKeyPair(commonName);

            Path certPath = keyPairsDir.resolve(certFileName(i));
            keyPair.certificate().writeToDisk(certPath);

            Path certCopyPath = certsDir.resolve(certFileName(i));
            keyPair.certificate().writeToDisk(certCopyPath);

            certificates.put(commonName, new PemGenResult.CertGen(certCopyPath, keyPair.certificate().toPem()));

            Path keyPath = keyPairsDir.resolve(keyFileName(i));
            keyPair.privateKey().writeToDisk(keyPath);
            locations.put(commonName, new PemGenResult.KeyPairLocation(certPath, keyPath));
        }

        return new PemGenResult(keyPairsDir, locations, certsDir, certificates);
    }

    public static String makeAlias(int i) {
        return "alias_%d".formatted(i);
    }

    static String certFileName(int i) {
        return "certificate_%d.pem".formatted(i);
    }

    static String keyFileName(int i) {
        return "private_key_%d.pem".formatted(i);
    }

    record KeyStoreGenResult(KeyStore keyStore, Path location, Map<String, TLSUtils.X509Cert> certs, Map<String, String> aliases) {
        Path toTrustStore() throws Exception {
            Path tempFile = Files.createTempFile("truststore", ".p12");
            KeyStore trustStore = TLSUtils.toTrustStore(keyStore, PASSWORD);
            try (var fout = new FileOutputStream(tempFile.toFile())) {
                trustStore.store(fout, PASSWORD);
            }
            return tempFile;
        }
    }

    record PemGenResult(
        Path keyPairDirectory,
        Map<String, KeyPairLocation> locations,
        Path certDirectory,
        Map<String, CertGen> certificates
    ) {
        record KeyPairLocation(Path certPath, Path keyPath) {}
        record CertGen(Path certPath, String cert) {}
    }
}
