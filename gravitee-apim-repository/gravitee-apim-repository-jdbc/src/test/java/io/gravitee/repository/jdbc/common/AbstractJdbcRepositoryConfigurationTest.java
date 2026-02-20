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
package io.gravitee.repository.jdbc.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zaxxer.hikari.HikariDataSource;
import io.gravitee.repository.jdbc.DatabaseConfigurationEnum;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbstractJdbcRepositoryConfigurationTest {

    private static class TestJdbcConfiguration extends AbstractJdbcRepositoryConfiguration {

        @Override
        protected String getScope() {
            return "management";
        }
    }

    private static class TestRateLimitConfiguration extends AbstractJdbcRepositoryConfiguration {

        @Override
        protected String getScope() {
            return "ratelimit";
        }
    }

    @Mock
    private Environment env;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ConfigurableApplicationContext configurableApplicationContext;

    @Mock
    private ConfigurableListableBeanFactory beanFactory;

    @Mock
    private DataSource mockDataSource;

    private TestJdbcConfiguration configuration;
    private TestRateLimitConfiguration rateLimitConfiguration;

    private static final String POSTGRES_TYPE = DatabaseConfigurationEnum.POSTGRESQL.name().toLowerCase();
    private static final String SQLSERVER_TYPE = DatabaseConfigurationEnum.SQLSERVER.name().toLowerCase();

    @BeforeEach
    void setUp() {
        configuration = new TestJdbcConfiguration();
        rateLimitConfiguration = new TestRateLimitConfiguration();
    }

    @Test
    void shouldSetEscapeFromPostgresJdbcUrl() {
        AbstractJdbcRepositoryConfiguration.setEscapeReservedWordFromJDBCUrl("jdbc:postgresql://localhost:5432/db");
        assertEquals("\"user\"", AbstractJdbcRepositoryConfiguration.escapeReservedWord("user"));
    }

    @Test
    void shouldSetEscapeFromSqlServerJdbcUrl() {
        AbstractJdbcRepositoryConfiguration.setEscapeReservedWordFromJDBCUrl("jdbc:sqlserver://localhost:1433;databaseName=gravitee");
        assertEquals("[order]", AbstractJdbcRepositoryConfiguration.escapeReservedWord("order"));
    }

    @Test
    void shouldHandleNullJdbcUrl() {
        assertDoesNotThrow(() -> AbstractJdbcRepositoryConfiguration.setEscapeReservedWordFromJDBCUrl(null));
    }

    @Test
    void shouldCreatePagingClauseForSqlServer() {
        AbstractJdbcRepositoryConfiguration.setEscapeReservedWordFromDatabaseType(SQLSERVER_TYPE);
        String clause = AbstractJdbcRepositoryConfiguration.createPagingClause(10, 20);
        assertEquals("OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY ", clause);
    }

    @Test
    void shouldCreateOffsetClauseForSqlServer() {
        AbstractJdbcRepositoryConfiguration.setEscapeReservedWordFromDatabaseType(SQLSERVER_TYPE);
        String clause = AbstractJdbcRepositoryConfiguration.createOffsetClause(100L);
        assertEquals("OFFSET 100 ROWS ", clause);
    }

    @Test
    void shouldReturnEmptyOffsetClauseWhenNull() {
        String clause = AbstractJdbcRepositoryConfiguration.createOffsetClause(null);
        assertEquals("", clause);
    }

    @Test
    void shouldReturnCorrectScopesFromConcreteImplementations() {
        assertEquals("management", configuration.getScope());
        assertEquals("ratelimit", rateLimitConfiguration.getScope());
    }

    @Test
    void shouldCreateJdbcTemplate() {
        JdbcTemplate jdbcTemplate = configuration.graviteeJdbcTemplate(mockDataSource);
        assertNotNull(jdbcTemplate);
        assertEquals(mockDataSource, jdbcTemplate.getDataSource());
    }

    @Test
    void shouldRegisterBeansWhenNoDataSourceExists() {
        stubEnvForDataSourceDefaults();

        when(configurableApplicationContext.getBeansOfType(DataSource.class)).thenReturn(java.util.Collections.emptyMap());
        when(configurableApplicationContext.getBeanFactory()).thenReturn(beanFactory);
        when(applicationContext.getParent()).thenReturn(configurableApplicationContext);

        injectEnvironment(configuration, env);

        try (MockedConstruction<HikariDataSource> mocked = mockConstruction(HikariDataSource.class)) {
            configuration.setApplicationContext(applicationContext);

            assertEquals(1, mocked.constructed().size());
            verify(beanFactory).registerSingleton(eq("graviteeDataSource"), any(DataSource.class));
            verify(beanFactory).registerSingleton(eq("graviteeTransactionManager"), any());
        }
    }

    @Test
    void shouldHandleApplicationContextWithoutParent() {
        stubEnvForDataSourceDefaults();

        when(configurableApplicationContext.getParent()).thenReturn(null);
        when(configurableApplicationContext.getBeansOfType(DataSource.class)).thenReturn(java.util.Collections.emptyMap());
        when(configurableApplicationContext.getBeanFactory()).thenReturn(beanFactory);

        injectEnvironment(configuration, env);

        try (MockedConstruction<HikariDataSource> mocked = mockConstruction(HikariDataSource.class)) {
            configuration.setApplicationContext(configurableApplicationContext);

            assertEquals(1, mocked.constructed().size());
            verify(beanFactory).registerSingleton(eq("graviteeDataSource"), any(DataSource.class));
        }
    }

    @Test
    void shouldNotRegisterBeansWhenDataSourceExists() {
        when(applicationContext.getParent()).thenReturn(configurableApplicationContext);
        when(configurableApplicationContext.getBeansOfType(DataSource.class)).thenReturn(
            java.util.Map.of("existingDataSource", mockDataSource)
        );

        configuration.setApplicationContext(applicationContext);

        verify(beanFactory, never()).registerSingleton(anyString(), any());
    }

    private void stubEnvForDataSourceDefaults() {
        when(env.getProperty("repositories.management.jdbc.url", String.class)).thenReturn("jdbc:h2:mem:test");
        when(env.getProperty("management.jdbc.url", String.class, null)).thenReturn("jdbc:h2:mem:test");
        when(env.getProperty("repositories.management.jdbc.username", String.class)).thenReturn("sa");
        when(env.getProperty("management.jdbc.username", String.class, null)).thenReturn("sa");
        when(env.getProperty("repositories.management.jdbc.password", String.class)).thenReturn("");
        when(env.getProperty("management.jdbc.password", String.class, null)).thenReturn("");
        when(env.getProperty("repositories.management.jdbc.schema", String.class)).thenReturn(null);
        when(env.getProperty("management.jdbc.schema", String.class, null)).thenReturn(null);

        when(env.getProperty(eq("repositories.management.jdbc.pool.autoCommit"), eq(Boolean.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.pool.autoCommit"), eq(Boolean.class), anyBoolean())).thenReturn(true);

        when(env.getProperty(eq("repositories.management.jdbc.pool.connectionTimeout"), eq(Long.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.pool.connectionTimeout"), eq(Long.class), anyLong())).thenReturn(10000L);

        when(env.getProperty(eq("repositories.management.jdbc.pool.idleTimeout"), eq(Long.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.pool.idleTimeout"), eq(Long.class), anyLong())).thenReturn(600000L);

        when(env.getProperty(eq("repositories.management.jdbc.pool.maxLifetime"), eq(Long.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.pool.maxLifetime"), eq(Long.class), anyLong())).thenReturn(1800000L);

        when(env.getProperty(eq("repositories.management.jdbc.pool.minIdle"), eq(Integer.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.pool.minIdle"), eq(Integer.class), anyInt())).thenReturn(10);

        when(env.getProperty(eq("repositories.management.jdbc.pool.maxPoolSize"), eq(Integer.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.pool.maxPoolSize"), eq(Integer.class), anyInt())).thenReturn(10);

        when(env.getProperty(eq("repositories.management.jdbc.pool.keepaliveTime"), eq(Integer.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.pool.keepaliveTime"), eq(Integer.class), anyInt())).thenReturn(0);

        when(env.getProperty(eq("repositories.management.jdbc.pool.registerMbeans"), eq(Boolean.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.pool.registerMbeans"), eq(Boolean.class), anyBoolean())).thenReturn(true);

        when(env.getProperty(eq("repositories.management.jdbc.liquibase"), eq(Boolean.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.liquibase"), eq(Boolean.class), anyBoolean())).thenReturn(false);

        when(env.getProperty(eq("repositories.management.jdbc.sqlOnly"), eq(Boolean.class))).thenReturn(null);
        when(env.getProperty(eq("management.jdbc.sqlOnly"), eq(Boolean.class), anyBoolean())).thenReturn(false);
    }

    private void injectEnvironment(Object target, Environment environment) {
        try {
            java.lang.reflect.Field envField = AbstractJdbcRepositoryConfiguration.class.getDeclaredField("env");
            envField.setAccessible(true);
            envField.set(target, environment);
        } catch (Exception e) {
            fail("Failed to inject environment: " + e.getMessage());
        }
    }
}
