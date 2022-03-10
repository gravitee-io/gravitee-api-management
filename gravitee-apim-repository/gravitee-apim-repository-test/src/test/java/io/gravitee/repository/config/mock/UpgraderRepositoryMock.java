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
package io.gravitee.repository.config.mock;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.node.api.upgrader.UpgraderRepository;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UpgraderRepositoryMock extends AbstractRepositoryMock<UpgraderRepository> {

    public UpgraderRepositoryMock() {
        super(UpgraderRepository.class);
    }

    @Override
    void prepare(UpgraderRepository upgraderRepository) throws Exception {
        final UpgradeRecord record = mock(UpgradeRecord.class);

        when(record.getId()).thenReturn("upgrader#1");
        when(record.getStatus()).thenReturn(UpgradeRecord.STATUS_SUCCESS);
        when(upgraderRepository.findById("upgrader#1")).thenReturn(Maybe.just(record));

        // create
        final UpgradeRecord createdUpgradeRecord = new UpgradeRecord("");
        createdUpgradeRecord.setId("new-upgrader");
        createdUpgradeRecord.setStartedAt(new Date(1464739200000L));
        createdUpgradeRecord.setStoppedAt(new Date(1464739200000L));
        createdUpgradeRecord.setStatus(UpgradeRecord.STATUS_RUNNING);

        when(upgraderRepository.create(any())).thenReturn(Single.just(createdUpgradeRecord));
        when(upgraderRepository.findById("new-upgrader")).thenReturn(Maybe.just(createdUpgradeRecord));

        // update
        final UpgradeRecord updateUpgradeRecord = new UpgradeRecord("upgrader#1");
        updateUpgradeRecord.setId("upgrader#1");
        updateUpgradeRecord.setStartedAt(new Date(1464739200000L));
        updateUpgradeRecord.setStoppedAt(new Date(1464739200000L));
        updateUpgradeRecord.setStatus(UpgradeRecord.STATUS_RUNNING);

        when(upgraderRepository.update(argThat(o -> o != null && o.getId().equals("upgrader#1"))))
            .thenReturn(Single.just(updateUpgradeRecord));

        // updateUnknownUpgradeRecord
        when(upgraderRepository.update(argThat(o -> o == null || o.getId().equals("unknown"))))
            .thenReturn(Single.error(new IllegalStateException()));
    }
}
