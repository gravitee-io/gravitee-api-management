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
package io.gravitee.apim.integration.tests.secrets.conf;

import static io.gravitee.apim.integration.tests.secrets.conf.SecuredVaultContainer.CERT_PEMFILE;
import static io.gravitee.apim.integration.tests.secrets.conf.SecuredVaultContainer.PASSWORD;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

/**
 * Static utility methods for generating client-side SSL certs and keys, for tests that use Vault's
 * TLS Certificate auth backend.  Right now, all such code is isolated to `AuthBackendCertTests`.
 */
public class SSLUtils {

    record SSLPairs(String cert, String privateKey, KeyStore keyStore, KeyStore clientStore) {}

    private SSLUtils() {}

    public static SSLPairs createPairs() throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        final X509CertificateHolder certificateHolder = getX509CertificateHolder();
        final X509Certificate vaultCertificate = getCertificate(certificateHolder);

        KeyStore clientTrustStore = getClientTrustStore(vaultCertificate);

        // Store the Vault's server certificate as a trusted cert in the truststore

        // Generate a client certificate, and store it in a Java keystore
        final KeyPair keyPair = generateKeyPair();
        final X509Certificate clientCertificate = generateCert(keyPair);
        if (clientCertificate == null) {
            throw new IOException("Failed to generate certificate");
        }
        final KeyStore clientKeystore = getClientKeystore(keyPair, clientCertificate);

        // Also write the client certificate to a PEM file, so it can be registered with Vault
        String certToPem = certToPem(clientCertificate);
        String privateKeyToPem = privateKeyToPem(keyPair.getPrivate());
        return new SSLPairs(certToPem, privateKeyToPem, clientKeystore, clientTrustStore);
    }

    private static KeyStore getClientTrustStore(X509Certificate vaultCertificate) throws IOException {
        final KeyStore trustStore = emptyStore();
        try {
            trustStore.setCertificateEntry("cert", vaultCertificate);
        } catch (KeyStoreException e) {
            throw new IOException("Cannot create trust keystore.", e);
        }
        return trustStore;
    }

    private static KeyStore getClientKeystore(KeyPair keyPair, X509Certificate clientCertificate) {
        try {
            final KeyStore keyStore = emptyStore();
            keyStore.setKeyEntry("privatekey", keyPair.getPrivate(), PASSWORD.toCharArray(), new Certificate[] { clientCertificate });
            keyStore.setCertificateEntry("cert", clientCertificate);
            return keyStore;
        } catch (KeyStoreException | IOException e) {
            return null;
        }
    }

    private static X509CertificateHolder getX509CertificateHolder() {
        final PEMParser pemParser;
        try (FileReader fileReader = new FileReader(CERT_PEMFILE)) {
            pemParser = new PEMParser(fileReader);
            return (X509CertificateHolder) pemParser.readObject();
        } catch (IOException e) {
            return null;
        }
    }

    private static X509Certificate getCertificate(X509CertificateHolder certificateHolder) {
        try {
            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certificateHolder);
        } catch (CertificateException e) {
            return null;
        }
    }

    private static KeyPair generateKeyPair() throws IOException {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
            keyPairGenerator.initialize(4096);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            if (keyPair == null) {
                throw new IOException("Failed to generate keypair");
            }
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to generate keypair", e);
        }
    }

    private static X509Certificate generateCert(final KeyPair keyPair) {
        String issuer = "C=AU, O=The Legion of the Bouncy Castle, OU=Client Certificate, CN=localhost";
        final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
            new X500Name(issuer),
            BigInteger.ONE,
            new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30),
            new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
            new X500Name(issuer),
            SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        final GeneralNames subjectAltNames = new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
        try {
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        } catch (CertIOException e) {
            e.printStackTrace();
            return null;
        }

        final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WithRSAEncryption");
        final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        final BcContentSignerBuilder signerBuilder = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
        final X509CertificateHolder x509CertificateHolder;
        try {
            final AsymmetricKeyParameter keyp = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
            final ContentSigner signer = signerBuilder.build(keyp);
            x509CertificateHolder = certificateBuilder.build(signer);
        } catch (IOException | OperatorCreationException e) {
            e.printStackTrace();
            return null;
        }

        final X509Certificate certificate;
        try {
            certificate = new JcaX509CertificateConverter().getCertificate(x509CertificateHolder);
            certificate.checkValidity(new Date());
            certificate.verify(keyPair.getPublic());
        } catch (CertificateException | SignatureException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        }

        return certificate;
    }

    private static String certToPem(final X509Certificate certificate) throws IOException {
        final Base64.Encoder encoder = Base64.getMimeEncoder();

        final String certHeader = "-----BEGIN CERTIFICATE-----\n";
        final String certFooter = "\n-----END CERTIFICATE-----";
        final byte[] certBytes;
        try {
            certBytes = certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new IOException("Failed to encode certificate", e);
        }
        final String certContents = new String(encoder.encode(certBytes));
        return certHeader + certContents + certFooter;
    }

    private static String privateKeyToPem(final PrivateKey key) {
        final Base64.Encoder encoder = Base64.getMimeEncoder();

        final String keyHeader = "-----BEGIN PRIVATE KEY-----\n";
        final String keyFooter = "\n-----END PRIVATE KEY-----";
        final byte[] keyBytes = key.getEncoded();
        final String keyContents = new String(encoder.encode(keyBytes));
        return keyHeader + keyContents + keyFooter;
    }

    private static KeyStore emptyStore() throws IOException {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");

            // Loading creates the store, can't do anything with it until it's loaded
            ks.load(null, PASSWORD.toCharArray());
            return ks;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new IOException("Cannot create empty keystore.", e);
        }
    }
}
