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

import static org.junit.Assert.assertTrue;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
public class FlowRepositoryPKTest extends AbstractManagementRepositoryTest {

    @Inject
    private DataSource dataSource;

    @Override
    protected String getTestCasesPath() {
        return "/data/flow-tests/";
    }

    @Override
    protected String getModelPackage() {
        return super.getModelPackage() + "flow.";
    }

    @Test
    public void shouldHaveNoNullIds() {
        final JdbcTemplate jt = new JdbcTemplate(dataSource);
        AtomicBoolean areIdsOk = new AtomicBoolean(false);
        jt.query("SELECT COUNT(*) FROM test_gio_flow_steps WHERE test_gio_flow_steps.id IS NULL", rs -> {
            areIdsOk.set(rs.getInt(1) == 0);
        });
        assertTrue(areIdsOk.get());
    }
}
