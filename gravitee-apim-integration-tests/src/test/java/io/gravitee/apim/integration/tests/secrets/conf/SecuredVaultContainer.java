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

import com.github.dockerjava.api.model.Capability;
import io.github.jopenlibs.vault.SslConfig;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.lifecycle.TestLifecycleAware;

/**
 * Sets up and exposes utilities for dealing with a Docker-hosted instance of Vault, for integration
 * tests.
 *
 */
@Slf4j
class SecuredVaultContainer extends GenericContainer<SecuredVaultContainer> implements TestLifecycleAware {

    static final String USER_ID = "fake_user";
    static final String PASSWORD = "fake_password";
    static final int MAX_RETRIES = 5;
    static final int RETRY_MILLIS = 1000;
    static final String TEST_POLICY_NAME = "tester_pol";

    static final String CURRENT_WORKING_DIRECTORY = "target" + File.separator + "vault";
    static final String SSL_DIRECTORY = CURRENT_WORKING_DIRECTORY + File.separator + "ssl";
    static final String CERT_PEMFILE = SSL_DIRECTORY + File.separator + "root-cert.pem";

    static final String CLIENT_CERT_PEMFILE = SSL_DIRECTORY + File.separator + "client-cert.pem";
    static final String TESTROLE = "testrole";
    static final String HASHICORP_VAULT_IMAGE = "hashicorp/vault:1.13.3";

    final String CONTAINER_STARTUP_SCRIPT = "/vault/config/startup.sh";
    final String CONTAINER_CONFIG_FILE = "/vault/config/config.json";
    final String CONTAINER_OPENSSL_CONFIG_FILE = "/vault/config/libressl.conf";
    final String CONTAINER_SSL_DIRECTORY = "/vault/config/ssl";
    final String CONTAINER_CERT_PEMFILE = CONTAINER_SSL_DIRECTORY + "/vault-cert.pem";
    final String CONTAINER_CLIENT_CERT_PEMFILE = CONTAINER_SSL_DIRECTORY + "/client-cert.pem";
    final String TEST_POLICY_FILE = "/home/vault/testPolicy.hcl";

    private String rootToken;
    private String unsealKey;

    public SecuredVaultContainer() {
        super(HASHICORP_VAULT_IMAGE);
        this.withNetworkAliases("vault")
            // script that will create certs and start vault
            .withClasspathResourceMapping("/vault/startup.sh", CONTAINER_STARTUP_SCRIPT, BindMode.READ_ONLY)
            // config to setup vault as secured
            .withClasspathResourceMapping("/vault/config.json", CONTAINER_CONFIG_FILE, BindMode.READ_ONLY)
            // conf for openssl when setting up scripts for openssl
            .withClasspathResourceMapping("/vault/libressl.conf", CONTAINER_OPENSSL_CONFIG_FILE, BindMode.READ_ONLY)
            // policy to bind to various auth
            .withClasspathResourceMapping("/vault/testPolicy.hcl", TEST_POLICY_FILE, BindMode.READ_ONLY)
            .withFileSystemBind(SSL_DIRECTORY, CONTAINER_SSL_DIRECTORY, BindMode.READ_WRITE)
            .withCreateContainerCmdModifier(command -> command.withCapAdd(Capability.IPC_LOCK))
            .withExposedPorts(8200, 8280)
            .withCommand("/bin/sh " + CONTAINER_STARTUP_SCRIPT)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(
                // Vault is configured to run a plain HTTP listener on port 8280, purely for purposes of detecting
                // when the Docker container is fully ready.
                new HttpWaitStrategy().forPort(8280).forPath("/v1/sys/seal-status").forStatusCode(HttpURLConnection.HTTP_OK)
            );
    }

    public void initAndUnsealVault() throws IOException, InterruptedException {
        // Initialize the Vault server
        final ExecResult initResult = runCommand(
            false,
            "vault",
            "operator",
            "init",
            "-ca-cert=" + CONTAINER_CERT_PEMFILE,
            "-key-shares=1",
            "-key-threshold=1",
            "-format=json"
        );

        final String stdout = initResult.getStdout().replaceAll("\\r?\\n", "");
        JsonObject initJson = Json.parse(stdout).asObject();
        this.unsealKey = initJson.get("unseal_keys_b64").asArray().get(0).asString();
        this.rootToken = initJson.get("root_token").asString();

        System.out.println("Root token: " + rootToken);

        // Unseal the Vault server
        runCommand(false, "vault", "operator", "unseal", "-ca-cert=" + CONTAINER_CERT_PEMFILE, unsealKey);
    }

    public void loginAndLoadTestPolicy() throws IOException, InterruptedException {
        runCommand("vault", "login", "-ca-cert=" + CONTAINER_CERT_PEMFILE, rootToken);
        runCommand("vault", "policy", "write", "-ca-cert=" + CONTAINER_CERT_PEMFILE, TEST_POLICY_NAME, TEST_POLICY_FILE);
    }

