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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesHelper {

    public static final DockerImageName K3S_SERVER_DOCKER_IMAGE = DockerImageName.parse("rancher/k3s:v1.28.1-k3s1");

    static K3sContainer getK3sServer() {
        K3sContainer k3sContainer = new K3sContainer(K3S_SERVER_DOCKER_IMAGE);
        String k3sHost = k3sContainer.getHost();

        return k3sContainer
            .withCommand("server", "--disable", " traefik", "--tls-san=" + k3sHost)
            .withStartupAttempts(2)
            .waitingFor(Wait.forLogMessage(".*Node controller sync successful.*", 1).withStartupTimeout(Duration.ofMinutes(2)));
    }

    static void createNamespace(K3sContainer k3sContainer, String name) throws IOException, InterruptedException {
        Container.ExecResult execResult = k3sContainer.execInContainer("kubectl", "create", "namespace", name);
        assertThat(execResult.getExitCode()).isZero();
        assertThat(execResult.getStdout()).contains("namespace/%s created".formatted(name));
    }

    static void createSecret(K3sContainer k3sContainer, String namespace, String name, Map<String, String> data)
        throws IOException, InterruptedException {
        createSecret(k3sContainer, namespace, name, data, false);
    }

    static void createSecret(K3sContainer k3sContainer, String namespace, String name, Map<String, String> data, boolean isTLS)
        throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("kubectl", "create", "secret", isTLS ? "tls" : "generic", name, "-n", namespace));
        if (isTLS) {
            String certPath = "/tmp/%s-tls.crt".formatted(UUID.randomUUID());
            String keyPath = "/tmp/%s-tls.key".formatted(UUID.randomUUID());
            k3sContainer.copyFileToContainer(Transferable.of(Objects.requireNonNull(data.get("tls.crt"))), certPath);
            k3sContainer.copyFileToContainer(Transferable.of(Objects.requireNonNull(data.get("tls.key"))), keyPath);
            args.add("--cert=%s".formatted(certPath));
            args.add("--key=%s".formatted(keyPath));
        } else {
            data
                .entrySet()
                .stream()
                .map(entry -> "--from-literal=%s=%s".formatted(entry.getKey(), entry.getValue()))
                .forEach(args::add);
        }
        Container.ExecResult execResult = k3sContainer.execInContainer(args.toArray(new String[0]));
        assertThat(execResult.getStderr()).isEmpty();
        assertThat(execResult.getStdout()).contains("secret/%s created".formatted(name));
        assertThat(execResult.getExitCode()).isZero();
    }

    public static void updateSecret(K3sContainer k3sContainer, String namespace, String name, Map<String, String> data, boolean isTLS)
        throws IOException, InterruptedException {
        String[] args = { "kubectl", "delete", "secret", name, "-n", namespace };
        Container.ExecResult execResult = k3sContainer.execInContainer(args);
        assertThat(execResult.getExitCode()).isZero();
        assertThat(execResult.getStdout()).contains("secret \"%s\" deleted".formatted(name));
        createSecret(k3sContainer, namespace, name, data, isTLS);
    }
}
