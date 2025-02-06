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
package io.gravitee.repository.mongodb.common;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;

public class AbstractRepositoryConfigurationTest {

    private AbstractRepositoryConfiguration abstractRepositoryConfiguration;

    private MockEnvironment environment;

    @Mock
    private ApplicationContext applicationContext;

    @Before
    public void setUp() throws Exception {
        environment = new MockEnvironment();
        abstractRepositoryConfiguration = new AbstractRepositoryConfiguration(environment, applicationContext) {};
    }

    @Test
    public void getDatabaseNameWithURIEnvVar() {
        // Default database name is gravitee
        assertEquals("gravitee", abstractRepositoryConfiguration.getDatabaseName());

        environment.setProperty("management.mongodb.uri", "mongodb://localhost:27017/custom-db");
        assertEquals("custom-db", abstractRepositoryConfiguration.getDatabaseName());

        environment.setProperty("management.mongodb.uri", "mongodb://user:password@localhost:27017/custom-db?authSource=admin");
        assertEquals("custom-db", abstractRepositoryConfiguration.getDatabaseName());

        environment.setProperty("management.mongodb.uri", "mongodb://user:pa#ss+wo*rd@localhost:27017/custom-db?authSource=admin");
        assertEquals("custom-db", abstractRepositoryConfiguration.getDatabaseName());
    }

    @Test
    public void getDatabaseNameWithDBNameEnvVar() {
        // Default database name is gravitee
        assertEquals("gravitee", abstractRepositoryConfiguration.getDatabaseName());

        environment.setProperty("management.mongodb.dbname", "custom-db");
        assertEquals("custom-db", abstractRepositoryConfiguration.getDatabaseName());
    }
}
