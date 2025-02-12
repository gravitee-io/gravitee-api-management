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
package io.gravitee.apim.integration.tests.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesHelper {

    public static void createNamespace(ContainerState k8sContainer, String name) throws IOException, InterruptedException {
        Container.ExecResult execResult = k8sContainer.execInContainer("kubectl", "create", "namespace", name);
        assertThat(execResult.getExitCode()).isZero();
        assertThat(execResult.getStdout()).contains("namespace/%s created".formatted(name));
    }

    public static void createSecret(ContainerState k8sContainer, String namespace, String name, Map<String, String> data)
        throws IOException, InterruptedException {
        createSecret(k8sContainer, namespace, name, data, false);
    }

    public static void createSecret(ContainerState k8sContainer, String namespace, String name, Map<String, String> data, boolean isTLS)
        throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("kubectl", "create", "secret", isTLS ? "tls" : "generic", name, "-n", namespace));
        if (isTLS) {
            String certPath = "/kindcontainer/%s-tls.crt".formatted(UUID.randomUUID());
            String keyPath = "/kindcontainer/%s-tls.key".formatted(UUID.randomUUID());
            k8sContainer.copyFileToContainer(Transferable.of(Objects.requireNonNull(data.get("tls.crt"))), certPath);
            k8sContainer.copyFileToContainer(Transferable.of(Objects.requireNonNull(data.get("tls.key"))), keyPath);
            args.add("--cert=%s".formatted(certPath));
            args.add("--key=%s".formatted(keyPath));
        } else {
            data.entrySet().stream().map(entry -> "--from-literal=%s=%s".formatted(entry.getKey(), entry.getValue())).forEach(args::add);
        }
        Container.ExecResult execResult = k8sContainer.execInContainer(args.toArray(new String[0]));
        assertThat(execResult.getStderr()).isEmpty();
        assertThat(execResult.getStdout()).contains("secret/%s created".formatted(name));
        assertThat(execResult.getExitCode()).isZero();
    }

    public static void updateSecret(ContainerState k8sContainer, String namespace, String name, Map<String, String> data, boolean isTLS)
        throws IOException, InterruptedException {
        String[] args = { "kubectl", "delete", "secret", name, "-n", namespace };
        Container.ExecResult execResult = k8sContainer.execInContainer(args);
        assertThat(execResult.getExitCode()).isZero();
        assertThat(execResult.getStdout()).contains("secret \"%s\" deleted".formatted(name));
        createSecret(k8sContainer, namespace, name, data, isTLS);
    }

    public static String createToken(ContainerState k8sContainer, String sa) throws IOException, InterruptedException {
        String[] args = { "kubectl", "create", "token", sa };
        Container.ExecResult execResult = k8sContainer.execInContainer(args);
        assertThat(execResult.getExitCode()).isZero();
        return execResult.getStdout();
    }

    public static void apply(ContainerState k8sContainer, String file) throws IOException, InterruptedException {
        String destination = UUID.randomUUID().toString();
        String containerPath = "/kindcontainer/" + destination + ".yaml";
        k8sContainer.copyFileToContainer(MountableFile.forHostPath(file), containerPath);
        String[] args = { "kubectl", "apply", "-f", containerPath };
        Container.ExecResult execResult = k8sContainer.execInContainer(args);
        System.out.println(execResult.getStdout());
        assertThat(execResult.getExitCode()).isZero();
    }

    public static String getServiceAccountToken(ContainerState k8sContainer, String secret) throws IOException, InterruptedException {
        String[] args = { "kubectl", "get", "secret", secret, "-o", "go-template='{{ .data.token }}'" };
        Container.ExecResult execResult = k8sContainer.execInContainer(args);
        assertThat(execResult.getExitCode()).isZero();
        String stdout = execResult.getStdout();
        System.out.println("'" + stdout + "'");
        return new String(Base64.getMimeDecoder().decode(stdout));
    }
}
