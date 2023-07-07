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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.*;

import io.gravitee.node.api.upgrader.UpgradeRecord;
import java.time.Instant;
import java.util.Date;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class UpgraderRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/upgrader-tests/";
    }

    @Test
    public void shouldCreate() {
        String id = "io.gravitee.rest.api.service.impl.upgrade.DefaultCategoryUpgrader";
        UpgradeRecord record = new UpgradeRecord();
        record.setId(id);
        record.setAppliedAt(new Date());
        record = upgraderRepository.create(record).blockingGet();
        UpgradeRecord created = upgraderRepository.findById(id).blockingGet();
        assertThat(created).usingRecursiveComparison().isEqualTo(record);
    }

    @Test
    public void shouldFind() {
        String id = "io.gravitee.rest.api.service.impl.upgrade.DefaultDashboardsUpgrader";
        UpgradeRecord record = upgraderRepository.findById(id).blockingGet();
        assertThat(record).isNotNull();
        assertThat(record.getAppliedAt()).isEqualTo(Date.from(Instant.ofEpochMilli(1439022010883L)));
    }

    @Override
    protected String getModelPackage() {
        return "io.gravitee.node.api.upgrader.";
    }
}
