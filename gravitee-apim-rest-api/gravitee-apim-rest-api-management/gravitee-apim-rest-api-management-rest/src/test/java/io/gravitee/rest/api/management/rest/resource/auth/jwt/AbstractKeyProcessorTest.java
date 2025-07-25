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
package io.gravitee.rest.api.management.rest.resource.auth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import java.io.StringWriter;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class AbstractKeyProcessorTest {

    protected static String RSA_KID = "rsa";
    protected static String EC_KID = "ec";

    protected final DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier = new DefaultJWTClaimsVerifier<>(
        new JWTClaimsSet.Builder().issuer("iss").build(),
        new HashSet<>(Arrays.asList("sub", "iss", "iat"))
    );

    protected static KeyPair getRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    protected static KeyPair getCEKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyPairGenerator.initialize(256, random);
        return keyPairGenerator.generateKeyPair();
    }

    protected String generateRSAToken(KeyPair keyPair) throws JOSEException {
        JWSHeader header = new JWSHeader.Builder(Algorithm.RS256.getAlgorithm()).type(JOSEObjectType.JWT).keyID(RSA_KID).build();

        SignedJWT signedJWT = new SignedJWT(header, getJwtClaimsSet());
        signedJWT.sign(new RSASSASigner(keyPair.getPrivate()));
        return signedJWT.serialize();
    }

    protected String generateECToken(KeyPair keyPair) throws JOSEException {
        JWSHeader header = new JWSHeader.Builder(Algorithm.ES256.getAlgorithm()).type(JOSEObjectType.JWT).keyID(EC_KID).build();

        SignedJWT signedJWT = new SignedJWT(header, getJwtClaimsSet());
        signedJWT.sign(new ECDSASigner((ECPrivateKey) keyPair.getPrivate()));
        return signedJWT.serialize();
    }

    protected static JWTClaimsSet getJwtClaimsSet() {
        return new JWTClaimsSet.Builder()
            .issuer("iss")
            .subject("sub")
            .claim("token", "test-token")
            .issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
            .build();
    }

    protected String formatPem(PublicKey publicKey) {
        try {
            StringWriter writer = new StringWriter();
            PemWriter pemWriter = new PemWriter(writer);
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
            pemWriter.flush();
            pemWriter.close();

            return writer.toString();
        } catch (Exception e) {
            log.error("Unable to format public key", e);
            return "";
        }
    }
}
