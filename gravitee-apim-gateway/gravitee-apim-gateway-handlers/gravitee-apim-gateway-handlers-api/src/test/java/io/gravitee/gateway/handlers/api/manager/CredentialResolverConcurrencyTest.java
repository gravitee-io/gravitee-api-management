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
package io.gravitee.gateway.handlers.api.manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.secrets.api.el.FieldKind;
import io.gravitee.secrets.api.el.SecretFieldAccessControl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * One resolver is shared by every request the gateway serves, and it decrypts on each resolution, so the
 * decrypt path runs concurrently under load. {@code DataEncryptor} builds a {@link javax.crypto.Cipher} per
 * call instead of caching one, which is what makes that safe. This pins it: a shared cipher would not fail
 * outright, it would hand one request another request's plaintext, so the credentials carry distinct secrets
 * and every resolution is checked against its own.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialResolverConcurrencyTest {

    private static final SecretFieldAccessControl SECRET_FIELD = new SecretFieldAccessControl(true, FieldKind.PASSWORD, "clientSecret");
    private static final int THREADS = 8;
    private static final int RESOLUTIONS_PER_THREAD = 250;

    private final DataEncryptor dataEncryptor = new DataEncryptor(
        new MockEnvironment(),
        "api.properties.encryption.secret",
        "vvLJ4Q8Khvv9tm2tIPdkGEdmgKUruAL6"
    );

    @Test
    void every_resolution_returns_its_own_credential_under_concurrent_load() throws Exception {
        Map<String, String> secretsById = Map.of(
            "credential-1",
            "s3cr3t-one",
            "credential-2",
            "s3cr3t-two",
            "credential-3",
            "s3cr3t-three",
            "credential-4",
            "s3cr3t-four"
        );
        List<String> ids = List.copyOf(secretsById.keySet());
        var resolver = new CredentialResolver(new FixedCredentialManager(deploy(secretsById)), dataEncryptor, new ObjectMapper());

        var barrier = new CyclicBarrier(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Callable<List<String>>> tasks = IntStream.range(0, THREADS)
                .mapToObj(thread ->
                    (Callable<List<String>>) () -> {
                        List<String> mismatches = new ArrayList<>();
                        barrier.await();
                        for (int i = 0; i < RESOLUTIONS_PER_THREAD; i++) {
                            String id = ids.get((thread + i) % ids.size());
                            String value = resolver.resolve("env-1", "api-1", id, "clientSecret", SECRET_FIELD);
                            if (!secretsById.get(id).equals(value)) {
                                mismatches.add(id + " resolved to " + value);
                            }
                        }
                        return mismatches;
                    }
                )
                .toList();

            List<String> mismatches = new ArrayList<>();
            for (Future<List<String>> result : pool.invokeAll(tasks)) {
                mismatches.addAll(result.get());
            }

            assertThat(mismatches).as("every resolution returns the secret of the credential it asked for").isEmpty();
        } finally {
            pool.shutdownNow();
        }
    }

    private Map<String, DeployedCredential> deploy(Map<String, String> secretsById) throws Exception {
        Map<String, DeployedCredential> deployed = new HashMap<>();
        for (Map.Entry<String, String> entry : secretsById.entrySet()) {
            String encrypted = dataEncryptor.encrypt("{\"clientSecret\": \"" + entry.getValue() + "\"}");
            deployed.put(entry.getKey(), new DeployedCredential(entry.getKey(), "env-1", "org-1", Set.of("api-1"), encrypted, 1L));
        }
        return Map.copyOf(deployed);
    }

    /** Not a mock: driving Mockito's bookkeeping from every thread would be its own source of contention. */
    private record FixedCredentialManager(Map<String, DeployedCredential> byId) implements CredentialManager {
        @Override
        public void deploy(DeployedCredential credential) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void undeploy(String environmentId, String credentialId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<DeployedCredential> get(String environmentId, String credentialId) {
            return "env-1".equals(environmentId) ? Optional.ofNullable(byId.get(credentialId)) : Optional.empty();
        }
    }
}
