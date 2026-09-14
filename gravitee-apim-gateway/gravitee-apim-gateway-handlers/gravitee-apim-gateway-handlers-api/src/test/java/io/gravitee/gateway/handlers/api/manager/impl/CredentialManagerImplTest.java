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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.handlers.api.manager.DeployedCredential;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialManagerImplTest {

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private License license;

    private CredentialManagerImpl manager;

    @BeforeEach
    void setUp() {
        lenient().when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
        lenient().when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(true);
        manager = new CredentialManagerImpl(licenseManager);
    }

    @Nested
    class LicenseTest {

        @Test
        void should_not_deploy_when_license_does_not_include_the_feature() {
            when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(false);

            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));

            assertThat(manager.get("env-1", "credential-1")).isEmpty();
        }

        @Test
        void should_not_deploy_when_there_is_no_license() {
            when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(null);

            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));

            assertThat(manager.get("env-1", "credential-1")).isEmpty();
        }

        @Test
        void should_check_the_license_of_the_credential_organization() {
            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));

            verify(licenseManager).getOrganizationLicenseOrPlatform("org-1");
        }
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_a_credential() {
            DeployedCredential credential = credential("credential-1", "env-1", "secret-v1", 1L);

            manager.deploy(credential);

            assertThat(manager.get("env-1", "credential-1")).contains(credential);
        }

        @Test
        void should_replace_a_credential_with_a_newer_copy() {
            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));

            manager.deploy(credential("credential-1", "env-1", "secret-v2", 2L));

            assertThat(manager.get("env-1", "credential-1")).map(DeployedCredential::encryptedSecret).contains("secret-v2");
        }

        @Test
        void should_keep_the_newer_copy_when_an_older_one_arrives_late() {
            manager.deploy(credential("credential-1", "env-1", "secret-v2", 2L));

            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));

            assertThat(manager.get("env-1", "credential-1")).map(DeployedCredential::encryptedSecret).contains("secret-v2");
        }
    }

    @Nested
    class GetTest {

        @Test
        void should_not_find_a_credential_from_another_environment() {
            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));

            assertThat(manager.get("env-2", "credential-1")).isEmpty();
        }

        @Test
        void should_return_empty_for_an_unknown_credential() {
            assertThat(manager.get("env-1", "unknown")).isEmpty();
        }

        @Test
        void should_return_empty_when_environment_or_id_is_missing() {
            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));

            assertThat(manager.get(null, "credential-1")).isEmpty();
            assertThat(manager.get("env-1", null)).isEmpty();
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_a_credential() {
            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));

            manager.undeploy("env-1", "credential-1");

            assertThat(manager.get("env-1", "credential-1")).isEmpty();
        }

        @Test
        void should_only_undeploy_from_the_given_environment() {
            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));
            manager.deploy(credential("credential-1", "env-2", "secret-v1", 1L));

            manager.undeploy("env-1", "credential-1");

            assertThat(manager.get("env-1", "credential-1")).isEmpty();
            assertThat(manager.get("env-2", "credential-1")).isPresent();
        }

        @Test
        void should_undeploy_from_every_environment_when_the_environment_is_unknown() {
            manager.deploy(credential("credential-1", "env-1", "secret-v1", 1L));
            manager.deploy(credential("credential-1", "env-2", "secret-v1", 1L));

            manager.undeploy(null, "credential-1");

            assertThat(manager.get("env-1", "credential-1")).isEmpty();
            assertThat(manager.get("env-2", "credential-1")).isEmpty();
        }

        @Test
        void should_ignore_undeploying_an_unknown_credential() {
            manager.undeploy("env-1", "unknown");

            assertThat(manager.get("env-1", "unknown")).isEmpty();
        }
    }

    @Nested
    class ConcurrencyTest {

        @Test
        void should_not_lose_a_deployed_credential_while_another_one_in_the_same_environment_is_undeployed() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                for (int round = 0; round < 50_000; round++) {
                    String undeployed = "undeployed-" + round;
                    String deployed = "deployed-" + round;
                    manager.deploy(credential(undeployed, "env-1", "secret", 1L));
                    CountDownLatch start = new CountDownLatch(1);

                    Future<?> undeploy = executor.submit(() -> {
                        start.await();
                        manager.undeploy("env-1", undeployed);
                        return null;
                    });
                    Future<?> deploy = executor.submit(() -> {
                        start.await();
                        manager.deploy(credential(deployed, "env-1", "secret", 1L));
                        return null;
                    });
                    start.countDown();
                    undeploy.get();
                    deploy.get();

                    assertThat(manager.get("env-1", deployed)).as("round %d", round).isPresent();
                    manager.undeploy("env-1", deployed);
                }
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void should_keep_the_encrypted_secret_out_of_toString() {
        assertThat(credential("credential-1", "env-1", "secret-v1", 1L).toString()).contains("credential-1").doesNotContain("secret-v1");
    }

    private static DeployedCredential credential(String id, String environmentId, String encryptedSecret, long updatedAt) {
        return new DeployedCredential(id, environmentId, "org-1", encryptedSecret, updatedAt);
    }
}
