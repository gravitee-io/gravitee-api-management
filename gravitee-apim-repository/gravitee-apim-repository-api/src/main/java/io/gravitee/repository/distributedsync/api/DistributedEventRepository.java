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
package io.gravitee.repository.distributedsync.api;

import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Date;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DistributedEventRepository {
    /**
     * Search for {@link DistributedEvent} matching the corresponding criteria.
     *
     * @param criteria Criteria to search for {@link DistributedEvent}.
     * @param page optional page number starting from 0, <code>null</code> means no paging.
     * @param size optional number of events to retrieve, <code>null</code> means no limit.
     *
     * @return the {@link Flowable} of the latest events.
     */
    Flowable<DistributedEvent> search(final DistributedEventCriteria criteria, final Long page, final Long size);

    /**
     * This method allows to create or update a distributed event if it does not exist in database or update it if it's present (replace old values by new ones).
     *
     * @param distributedEvent {@link DistributedEvent} to store
     * @return {@link Completable}
     */
    Completable createOrUpdate(final DistributedEvent distributedEvent);

    /**
     * This method allows to update all distributed event associated to reference id and type.
     *
     * @param refType {@link DistributedEventType} used to filter distributed events
     * @param refId {@link String} used to filter distributed events
     * @param syncAction {@link DistributedSyncAction} to update
     * @param updateAt {@link Date} to update
     * @return {@link Completable}
     */
    Completable updateAll(
        final DistributedEventType refType,
        final String refId,
        final DistributedSyncAction syncAction,
        final Date updateAt
    );
}
