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
package io.gravitee.repository.redis.vertx;

import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPTS_RATELIMIT_LUA;
import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPT_RATELIMIT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.gravitee.plugin.configurations.redis.HostAndPort;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.plugin.configurations.redis.RedisClusterOptions;
import io.gravitee.plugin.configurations.redis.RedisSentinelOptions;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Sentinel and cluster connections do not expose their sockets the way a standalone pooled connection does. Dropping one
 * must not leave a Redis client behind on the Vert.x close future: on Vert.x 5.0.x a closed client stays registered there
 * until Vert.x stops.
 *
 * <p>Both topologies run in a single container whose ports are published on the same host ports, so that the addresses
 * announced by Sentinel and by the cluster ({@code 127.0.0.1:port}) are reachable from the test.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientTopologyCloseFutureRetentionTest {

    private static final int RECONNECTS = 6;
    private static final Map<String, String> SCRIPTS = Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA);

    abstract static class TopologyRetention {

        protected final Vertx vertx = Vertx.vertx();
        protected GenericContainer<?> redis;
        protected int dataPort;

        abstract GenericContainer<?> startRedis();

        abstract RedisClientOptions options();

        @BeforeAll
        void start() {
            assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
            redis = startRedis();
        }

        @AfterAll
        void stop() {
            if (redis != null) {
                redis.stop();
            }
            vertx.close().await();
        }

        @Test
        void should_not_retain_a_client_per_discarded_connection() throws Exception {
            Context context = vertx.getOrCreateContext();
            CountingConnectFactory factory = new CountingConnectFactory(vertx);
            int before = RetainedClients.count(vertx);

            RedisClient client = createOn(context, factory);
            try {
                awaitReady(context, client);
                for (int i = 0; i < RECONNECTS; i++) {
                    int connects = factory.connects.get();
                    // What the repositories report when a command fails on a dead or half-open socket.
                    context.runOnContext(v -> client.notifyConnectionFailure(new ClosedChannelException()));
                    Awaitility.await()
                        .atMost(10, TimeUnit.SECONDS)
                        .until(() -> factory.connects.get() > connects);
                    awaitReady(context, client);
                }

                assertThat(RetainedClients.count(vertx) - before)
                    .as("Redis clients retained by the close future after %d discarded connections", RECONNECTS)
                    .isLessThanOrEqualTo(1);
            } finally {
                client.close();
            }
        }

        @Test
        void should_not_retain_a_client_per_connect_attempt_failing_after_connection() throws Exception {
            Context context = vertx.getOrCreateContext();
            CountingConnectFactory factory = new CountingConnectFactory(vertx);
            int before = RetainedClients.count(vertx);
            // The connection opens, then SCRIPT LOAD is refused: the next attempt discards that connection.
            masterCli("ACL", "SETUSER", "default", "-@scripting");

            RedisClient client = createOn(context, factory);
            try {
                Awaitility.await()
                    .atMost(30, TimeUnit.SECONDS)
                    .until(() -> factory.connects.get() > RECONNECTS);

                assertThat(RetainedClients.count(vertx) - before)
                    .as("Redis clients retained by the close future after %d failed connect attempts", factory.connects.get())
                    .isLessThanOrEqualTo(1);
            } finally {
                client.close();
                masterCli("ACL", "SETUSER", "default", "+@scripting");
            }
        }

        private void masterCli(String... command) throws Exception {
            String[] args = new String[command.length + 3];
            args[0] = "redis-cli";
            args[1] = "-p";
            args[2] = String.valueOf(dataPort);
            System.arraycopy(command, 0, args, 3, command.length);
            var result = redis.execInContainer(args);
            assertThat(result.getStdout().trim()).as("redis-cli %s: %s", List.of(command), result.getStderr()).isEqualTo("OK");
        }

        private RedisClient createOn(Context context, CountingConnectFactory factory) throws Exception {
            // Created on the context so that its first connection runs on the same event loop as the test's calls.
            CompletableFuture<RedisClient> created = new CompletableFuture<>();
            context.runOnContext(v -> created.complete(new RedisClient(vertx, factory, options(), SCRIPTS)));
            return created.get(5, TimeUnit.SECONDS);
        }

        private static void awaitReady(Context context, RedisClient client) {
            Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(() -> ping(context, client));
        }

        private static boolean ping(Context context, RedisClient client) throws Exception {
            CompletableFuture<Boolean> pong = new CompletableFuture<>();
            context.runOnContext(v ->
                client
                    .redisApi()
                    .compose(api -> api.ping(List.of()))
                    .onComplete((response, failure) -> pong.complete(failure == null))
            );
            return pong.get(5, TimeUnit.SECONDS);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Sentinel extends TopologyRetention {

        private int sentinelPort;

        @Override
        GenericContainer<?> startRedis() {
            dataPort = findFreePort(22000);
            sentinelPort = findFreePort(dataPort + 1);
            GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                .withExposedPorts(dataPort, sentinelPort)
                .withCreateContainerCmdModifier(cmd ->
                    cmd
                        .getHostConfig()
                        .withPortBindings(
                            new PortBinding(Ports.Binding.bindPort(dataPort), new ExposedPort(dataPort)),
                            new PortBinding(Ports.Binding.bindPort(sentinelPort), new ExposedPort(sentinelPort))
                        )
                )
                .withCommand(
                    "sh",
                    "-c",
                    "redis-server --port " +
                        dataPort +
                        " --daemonize yes" +
                        " && printf 'port " +
                        sentinelPort +
                        "\\nsentinel monitor mymaster 127.0.0.1 " +
                        dataPort +
                        " 1\\n' > /tmp/sentinel.conf" +
                        " && exec redis-sentinel /tmp/sentinel.conf"
                )
                .waitingFor(Wait.forLogMessage(".*\\+monitor master mymaster.*", 1));
            container.start();
            return container;
        }

        @Override
        RedisClientOptions options() {
            return RedisClientOptions.builder()
                .host("127.0.0.1")
                .port(dataPort)
                .connectTimeout(2000)
                .sentinel(
                    RedisSentinelOptions.builder()
                        .masterId("mymaster")
                        .nodes(List.of(HostAndPort.builder().host("127.0.0.1").port(sentinelPort).build()))
                        .build()
                )
                .build();
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Cluster extends TopologyRetention {

        @Override
        GenericContainer<?> startRedis() {
            dataPort = findFreePort(20000);
            GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                .withExposedPorts(dataPort)
                .withCreateContainerCmdModifier(cmd ->
                    cmd.getHostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(dataPort), new ExposedPort(dataPort)))
                )
                .withCommand(
                    "redis-server --port " +
                        dataPort +
                        " --cluster-enabled yes --cluster-node-timeout 5000" +
                        " --cluster-announce-ip 127.0.0.1 --cluster-announce-port " +
                        dataPort
                )
                .waitingFor(Wait.forListeningPort());
            container.start();
            AtomicReference<String> info = new AtomicReference<>("");
            try {
                info.set(
                    container
                        .execInContainer("redis-cli", "-p", String.valueOf(dataPort), "cluster", "addslotsrange", "0", "16383")
                        .toString()
                );
                Awaitility.await()
                    .atMost(30, TimeUnit.SECONDS)
                    .until(() -> {
                        info.set(container.execInContainer("redis-cli", "-p", String.valueOf(dataPort), "cluster", "info").getStdout());
                        return info.get().contains("cluster_state:ok");
                    });
            } catch (Exception e) {
                String logs = container.getLogs();
                container.stop();
                throw new IllegalStateException("Redis cluster did not start: " + info.get() + "\n" + logs, e);
            }
            return container;
        }

        @Override
        RedisClientOptions options() {
            return RedisClientOptions.builder()
                .host("127.0.0.1")
                .port(dataPort)
                .connectTimeout(2000)
                .cluster(
                    RedisClusterOptions.builder().nodes(List.of(HostAndPort.builder().host("127.0.0.1").port(dataPort).build())).build()
                )
                .build();
        }
    }

    // A low range: the OS ephemeral range can sit above 55535, where a cluster bus port (data port + 10000) cannot exist.
    private static int findFreePort(int from) {
        for (int port = from; port < from + 1000; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                return socket.getLocalPort();
            } catch (IOException ignored) {
                // in use: try the next one
            }
        }
        throw new IllegalStateException("No free port from " + from);
    }
}
