/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository;

import io.gravitee.common.util.Version;
import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.TestObserver;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;

public class UpgraderRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/upgrader-tests/";
    }

    @Override
    protected Class getClassFromFileName(String baseName) {
        return UpgradeRecord.class;
    }

    @Test
    public void shouldFindById() throws Exception {
        final TestObserver<UpgradeRecord> testObserver = upgraderRepository.findById("upgrader#1").test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(
            new Predicate<UpgradeRecord>() {
                @Override
                public boolean test(@NonNull UpgradeRecord record) throws Exception {
                    Assert.assertEquals(UpgradeRecord.STATUS_SUCCESS, record.getStatus());

                    return true;
                }
            }
        );
    }

    @Test
    public void shouldCreate() throws Exception {
        final UpgradeRecord record = new UpgradeRecord("new-upgrader");
        record.setStatus(UpgradeRecord.STATUS_RUNNING);
        record.setStartedAt(new Date());
        record.setVersion(Version.RUNTIME_VERSION.MAJOR_VERSION);

        TestObserver<UpgradeRecord> testObserver = upgraderRepository.create(record).test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(createdUpgradeRecord -> createdUpgradeRecord.getId().equals("new-upgrader"));

        testObserver = upgraderRepository.findById("new-upgrader").test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();

        testObserver.assertValue(
            new Predicate<UpgradeRecord>() {
                @Override
                public boolean test(@NonNull UpgradeRecord upgradeRecord) throws Exception {
                    Assert.assertEquals("Invalid saved upgrade record name.", record.getId(), upgradeRecord.getId());
                    Assert.assertEquals("Invalid tenant upgrade record status.", record.getStatus(), upgradeRecord.getStatus());

                    return true;
                }
            }
        );
    }

    @Test
    public void shouldUpdate() throws Exception {
        TestObserver<UpgradeRecord> testObserver = new TestObserver<>();
        upgraderRepository.findById("upgrader#1").subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        testObserver.assertValueCount(1);

        testObserver.assertValue(
            new Predicate<UpgradeRecord>() {
                @Override
                public boolean test(@NonNull UpgradeRecord record) throws Exception {
                    Assert.assertEquals("Invalid saved upgrade record status.", UpgradeRecord.STATUS_SUCCESS, record.getStatus());

                    return true;
                }
            }
        );

        UpgradeRecord record = new UpgradeRecord("upgrader#1");
        record.setStartedAt(new Date());
        record.setVersion(Version.RUNTIME_VERSION.MAJOR_VERSION);
        record.setStatus(UpgradeRecord.STATUS_RUNNING);

        testObserver = upgraderRepository.update(record).test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(updatedUpgradeRecord -> updatedUpgradeRecord.getStatus() == UpgradeRecord.STATUS_RUNNING);
    }

    @Test
    public void shouldNotUpdateUnknown() throws Exception {
        final UpgradeRecord unknownRecord = new UpgradeRecord("unknown");

        final TestObserver<UpgradeRecord> testObserver = upgraderRepository.update(unknownRecord).test();

        testObserver.assertNotComplete();
        testObserver.assertError(IllegalStateException.class);
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        final TestObserver<UpgradeRecord> testObserver = upgraderRepository.update(null).test();

        testObserver.assertNotComplete();
        testObserver.assertError(IllegalStateException.class);
    }
}
