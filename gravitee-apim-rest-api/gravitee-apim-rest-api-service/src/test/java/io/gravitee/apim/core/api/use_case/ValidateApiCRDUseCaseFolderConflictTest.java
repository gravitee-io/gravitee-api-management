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
package io.gravitee.apim.core.api.use_case;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;

import fixtures.core.model.ApiCRDFixtures;
import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateHealthCheckScheduleDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBindingPermissionDomainService;
import io.gravitee.apim.core.cluster.domain_service.ValidateApiClusterBindingService;
import io.gravitee.apim.core.cluster.domain_service.ValidateKafkaClusterBindingDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.notification.domain_service.ValidatePortalNotificationDomainService;
import io.gravitee.apim.core.plan.domain_service.ValidatePlanDomainService;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal_listing.domain_service.PortalListingSyncDomainService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateApiCRDUseCaseFolderConflictTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().environmentId("TEST").organizationId("TEST").build();

    // Constructor injection resolves the validator's dependencies by type, so the test does not hard-code its argument list
    @Mock
    ValidateCategoryIdsDomainService categoryIdsValidator;

    @Mock
    VerifyApiPathDomainService apiPathValidator;

    @Mock
    VerifyApiHostsDomainService apiHostValidator;

    @Mock
    ValidateCRDMembersDomainService membersValidator;

    @Mock
    ValidateGroupsDomainService groupsValidator;

    @Mock
    ValidateResourceDomainService resourceValidator;

    @Mock
    ValidatePagesDomainService pagesValidator;

    @Mock
    ValidatePlanDomainService planValidator;

    @Mock
    ValidatePortalNotificationDomainService portalNotificationValidator;

    @Mock
    ValidateHealthCheckScheduleDomainService healthCheckScheduleValidator;

    @Mock
    ApiQueryService apiQueryService;

    @Mock
    PropertyDomainService propertyDomainService;

    @Mock
    CRDMembersDomainService membersDomainService;

    @Mock
    ValidateKafkaClusterBindingDomainService kafkaClusterBindingValidator;

    @Mock
    ClusterBindingPermissionDomainService clusterBindingPermissionDomainService;

    @Mock
    ValidateApiClusterBindingService apiClusterBindingValidator;

    @Mock
    PortalListingSyncDomainService portalListingSyncDomainService;

    @InjectMocks
    ValidateApiCRDDomainService validator;

    @Test
    void should_refuse_a_hand_made_folder_at_a_declared_path_naming_the_path() {
        var spec = ApiCRDFixtures.anApiCRD();
        spec.setPortalNavigation(List.of(new NavigationPath("/tba-guides", null)));
        doThrow(PathConflictException.folderPath("/tba-guides"))
            .when(portalListingSyncDomainService)
            .validateApiFolderConflictsForApi(AUDIT_INFO, spec.getId(), spec.getPortalNavigation());
        var cut = new ValidateApiCRDUseCase(validator);

        assertThatExceptionOfType(PathConflictException.class)
            .as("the dry run must refuse the folder conflict, as the import does")
            .isThrownBy(() -> cut.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, spec)))
            .withMessage("Navigation path [/tba-guides] is already occupied by content not managed by the Automation API");
    }
}
