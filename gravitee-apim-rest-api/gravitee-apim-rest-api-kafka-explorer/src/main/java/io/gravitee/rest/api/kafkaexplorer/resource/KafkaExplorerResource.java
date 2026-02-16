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
package io.gravitee.rest.api.kafkaexplorer.resource;

import io.gravitee.rest.api.kafkaexplorer.domain.use_case.GetKafkaExplorerInfoUseCase;
import io.gravitee.rest.api.kafkaexplorer.mapper.KafkaExplorerMapper;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerInfoResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/kafka-explorer")
public class KafkaExplorerResource {

    @Inject
    private GetKafkaExplorerInfoUseCase getKafkaExplorerInfoUseCase;

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public KafkaExplorerInfoResponse getInfo() {
        var result = getKafkaExplorerInfoUseCase.execute(new GetKafkaExplorerInfoUseCase.Input());
        return KafkaExplorerMapper.INSTANCE.map(result.info());
    }
}
