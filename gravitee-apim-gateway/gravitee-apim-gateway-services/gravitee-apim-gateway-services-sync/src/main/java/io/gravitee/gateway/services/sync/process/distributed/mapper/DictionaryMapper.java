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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncMetrics;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class DictionaryMapper {

    private final ObjectMapper objectMapper;
    private final DistributedSyncMetrics metrics;

    public DictionaryMapper(final ObjectMapper objectMapper) {
        this(objectMapper, new DistributedSyncMetrics());
    }

    public Maybe<DictionaryDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                final Dictionary dictionary = objectMapper.readValue(event.getPayload(), Dictionary.class);
                return DictionaryDeployable.builder()
                    .id(dictionary.getId())
                    .dictionary(dictionary)
                    .syncAction(SyncActionMapper.to(event.getSyncAction()))
                    .build();
            } catch (Exception e) {
                return MappingFailures.decodeFailure(metrics, event, e);
            }
        });
    }

    public Maybe<DistributedEvent> to(final DictionaryDeployable deployable) {
        return Maybe.fromCallable(() -> {
            try {
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent.builder()
                    .id(deployable.id())
                    .type(DistributedEventType.DICTIONARY)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date());
                if (deployable.dictionary() != null) {
                    builder.payload(objectMapper.writeValueAsString(deployable.dictionary()));
                }
                return builder.build();
            } catch (Exception e) {
                return MappingFailures.deadLetter(metrics, DistributedEventType.DICTIONARY, deployable.id(), e);
            }
        });
    }
}
