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

import static org.junit.Assert.*;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.*;
import java.util.Date;
import org.junit.Test;

public class PromotionRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/promotion-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final Promotion promotion = new Promotion();
        promotion.setId("promotion#new");
        promotion.setApiDefinition("{\"id\" : \"anAPIID\",\"name\" : \"Product\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}");
        promotion.setStatus(PromotionStatus.CREATED);
        promotion.setSourceEnvironmentId("env#1");
        promotion.setSourceInstallationId("inst#1");
        promotion.setTargetEnvironmentId("env#2");
        promotion.setTargetInstallationId("inst#2");
        promotion.setCreatedAt(new Date());

        boolean presentBefore = promotionRepository.findById("promotion#new").isPresent();
        assertFalse("must not exists before creation", presentBefore);

        Promotion createdPromotion = promotionRepository.create(promotion);
        assertEquals(promotion, createdPromotion);

        Promotion storedPromotion = promotionRepository.findById("promotion#new").get();
        assertEquals(promotion, storedPromotion);
    }

    @Test
    public void shouldFindById() throws Exception {
        Promotion storedPromotion = promotionRepository.findById("promotion#1").get();

        final Promotion expectedPromotion = new Promotion();
        expectedPromotion.setId("promotion#1");
        expectedPromotion.setApiDefinition("{\"id\" : \"anAPIID\",\"name\" : \"Product\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}");
        expectedPromotion.setStatus(PromotionStatus.CREATED);
        expectedPromotion.setSourceEnvironmentId("env#1");
        expectedPromotion.setSourceInstallationId("inst#1");
        expectedPromotion.setTargetEnvironmentId("env#2");
        expectedPromotion.setTargetInstallationId("inst#2");

        assertEquals(expectedPromotion, storedPromotion);
    }

    @Test
    public void shouldUpdate() throws Exception {
        Promotion storedPromotion = promotionRepository.findById("promotion#1").get();
        storedPromotion.setApiDefinition("{\"id\" : \"anAPIID\",\"name\" : \"Product Updated\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}");

        Promotion updatedPromotion = promotionRepository.update(storedPromotion);
        assertEquals(storedPromotion, updatedPromotion);

        Promotion dbPromotion = promotionRepository.findById("promotion#1").get();
        assertEquals(storedPromotion, dbPromotion);
    }


    @Test
    public void shouldDelete() throws Exception {
        String idOfPromotionToDelete = "promotion#to-delete";

        boolean presentBefore = promotionRepository.findById(idOfPromotionToDelete).isPresent();
        assertTrue("must exists before deletion", presentBefore);

        promotionRepository.delete(idOfPromotionToDelete);

        boolean presentAfter = promotionRepository.findById(idOfPromotionToDelete).isPresent();
        assertFalse("must not exists anymore after deletion", presentAfter);
    }
}
