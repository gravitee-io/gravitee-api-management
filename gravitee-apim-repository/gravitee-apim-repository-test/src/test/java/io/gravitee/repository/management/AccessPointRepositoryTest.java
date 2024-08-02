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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.management.model.AccessPointTarget;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class AccessPointRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/accesspoint-tests/";
    }

    @Test
    public void should_create_and_find_access_point() throws Exception {
        AccessPoint accessPoint = AccessPoint
            .builder()
            .id("id")
            .host("host")
            .target(AccessPointTarget.GATEWAY)
            .secured(true)
            .referenceId("referenceId")
            .referenceType(AccessPointReferenceType.ENVIRONMENT)
            .overriding(true)
            .status(AccessPointStatus.CREATED)
            .updatedAt(new Date(1486771200000L))
            .build();

        accessPointRepository.create(accessPoint);

        Optional<AccessPoint> optional = accessPointRepository.findById("id");
        assertTrue("AccessPoint not found", optional.isPresent());

        AccessPoint accessPointFound = optional.get();

        assertEquals(accessPoint, accessPointFound);
    }

    @Test
    public void should_return_empty_optional_if_id_not_found() throws Exception {
        Optional<AccessPoint> optional = accessPointRepository.findById("not found");

        assertFalse("AccessPoint shouldn't be found", optional.isPresent());
    }

    @Test
    public void should_return_access_point_from_reference_id_and_type() throws Exception {
        List<AccessPoint> accessPoints = accessPointRepository.findByReferenceAndTarget(
            AccessPointReferenceType.ENVIRONMENT,
            "69a7a51a-98b7-4943-a7a5-1a98b79943e6",
            AccessPointTarget.PORTAL
        );

        assertFalse(accessPoints.isEmpty());
        assertEquals(1, accessPoints.size());
        AccessPoint accessPoint = accessPoints.get(0);
        assertEquals("dev.en.company.apim-portal.gravitee.io:4100", accessPoint.getHost());
        assertEquals("69a7a51a-98b7-4943-a7a5-1a98b79943e6", accessPoint.getReferenceId());
        assertEquals(AccessPointTarget.PORTAL, accessPoint.getTarget());
        assertFalse(accessPoint.isSecured());
        assertFalse(accessPoint.isOverriding());
        assertEquals(AccessPointStatus.CREATED, accessPoint.getStatus());
        assertTrue(compareDate(new Date(1486771200000L), accessPoint.getUpdatedAt()));
    }

    @Test
    public void should_return_access_point_from_target() throws Exception {
        List<AccessPoint> accessPoints = accessPointRepository.findByTarget(AccessPointTarget.PORTAL);

        assertFalse(accessPoints.isEmpty());
        assertEquals(2, accessPoints.size());
        AccessPoint accessPoint = accessPoints.get(0);
        assertEquals("dev.en.company.apim-portal.gravitee.io:4100", accessPoint.getHost());
        assertEquals("69a7a51a-98b7-4943-a7a5-1a98b79943e6", accessPoint.getReferenceId());
        assertEquals(AccessPointTarget.PORTAL, accessPoint.getTarget());
        assertFalse(accessPoint.isSecured());
        assertFalse(accessPoint.isOverriding());
        assertEquals(AccessPointStatus.CREATED, accessPoint.getStatus());
        assertTrue(compareDate(new Date(1486771200000L), accessPoint.getUpdatedAt()));
    }

    @Test
    public void should_return_access_point_from_host() throws Exception {
        Optional<AccessPoint> accessPointOptional = accessPointRepository.findByHost("dev.en.company.apim-portal.gravitee.io:4100");

        assertTrue(accessPointOptional.isPresent());
        AccessPoint accessPoint = accessPointOptional.get();
        assertEquals("dev.en.company.apim-portal.gravitee.io:4100", accessPoint.getHost());
        assertEquals("69a7a51a-98b7-4943-a7a5-1a98b79943e6", accessPoint.getReferenceId());
        assertEquals(AccessPointTarget.PORTAL, accessPoint.getTarget());
        assertFalse(accessPoint.isSecured());
        assertFalse(accessPoint.isOverriding());
        assertEquals(AccessPointStatus.CREATED, accessPoint.getStatus());
        assertTrue(compareDate(new Date(1486771200000L), accessPoint.getUpdatedAt()));
    }

    @Test
    public void should_return_access_point_from_criteria() throws Exception {
        AccessPointCriteria accessPointCriteria = AccessPointCriteria
            .builder()
            .from(1486771200000L - 1)
            .to(1486771200000L + 1)
            .target(AccessPointTarget.GATEWAY)
            .status(AccessPointStatus.CREATED)
            .referenceType(AccessPointReferenceType.ENVIRONMENT)
            .referenceIds(Set.of("b78f2219-890d-4344-8f22-19890d834442"))
            .build();

        List<AccessPoint> accessPoints = accessPointRepository.findByCriteria(accessPointCriteria, null, null);

        assertEquals(1, accessPoints.size());
        AccessPoint accessPoint = accessPoints.get(0);
        assertEquals("prod.en.company.apim-gateway.gravitee.io:8082", accessPoint.getHost());
        assertEquals("b78f2219-890d-4344-8f22-19890d834442", accessPoint.getReferenceId());
        assertEquals(AccessPointTarget.GATEWAY, accessPoint.getTarget());
        assertFalse(accessPoint.isSecured());
        assertFalse(accessPoint.isOverriding());
        assertEquals(AccessPointStatus.CREATED, accessPoint.getStatus());
        assertTrue(compareDate(new Date(1486771200000L), accessPoint.getUpdatedAt()));
    }

    @Test
    public void should_return_access_point_from_criteria_paginated() throws Exception {
        var accessPointCriteria = AccessPointCriteria
            .builder()
            .from(1486771200000L - 1)
            .to(1486771200000L + 1)
            .target(AccessPointTarget.GATEWAY)
            .status(AccessPointStatus.CREATED)
            .build();

        var accessPoints = accessPointRepository.findByCriteria(accessPointCriteria, 0L, 2L);

        assertEquals(2, accessPoints.size());
    }

    @Test
    public void should_delete_from_reference() throws Exception {
        List<AccessPoint> beforeDeletion = accessPointRepository
            .findAll()
            .stream()
            .filter(accessPoint -> accessPoint.getReferenceId().equals("env_id_to_be_deleted"))
            .toList();
        List<String> deleted = accessPointRepository.deleteByReferenceIdAndReferenceType(
            "env_id_to_be_deleted",
            AccessPointReferenceType.ENVIRONMENT
        );
        List<AccessPoint> afterDeletion = accessPointRepository
            .findAll()
            .stream()
            .filter(accessPoint -> accessPoint.getReferenceId().equals("env_id_to_be_deleted"))
            .toList();

        assertEquals(beforeDeletion.size(), deleted.size());
        assertTrue(beforeDeletion.stream().map(AccessPoint::getId).toList().containsAll(deleted));
        assertEquals(0, afterDeletion.size());
    }

    @Test
    public void should_delete_from_id() throws Exception {
        AccessPoint accessPoint = AccessPoint
            .builder()
            .id("id3")
            .host("host3")
            .target(AccessPointTarget.GATEWAY)
            .secured(true)
            .referenceId("referenceId3")
            .referenceType(AccessPointReferenceType.ENVIRONMENT)
            .overriding(true)
            .status(AccessPointStatus.CREATED)
            .updatedAt(new Date(1486771200000L))
            .build();

        accessPointRepository.create(accessPoint);

        Optional<AccessPoint> optionalCreated = accessPointRepository.findById("id3");
        assertTrue("AccessPoint not found", optionalCreated.isPresent());

        accessPointRepository.delete("id3");

        Optional<AccessPoint> optionalDeleted = accessPointRepository.findById("id3");
        assertFalse("AccessPoint not deleted", optionalDeleted.isPresent());
    }
}
