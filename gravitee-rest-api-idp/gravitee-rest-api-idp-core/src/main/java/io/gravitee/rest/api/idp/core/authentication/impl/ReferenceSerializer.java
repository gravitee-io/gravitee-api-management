/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.idp.core.authentication.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.gravitee.rest.api.idp.api.identity.IdentityReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReferenceSerializer implements ApplicationContextAware {

    @Value("${user.reference.secret:s3cR3t4grAv1t33.1Ous3D4R3f3r3nc3}")
    private String secret;

    private SecretKey secretKey;

    /*
    Per JDK-8170157, the unlimited cryptographic policy is now enabled by default.

    Specific versions from the JIRA issue:
    * Java 9: Any official release!
    * Java 8u161 or later

    //this code allows to break limit if client jdk/jre has no unlimited policy files for JCE.
    //it should be run once. So this static section is always execute during the class loading process.
    //this code is useful when working with Bouncycastle library.
    static {
        try {
            Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            field.setAccessible(true);
            field.set(null, java.lang.Boolean.FALSE);
        } catch (Exception ex) {
        }
    }
    */

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        secretKey = new SecretKeySpec(secret.getBytes(), "AES");
    }

    public String serialize(IdentityReference reference) throws Exception {
        // Create HMAC signer
        JWSSigner signer = new MACSigner(secretKey.getEncoded());

        // Prepare JWT with claims set
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(reference.getReference())
                .issuer(reference.getSource())
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        // Apply the HMAC protection
        signedJWT.sign(signer);

        // Create JWE object with signed JWT as payload
        JWEObject jweObject = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                        .contentType("JWT") // required to signal nested JWT
                        .build(),
                new Payload(signedJWT));

        // Perform encryption
        jweObject.encrypt(new DirectEncrypter(secretKey.getEncoded()));

        // Serialize to compact form
        return new String(Base64.getEncoder().encode(jweObject.serialize().getBytes()));
    }

    public IdentityReference deserialize(String token) throws Exception {
        String sToken = new String(Base64.getDecoder().decode(token));

        // Parse the JWE string
        JWEObject jweObject = JWEObject.parse(sToken);

        // Decrypt with shared key
        jweObject.decrypt(new DirectDecrypter(secretKey.getEncoded()));

        // Extract payload
        SignedJWT signedJWT = jweObject.getPayload().toSignedJWT();

        // Check the HMAC
        signedJWT.verify(new MACVerifier(secretKey.getEncoded()));

        // Retrieve the JWT claims
        return new IdentityReference(signedJWT.getJWTClaimsSet().getIssuer(), signedJWT.getJWTClaimsSet().getSubject());
    }
}