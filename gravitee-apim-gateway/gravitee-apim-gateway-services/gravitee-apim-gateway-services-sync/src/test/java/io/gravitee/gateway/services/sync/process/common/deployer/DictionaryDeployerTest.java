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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.mockito.Mockito.*;

import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DictionaryDeployerTest {

    @Mock
    private DictionaryManager dictionaryManager;

    private DictionaryDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new DictionaryDeployer(dictionaryManager, new NoopDistributedSyncService());
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_dictionary() {
            Dictionary dictionary = mock(Dictionary.class);
            DictionaryDeployable dictionaryDeployable = DictionaryDeployable.builder().id("dictionaryId").dictionary(dictionary).build();
            cut.deploy(dictionaryDeployable).test().assertComplete();
            verify(dictionaryManager).deploy(dictionary);
        }

        @Test
        void should_return_error_when_api_manager_throw_exception() {
            Dictionary dictionary = mock(Dictionary.class);
            DictionaryDeployable dictionaryDeployable = DictionaryDeployable.builder().id("dictionaryId").dictionary(dictionary).build();
            doThrow(new SyncException("error")).when(dictionaryManager).deploy(dictionary);
            cut.deploy(dictionaryDeployable).test().assertFailure(SyncException.class);
            verify(dictionaryManager).deploy(dictionary);
        }

        @Test
        void should_ignore_do_post_action() {
            cut.doAfterDeployment(null).test().assertComplete();
            verifyNoInteractions(dictionaryManager);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_dictionary() {
            DictionaryDeployable dictionaryDeployable = DictionaryDeployable.builder().id("dictionaryId").build();
            cut.undeploy(dictionaryDeployable).test().assertComplete();
            verify(dictionaryManager).undeploy(dictionaryDeployable.dictionary());
        }

        @Test
        void should_complete_on_error_when_dictionary_manager_throw_exception() {
            DictionaryDeployable dictionaryDeployable = DictionaryDeployable.builder().id("dictionaryId").build();
            doThrow(new SyncException("error")).when(dictionaryManager).undeploy(dictionaryDeployable.dictionary());
            cut.undeploy(dictionaryDeployable).test().assertFailure(SyncException.class);
            verify(dictionaryManager).undeploy(dictionaryDeployable.dictionary());
        }
    }
}
