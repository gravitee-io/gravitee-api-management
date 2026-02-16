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
package io.gravitee.rest.api.kafkaexplorer.domain.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetKafkaExplorerInfoUseCaseTest {

    private GetKafkaExplorerInfoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetKafkaExplorerInfoUseCase();
    }

    @Test
    void should_return_module_info_with_version() {
        var result = useCase.execute(new GetKafkaExplorerInfoUseCase.Input());

        assertThat(result.info().version()).isEqualTo("1.0.0-SNAPSHOT");
        assertThat(result.info().status()).isEqualTo("initialized");
        assertThat(result.info().message()).isEqualTo("Kafka Explorer module initialized successfully");
    }
}
