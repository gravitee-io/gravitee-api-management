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
package io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service;

import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.rest.api.kafkaexplorer.domain.domain_service.KafkaClusterDomainService;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaClusterInfo;

public class KafkaClusterDomainServiceInMemory implements KafkaClusterDomainService {

    private KafkaClusterInfo result;
    private KafkaExplorerException exception;

    public void givenClusterInfo(KafkaClusterInfo info) {
        this.result = info;
        this.exception = null;
    }

    public void givenException(KafkaExplorerException exception) {
        this.exception = exception;
        this.result = null;
    }

    @Override
    public KafkaClusterInfo describeCluster(KafkaClusterConfiguration config) {
        if (exception != null) {
            throw exception;
        }
        return result;
    }
}
