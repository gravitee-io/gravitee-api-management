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
package io.gravitee.rest.api.management.rest.resource.auth;

import java.io.File;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * FIPS images (BC-FIPS, approved only) have no PKCS12 KeyStore, so the Cloud client keystore is a BCFKS store there.
 * The non-FIPS BouncyCastle provider stands in for BC-FIPS: both register the BCFKS KeyStore type.
 */
public class CloudBcfksKeystoreAuthenticationResourceTest extends AbstractCloudAuthenticationResourceTest {

    private static final String KEYSTORE_PREFIX = "cloud.connector.ws.ssl";

    private boolean providerAdded;

    @BeforeAll
    void registerBouncyCastleProvider() {
        providerAdded = Security.addProvider(new BouncyCastleProvider()) != -1;
    }

    @AfterAll
    void unregisterBouncyCastleProvider() {
        if (providerAdded) {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        }
    }

    @Override
    protected String getPropertyPrefix() {
        return "cloud";
    }

    @Override
    protected String getKeystorePropertyPrefix() {
        return KEYSTORE_PREFIX;
    }

    @Override
    protected void configureKeystore() {
        System.setProperty(
            KEYSTORE_PREFIX + ".keystore.path",
            new File(getClass().getClassLoader().getResource("auth/keystore.bcfks").getFile()).getAbsolutePath()
        );
        System.setProperty(KEYSTORE_PREFIX + ".keystore.password", "keystore-secret");
        System.setProperty(KEYSTORE_PREFIX + ".keystore.type", "bcfks");
        System.setProperty(KEYSTORE_PREFIX + ".keystore.key.alias", "cockpit-ca");
    }
}
