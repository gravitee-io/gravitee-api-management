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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DictionaryMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private DictionaryMapper cut;
    private DistributedEvent distributedEvent;
    private DictionaryDeployable dictionaryDeployable;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new DictionaryMapper(objectMapper);

        Dictionary dictionary = new Dictionary();
        dictionary.setId("id");

        distributedEvent = DistributedEvent.builder()
            .id(dictionary.getId())
            .payload(objectMapper.writeValueAsString(dictionary))
            .updatedAt(new Date())
            .type(DistributedEventType.DICTIONARY)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        dictionaryDeployable = DictionaryDeployable.builder()
            .id(dictionary.getId())
            .dictionary(dictionary)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }

    @Test
    void should_return_distributed_event() {
        cut
            .to(distributedEvent)
            .test()
            .assertValue(dictionaryDeployable -> {
                assertThat(dictionaryDeployable).isEqualTo(this.dictionaryDeployable);
                return true;
            });
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut.to(DistributedEvent.builder().payload("wrong").build()).test().assertComplete();
    }

    @Test
    void should_map_dictionary_deployable() {
        cut
            .to(dictionaryDeployable)
            .test()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent.getId()).isEqualTo(this.distributedEvent.getId());
                assertThat(distributedEvent.getType()).isEqualTo(this.distributedEvent.getType());
                assertThat(distributedEvent.getPayload()).isEqualTo(this.distributedEvent.getPayload());
                assertThat(distributedEvent.getSyncAction()).isEqualTo(this.distributedEvent.getSyncAction());
                return true;
            });
    }
}
