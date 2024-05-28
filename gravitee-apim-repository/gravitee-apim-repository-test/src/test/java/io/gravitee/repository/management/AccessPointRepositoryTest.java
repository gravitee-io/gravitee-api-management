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
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.Nested;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
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
    public void should_return_accesspoint_from_reference_id_and_type() throws Exception {
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
        assertEquals(false, accessPoint.isSecured());
        assertEquals(false, accessPoint.isOverriding());
        assertEquals(AccessPointStatus.CREATED, accessPoint.getStatus());
        assertTrue(compareDate(new Date(1486771200000L), accessPoint.getUpdatedAt()));
    }

    @Test
    public void should_return_accesspoint_from_target() throws Exception {
        List<AccessPoint> accessPoints = accessPointRepository.findByTarget(AccessPointTarget.PORTAL);

        assertFalse(accessPoints.isEmpty());
        assertEquals(2, accessPoints.size());
        AccessPoint accessPoint = accessPoints.get(0);
        assertEquals("dev.en.company.apim-portal.gravitee.io:4100", accessPoint.getHost());
        assertEquals("69a7a51a-98b7-4943-a7a5-1a98b79943e6", accessPoint.getReferenceId());
        assertEquals(AccessPointTarget.PORTAL, accessPoint.getTarget());
        assertEquals(false, accessPoint.isSecured());
        assertEquals(false, accessPoint.isOverriding());
        assertEquals(AccessPointStatus.CREATED, accessPoint.getStatus());
        assertTrue(compareDate(new Date(1486771200000L), accessPoint.getUpdatedAt()));
    }

    @Test
    public void should_return_accesspoint_from_host() throws Exception {
        Optional<AccessPoint> accessPointOptional = accessPointRepository.findByHost("dev.en.company.apim-portal.gravitee.io:4100");

        assertTrue(accessPointOptional.isPresent());
        AccessPoint accessPoint = accessPointOptional.get();
        assertEquals("dev.en.company.apim-portal.gravitee.io:4100", accessPoint.getHost());
        assertEquals("69a7a51a-98b7-4943-a7a5-1a98b79943e6", accessPoint.getReferenceId());
        assertEquals(AccessPointTarget.PORTAL, accessPoint.getTarget());
        assertEquals(false, accessPoint.isSecured());
        assertEquals(false, accessPoint.isOverriding());
        assertEquals(AccessPointStatus.CREATED, accessPoint.getStatus());
        assertTrue(compareDate(new Date(1486771200000L), accessPoint.getUpdatedAt()));
    }

    @Test
    public void should_return_accesspoint_from_criteria() throws Exception {
        AccessPointCriteria accessPointCriteria = AccessPointCriteria
            .builder()
            .from(1486771200000L - 1)
            .to(1486771200000L + 1)
            .target(AccessPointTarget.GATEWAY)
            .status(AccessPointStatus.CREATED)
            .referenceType(AccessPointReferenceType.ENVIRONMENT)
            .referenceIds(List.of("b5655a66-cc38-46c5-8260-05a1c13850ec"))
            .build();

        List<AccessPoint> accessPoints = accessPointRepository.findByCriteria(accessPointCriteria, null, null);

        assertEquals(1, accessPoints.size());
        AccessPoint accessPoint = accessPoints.get(0);
        assertEquals("prod.en.company.apim-gateway.gravitee.io:8082", accessPoint.getHost());
        assertEquals("b5655a66-cc38-46c5-8260-05a1c13850ec", accessPoint.getReferenceId());
        assertEquals(AccessPointTarget.GATEWAY, accessPoint.getTarget());
        assertEquals(false, accessPoint.isSecured());
        assertEquals(false, accessPoint.isOverriding());
        assertEquals(AccessPointStatus.CREATED, accessPoint.getStatus());
        assertTrue(compareDate(new Date(1486771200000L), accessPoint.getUpdatedAt()));
    }

    @Test
    public void should_update_status_by_criteria() throws Exception {
        AccessPointCriteria accessPointCriteria = AccessPointCriteria
            .builder()
            .from(1486771200000L - 1)
            .to(1486771200000L + 1)
            .target(AccessPointTarget.GATEWAY)
            .status(AccessPointStatus.CREATED)
            .referenceType(AccessPointReferenceType.ENVIRONMENT)
            .referenceIds(List.of("0a660212-e982-498c-ad5c-32ec0f2737a4"))
            .build();

        List<AccessPoint> accessPoints = accessPointRepository.updateStatusByCriteria(accessPointCriteria, AccessPointStatus.DELETED);

        assertEquals(1, accessPoints.size());
        AccessPoint accessPoint = accessPoints.get(0);
        assertEquals("dev.en.company.apim-gateway.gravitee.io:8082", accessPoint.getHost());
        assertEquals("0a660212-e982-498c-ad5c-32ec0f2737a4", accessPoint.getReferenceId());
        assertEquals(AccessPointTarget.GATEWAY, accessPoint.getTarget());
        assertEquals(false, accessPoint.isSecured());
        assertEquals(false, accessPoint.isOverriding());
        assertTrue(accessPoint.getUpdatedAt().getTime() > 1486771200000L);
        assertEquals(AccessPointStatus.DELETED, accessPoints.get(0).getStatus());
    }

    @Test
    public void should_delete_from_reference() throws Exception {
        AccessPoint accessPoint = AccessPoint
            .builder()
            .id("id2")
            .host("host2")
            .target(AccessPointTarget.GATEWAY)
            .secured(true)
            .referenceId("referenceId2")
            .referenceType(AccessPointReferenceType.ENVIRONMENT)
            .overriding(true)
            .status(AccessPointStatus.CREATED)
            .updatedAt(new Date(1486771200000L))
            .build();

        accessPointRepository.create(accessPoint);

        Optional<AccessPoint> optionalCreated = accessPointRepository.findById("id2");
        assertTrue("AccessPoint not found", optionalCreated.isPresent());

        accessPointRepository.deleteByReference(AccessPointReferenceType.ENVIRONMENT, "referenceId2");

        Optional<AccessPoint> optionalDeleted = accessPointRepository.findById("id2");
        assertFalse("AccessPoint not deleted", optionalDeleted.isPresent());
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
