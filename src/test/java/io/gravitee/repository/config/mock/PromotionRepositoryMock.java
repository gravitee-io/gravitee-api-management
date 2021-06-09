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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.repository.management.model.Promotion;
import io.gravitee.repository.management.model.PromotionStatus;

public class PromotionRepositoryMock extends AbstractRepositoryMock<PromotionRepository> {

    public PromotionRepositoryMock() {
        super(PromotionRepository.class);
    }

    @Override
    void prepare(PromotionRepository repository) throws Exception {
        Promotion promotionToCreate = getAPromotion("promotion#new");
        when(repository.findById(promotionToCreate.getId())).thenReturn(empty(), of(promotionToCreate));
        when(repository.create(any())).thenAnswer(returnsFirstArg());

        Promotion promotion = getAPromotion("promotion#1");
        when(repository.update(any())).thenAnswer(returnsFirstArg());
        when(repository.findById(promotion.getId())).thenReturn(of(promotion));

        Promotion promotionToDelete = getAPromotion("promotion#to-delete");
        when(repository.findById(promotionToDelete.getId())).thenReturn(of(promotionToDelete), empty());
    }

    private Promotion getAPromotion(String id) {
        final Promotion promotion = new Promotion();
        promotion.setId(id);
        promotion.setApiDefinition("{\"id\" : \"anAPIID\",\"name\" : \"Product\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}");
        promotion.setStatus(PromotionStatus.CREATED);
        promotion.setSourceEnvironmentId("env#1");
        promotion.setSourceInstallationId("inst#1");
        promotion.setTargetEnvironmentId("env#2");
        promotion.setTargetInstallationId("inst#2");

        return promotion;
    }

}
