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
package io.gravitee.repository.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * Every Management API node acknowledges the same command, and several nodes do it at the same
 * moment. Under PostgreSQL READ COMMITTED, a node whose update deletes and re-inserts the tag rows
 * while another node's transaction has them rewritten but not committed yet ends up inserting a
 * duplicate primary key once the other transaction commits.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcCommandRepositoryConcurrentUpdateTest extends AbstractRepositoryTest {

    private static final String TAG = "DATA_TO_INDEX";

    @Inject
    private CommandRepository commandRepository;

    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcDatabaseContainer jdbcDatabaseContainer;

    @Inject
    private Properties graviteeProperties;

    @Test
    public void should_acknowledge_while_another_node_acknowledges_the_same_command() throws Exception {
        // The race depends on PostgreSQL READ COMMITTED semantics (a blocked DELETE re-checks the
        // rows it waited on, but never sees rows inserted concurrently).
        assumeTrue(
            "Only reproducible on PostgreSQL",
            jdbcDatabaseContainer.getDockerImageName().contains(DatabaseConfigurationEnum.POSTGRESQL.getDockerImageName())
        );
        String prefix = graviteeProperties.getProperty("management.jdbc.prefix", "");
        String acknowledgments = prefix + "command_acknowledgments";
        String tags = prefix + "command_tags";

        Command command = new Command();
        command.setEnvironmentId("DEFAULT");
        command.setOrganizationId("DEFAULT");
        command.setFrom("node-0");
        command.setTo("*");
        command.setContent("content");
        command.setTags(List.of(TAG));
        command.setAcknowledgments(new ArrayList<>());
        commandRepository.create(command);

        // Both nodes read the command before either of them acknowledges it.
        Command seenByNode2 = commandRepository.findById(command.getId()).orElseThrow();
        seenByNode2.setAcknowledgments(new ArrayList<>(List.of("node-2")));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Connection node1 = dataSource.getConnection()) {
            node1.setAutoCommit(false);
            // Node 1's acknowledgment, as written by an update that rewrites the child rows, left
            // uncommitted while node 2 acknowledges.
            execute(node1, "delete from " + acknowledgments + " where command_id = ?", command.getId());
            execute(node1, "insert into " + acknowledgments + " ( command_id, acknowledgment ) values ( ?, ? )", command.getId(), "node-1");
            execute(node1, "delete from " + tags + " where command_id = ?", command.getId());
            execute(node1, "insert into " + tags + " ( command_id, tag ) values ( ?, ? )", command.getId(), TAG);

            Future<Command> node2 = executor.submit(() -> commandRepository.update(seenByNode2));
            waitUntilDoneOrBlocked(node1, node2);
            node1.commit();

            node2.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        Command stored = commandRepository.findById(command.getId()).orElseThrow();
        assertEquals(List.of(TAG), stored.getTags());
        assertTrue(stored.getAcknowledgments().contains("node-2"));
    }

    private static void waitUntilDoneOrBlocked(Connection observer, Future<?> future) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!future.isDone() && System.nanoTime() < deadline) {
            try (
                PreparedStatement ps = observer.prepareStatement("select count(*) from pg_locks where not granted");
                ResultSet rs = ps.executeQuery()
            ) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
            Thread.sleep(20);
        }
    }

    private static void execute(Connection connection, String sql, Object... args) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }

    @Override
    protected String getTestCasesPath() {
        return "";
    }

    @Override
    protected String getModelPackage() {
        return "";
    }

    @Override
    protected void createModel(Object object) {}
}
