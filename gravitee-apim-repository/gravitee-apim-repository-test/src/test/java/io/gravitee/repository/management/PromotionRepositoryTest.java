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
package io.gravitee.repository.management;

import static org.junit.Assert.*;

import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.PromotionCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.*;
import java.util.Date;
import java.util.List;
import org.junit.Test;

public class PromotionRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/promotion-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final PromotionAuthor promotionAuthor = new PromotionAuthor();
        promotionAuthor.setUserId("user#1");
        promotionAuthor.setDisplayName("Gaetan Maisse");
        promotionAuthor.setEmail("gm@gv.io");
        promotionAuthor.setPicture("http://image.png");
        promotionAuthor.setSource("internal");
        promotionAuthor.setSourceId("internal#1");

        final Promotion promotion = new Promotion();
        promotion.setId("promotion#new");
        promotion.setApiDefinition(
            "{\"id\" : \"api#1\",\"name\" : \"Product\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}"
        );
        promotion.setStatus(PromotionStatus.CREATED);
        promotion.setSourceEnvCockpitId("env#cockpit-1");
        promotion.setSourceEnvName("Demo");
        promotion.setTargetEnvCockpitId("env#cockpit-2");
        promotion.setTargetEnvName("Prod");
        promotion.setCreatedAt(new Date());
        promotion.setAuthor(promotionAuthor);
        promotion.setApiId("api#1");

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

        final PromotionAuthor promotionAuthor = new PromotionAuthor();
        promotionAuthor.setUserId("user#1");
        promotionAuthor.setDisplayName("Gaetan Maisse");
        promotionAuthor.setEmail("gm@gv.io");
        promotionAuthor.setPicture("http://image.png");
        promotionAuthor.setSource("internal");
        promotionAuthor.setSourceId("internal#1");

        final Promotion expectedPromotion = new Promotion();
        expectedPromotion.setId("promotion#1");
        expectedPromotion.setApiId("api#1");
        expectedPromotion.setApiDefinition(
            "{\"id\" : \"api#1\",\"name\" : \"Product\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}"
        );
        expectedPromotion.setStatus(PromotionStatus.CREATED);
        expectedPromotion.setSourceEnvCockpitId("env#cockpit-1");
        expectedPromotion.setSourceEnvName("Demo");
        expectedPromotion.setTargetEnvCockpitId("env#cockpit-2");
        expectedPromotion.setTargetEnvName("Prod");
        expectedPromotion.setCreatedAt(new Date());
        expectedPromotion.setAuthor(promotionAuthor);

        assertEquals(expectedPromotion, storedPromotion);

        assertEquals(expectedPromotion.getAuthor(), storedPromotion.getAuthor());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Promotion storedPromotion = promotionRepository.findById("promotion#1").get();
        storedPromotion.setApiDefinition(
            "{\"id\" : \"anAPIID\",\"name\" : \"Product Updated\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}"
        );

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

    @Test
    public void shouldSearchWithoutCriteria() throws Exception {
        final List<Promotion> promotions = promotionRepository
            .search(null, null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 4, promotions.size());
        assertEquals("promotion#1", promotions.get(0).getId());
        assertEquals("promotion#to-delete", promotions.get(1).getId());
        assertEquals("promotion#to_be_validated_env_1", promotions.get(2).getId());
        assertEquals("promotion#to_be_validated_env_2", promotions.get(3).getId());
    }

    @Test
    public void shouldSearchWithEmptyCriteria() throws Exception {
        final PromotionCriteria criteria = new PromotionCriteria.Builder().build();
        final List<Promotion> promotions = promotionRepository
            .search(criteria, null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 4, promotions.size());
        assertEquals("promotion#1", promotions.get(0).getId());
        assertEquals("promotion#to-delete", promotions.get(1).getId());
        assertEquals("promotion#to_be_validated_env_1", promotions.get(2).getId());
        assertEquals("promotion#to_be_validated_env_2", promotions.get(3).getId());
    }

    @Test
    public void shouldSearchWithCriteriaTargetEnvIds() throws Exception {
        final PromotionCriteria criteria = new PromotionCriteria.Builder().targetEnvCockpitIds("env#cockpit-1").build();
        final List<Promotion> promotions = promotionRepository
            .search(criteria, null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 1, promotions.size());
        assertEquals("promotion#to_be_validated_env_1", promotions.get(0).getId());
    }

    @Test
    public void shouldSearchWithCriteriaStatuses() throws Exception {
        final PromotionCriteria criteria = new PromotionCriteria.Builder().statuses(List.of(PromotionStatus.TO_BE_VALIDATED)).build();
        final List<Promotion> promotions = promotionRepository
            .search(criteria, null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 2, promotions.size());
        assertEquals("promotion#to_be_validated_env_1", promotions.get(0).getId());
        assertEquals("promotion#to_be_validated_env_2", promotions.get(1).getId());
    }

    @Test
    public void shouldSearchWithCriteriaStatusesMultipleValues() throws Exception {
        final PromotionCriteria criteria = new PromotionCriteria.Builder()
            .statuses(List.of(PromotionStatus.TO_BE_VALIDATED, PromotionStatus.CREATED))
            .build();
        final List<Promotion> promotions = promotionRepository
            .search(criteria, null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 4, promotions.size());
        assertEquals("promotion#1", promotions.get(0).getId());
        assertEquals("promotion#to-delete", promotions.get(1).getId());
        assertEquals("promotion#to_be_validated_env_1", promotions.get(2).getId());
        assertEquals("promotion#to_be_validated_env_2", promotions.get(3).getId());
    }

    @Test
    public void shouldSearchWithCriteriaStatusSortByCreatedAtDesc() throws Exception {
        final PromotionCriteria criteria = new PromotionCriteria.Builder().statuses(List.of(PromotionStatus.TO_BE_VALIDATED)).build();
        final List<Promotion> promotions = promotionRepository
            .search(
                criteria,
                new SortableBuilder().field("created_at").order(Order.DESC).build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 2, promotions.size());
        assertEquals("promotion#to_be_validated_env_2", promotions.get(0).getId());
        assertEquals("promotion#to_be_validated_env_1", promotions.get(1).getId());
    }

    @Test
    public void shouldSearchWithCriteriaStatusPaginated() throws Exception {
        final PromotionCriteria criteria = new PromotionCriteria.Builder().statuses(List.of(PromotionStatus.TO_BE_VALIDATED)).build();
        final List<Promotion> promotions = promotionRepository
            .search(
                criteria,
                new SortableBuilder().field("created_at").order(Order.DESC).build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 1, promotions.size());
        assertEquals("promotion#to_be_validated_env_2", promotions.get(0).getId());
    }

    @Test
    public void shouldSearchWithCriteriaApiId() throws Exception {
        final PromotionCriteria criteria = new PromotionCriteria.Builder().apiId("api#1").build();
        final List<Promotion> promotions = promotionRepository
            .search(criteria, null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 3, promotions.size());
        assertEquals("promotion#1", promotions.get(0).getId());
        assertEquals("promotion#to_be_validated_env_1", promotions.get(1).getId());
        assertEquals("promotion#to_be_validated_env_2", promotions.get(2).getId());
    }

    @Test
    public void shouldSearchWithCriteriaTargetApiId() throws Exception {
        final PromotionCriteria criteria = new PromotionCriteria.Builder().targetApiExists(true).build();
        final List<Promotion> promotions = promotionRepository
            .search(criteria, null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();
        assertNotNull(promotions);
        assertEquals("Invalid promotions numbers in search", 2, promotions.size());
        assertEquals("promotion#to_be_validated_env_1", promotions.get(0).getId());
        assertEquals("promotion#to_be_validated_env_2", promotions.get(1).getId());
    }
}
