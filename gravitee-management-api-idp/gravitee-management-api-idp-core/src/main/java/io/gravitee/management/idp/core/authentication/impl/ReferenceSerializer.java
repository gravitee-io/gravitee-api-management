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
package io.gravitee.management.idp.core.authentication.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.gravitee.management.idp.api.identity.IdentityReference;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReferenceSerializer {

    private static SecretKey secretKey;

    static {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            secretKey = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

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