    public void setupUserPassAuth() throws IOException, InterruptedException {
        runCommand("vault", "auth", "enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "userpass");
        runCommand(
            "vault",
            "write",
            "-ca-cert=" + CONTAINER_CERT_PEMFILE,
            "auth/userpass/users/" + USER_ID,
            "password=" + PASSWORD,
            "policies=" + TEST_POLICY_NAME
        );
    }

    /**
     * Prepares the Vault server for testing of the AppRole auth backend (i.e. mounts the backend
     * and populates test data).
     */
    public void setupAppRoleAuth() throws IOException, InterruptedException, VaultException {
        runCommand("vault", "auth", "enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "approle");
        runCommand(
            "vault",
            "write",
            "-ca-cert=" + CONTAINER_CERT_PEMFILE,
            "auth/approle/role/" + TESTROLE,
            "secret_id_ttl=10m",
            "token_ttl=20m",
            "token_max_ttl=30m",
            "secret_id_num_uses=40",
            "policies=" + TEST_POLICY_NAME
        );
    }

    /**
     * Prepares the Vault server for testing of the TLS Certificate auth backend (i.e. mounts the
     * backend and registers the certificate and private key for client auth).
     */
    public void setupCertAuth(String cert) throws IOException, InterruptedException {
        runCommand("vault", "auth", "enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "cert");
        copyFileToContainer(Transferable.of(cert), CONTAINER_CLIENT_CERT_PEMFILE);
        runCommand(
            "vault",
            "write",
            "-ca-cert=" + CONTAINER_CERT_PEMFILE,
            "auth/cert/certs/web",
            "display_name=web",
            "policies=" + TEST_POLICY_NAME,
            "certificate=@" + CONTAINER_CLIENT_CERT_PEMFILE,
            "ttl=3600"
        );
    }

    /**
     * Prepares the Vault server for testing of the Database Backend using Postgres
     */

    public void setEngineVersions() throws IOException, InterruptedException {
        // Upgrade default secrets/ Engine to V2, set a new V1 secrets path at "kv-v1/"
        runCommand("vault", "secrets", "enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "-path=secret", "kv");
        runCommand("vault", "kv", "enable-versioning", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "secret/");
        runCommand("vault", "secrets", "enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "-path=kv-v1", "-version=1", "kv");
    }

    /**
     * Constructs an instance of the Vault driver with sensible defaults, configured to use the
     * supplied token for authentication.
     */
    public Vault getVault(final String token) throws VaultException {
        final VaultConfig config = new VaultConfig()
            .address(getAddress())
            .token(token)
            .openTimeout(5)
            .readTimeout(30)
            .sslConfig(new SslConfig().pemFile(new File(CERT_PEMFILE)).build())
            .build();
        return Vault.create(config).withRetries(MAX_RETRIES, RETRY_MILLIS);
    }

    /**
     * Constructs an instance of the Vault driver with sensible defaults, configured to the use the
     * root token for authentication.
     */
    public Vault getRootVault() throws VaultException {
        return getVault(rootToken).withRetries(MAX_RETRIES, RETRY_MILLIS);
    }

    private String getAddress() {
        return String.format("https://%s:%d", getHost(), getPort());
    }

    public int getPort() {
        return getMappedPort(8200);
    }

    public ExecResult runCommand(final String... command) throws IOException, InterruptedException {
        return runCommand(true, command);
    }

    public ExecResult runCommand(boolean checkResult, final String... command) throws IOException, InterruptedException {
        log.info("Command: {}", String.join(" ", command));
        final ExecResult result = execInContainer(command);
        final String out = result.getStdout();
        final String err = result.getStderr();
        if (out != null && !out.isEmpty()) {
            log.info("Command stdout: {}", result.getStdout());
        }
        if (err != null && !err.isEmpty()) {
            log.error("Command stderr: {}", result.getStderr());
        }
        if (checkResult) {
            assertThat(out).startsWith("Success!");
        }

        return result;
    }

    public record AppRoleIDs(String roleId, String secretId) {}

    public AppRoleIDs newAppRoleSecretId() throws IOException, InterruptedException {
        Container.ExecResult result = runCommand(
            false,
            "vault",
            "read",
            "-ca-cert=" + CONTAINER_CERT_PEMFILE,
            "-field=role_id",
            "auth/approle/role/" + TESTROLE + "/role-id"
        );
        assertThat(result.getStderr()).isEmpty();
        assertThat(result.getStdout()).isNotEmpty();
        var roleId = result.getStdout();
        result = runCommand(
            false,
            "vault",
            "write",
            "-f",
            "-ca-cert=" + CONTAINER_CERT_PEMFILE,
            "-field=secret_id",
            "auth/approle/role/" + TESTROLE + "/secret-id"
        );
        assertThat(result.getStderr()).isEmpty();
        assertThat(result.getStdout()).isNotEmpty();
        var secretId = result.getStdout();

        return new AppRoleIDs(roleId, secretId);
    }
}
