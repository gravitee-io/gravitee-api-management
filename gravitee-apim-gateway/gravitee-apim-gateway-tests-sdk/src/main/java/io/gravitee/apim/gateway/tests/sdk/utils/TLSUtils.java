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
package io.gravitee.apim.gateway.tests.sdk.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@SuppressWarnings("java:S112") // only used in tests
public class TLSUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Represent TLS data (cert or private key) with handy methods to export pem
     * @param <D> the type of TLS Data
     */
    interface TlsData<D> {
        void writeToDisk(Path path) throws IOException;

        String toPem() throws IOException;

        default String toPem(D object) throws IOException {
            try (StringWriter out = new StringWriter(); JcaPEMWriter writer = new JcaPEMWriter(out)) {
                writer.writeObject(object);
                writer.flush();
                return out.toString();
            }
        }

        D data();
    }

    /**
     * {@link X509Certificate} record wrapper can be used directly or as pem
     * @param data a X509Certificate object
     */
    public record X509Cert(X509Certificate data) implements TlsData<X509Certificate> {
        public void writeToDisk(Path path) throws IOException {
            Files.writeString(path, toPem());
        }

        @Override
        public String toPem() throws IOException {
            return toPem(data);
        }
    }

    /**
     * {@link PrivateKey} record wrapper can be used directly or as pem
     * @param data
     */
    public record X509Key(PrivateKey data) implements TlsData<PrivateKey> {
        @Override
        public void writeToDisk(Path path) throws IOException {
            Files.writeString(path, toPem());
        }

        @Override
        public String toPem() throws IOException {
            return toPem(data);
        }
    }

    /**
     * A match pair of X509 cert and private key
     * @param certificate the cert
     * @param privateKey the key
     */
    public record X509Pair(X509Cert certificate, X509Key privateKey) {}

    /**
     * Generate a self-signed certificate with an RSA private key signed with SHA256WithRSAEncryption
     * @param commonName the CN to add to the subject and issuer (C=FR, O=Gravitee, OU=IntegrationTests, CN=&lt;commonName&gt;)
     * @return a pair
     * @throws Exception when something wrong happened when generating the certificate
     */
    public static X509Pair createKeyPair(String commonName) throws Exception {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
        keyPairGenerator.initialize(2048);
        final KeyPair keyPair = keyPairGenerator.genKeyPair();

        final X509Certificate certificate = generateCert(keyPair, commonName);

        return new X509Pair(new X509Cert(certificate), new X509Key(keyPair.getPrivate()));
    }

    private static X509Certificate generateCert(final KeyPair keyPair, String commonName) throws Exception {
        String issuer = "C=FR, O=Gravitee, OU=IntegrationTests, CN=%s".formatted(commonName);
        final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
            new X500Name(issuer),
            BigInteger.ONE,
            Date.from(Instant.now().minus(30, ChronoUnit.DAYS)),
            Date.from(Instant.now().plus(30, ChronoUnit.DAYS)),
            new X500Name(issuer),
            SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSAEncryption");
        final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        final ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(
            PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded())
        );
        final X509CertificateHolder x509CertificateHolder = certificateBuilder.build(signer);

        final X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(x509CertificateHolder);
        certificate.checkValidity(new Date());
        certificate.verify(keyPair.getPublic());

        return certificate;
    }

    /**
     * Create a PKCS12 keystore or truststore depending on the type of <code>data</code>
     * @param alias the alias used to add  <code>data</code> to the keystore
     * @param data a {@link X509Pair} or {@link X509Cert} instance
     * @param password used to secure the keystore
     * @return a keystore with 1 entry
     * @throws Exception when something wrong happened or  <code>data</code>  is not a supported type
     */
    public static KeyStore createKeyStore(String alias, Object data, char[] password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, password);
        addEntry(ks, alias, data, password);
        return ks;
    }

    /**
     * Happen data to an existing keystore.
     * @param keystore the keystore to happen
     * @param alias the alias used to add <code>data</code> to the keystore
     * @param data a {@link X509Pair} or {@link X509Cert} instance
     * @param password used to secure the keystore
     * @throws Exception when something wrong happened or  <code>data</code>  is not a supported type
     */
    public static void appendToKeyStore(KeyStore keystore, String alias, Object data, char[] password) throws Exception {
        addEntry(keystore, alias, data, password);
    }

    /**
     * Extract all certificates from the KeyStore and add them to a new PKCS12 KeyStore secured with the same password
     * @param keyStore the keystore to read.
     * @param password password of the keystore to read and future truststore
     * @return a truststore containing only certificate entries
     * @throws Exception when something wrong happened while read/write keystores
     */
    public static KeyStore toTrustStore(KeyStore keyStore, char[] password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, password);
        for (String alias : Collections.list(keyStore.aliases())) {
            Certificate certificate = keyStore.getCertificate(alias);
            trustStore.setEntry(alias, new KeyStore.TrustedCertificateEntry(certificate), null);
        }
        return trustStore;
    }

    private static void addEntry(KeyStore ks, String alias, Object data, char[] password) throws KeyStoreException {
        if (ks.containsAlias(alias)) {
            throw new IllegalArgumentException("alias %s already exists".formatted(alias));
        }
        if (data instanceof X509Pair x509Pair) {
            ks.setEntry(
                alias,
                new KeyStore.PrivateKeyEntry(x509Pair.privateKey().data(), new X509Certificate[] { x509Pair.certificate().data() }),
                new KeyStore.PasswordProtection(password)
            );
        } else if (data instanceof X509Cert x509Cert) {
            ks.setEntry(alias, new KeyStore.TrustedCertificateEntry(x509Cert.data()), null);
        } else {
            throw new IllegalArgumentException("%s cannot be added to a key store".formatted(data));
        }
    }
}
