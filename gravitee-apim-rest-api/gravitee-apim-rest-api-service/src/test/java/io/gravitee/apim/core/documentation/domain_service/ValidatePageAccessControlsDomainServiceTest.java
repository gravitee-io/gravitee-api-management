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
package io.gravitee.apim.core.documentation.domain_service;

import static org.assertj.core.api.Assertions.*;

import inmemory.GroupQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.group.model.Group;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePageAccessControlsDomainServiceTest {

    private static final String ORG_ID = "test-organization";
    private static final String ENV_ID = "test-environment";

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();

    private final ValidatePageAccessControlsDomainService cut = new ValidatePageAccessControlsDomainService(groupQueryService);

    @BeforeEach
    void setUp() {
        groupQueryService.reset();
    }

    @Test
    void should_return_empty_result_with_no_access_controls() {
        var result = cut.validateAndSanitize(new ValidatePageAccessControlsDomainService.Input(AUDIT_INFO, null));
        assertThat(result.errors()).isEmpty();
        assertThat(result.value()).isEmpty();
    }

    @Test
    void should_replace_access_control_group_name_with_group_id() {
        groupQueryService.initWith(List.of(Group.builder().id("group-id").name("group-name").environmentId(ENV_ID).build()));

        var given = Set.of(AccessControl.builder().referenceType("GROUP").referenceId("group-name").build());

        var result = cut.validateAndSanitize(new ValidatePageAccessControlsDomainService.Input(AUDIT_INFO, given));

        assertThat(result.errors()).isEmpty();
        assertThat(result.value())
            .isNotEmpty()
            .hasValue(
                new ValidatePageAccessControlsDomainService.Input(
                    AUDIT_INFO,
                    Set.of(AccessControl.builder().referenceId("group-id").referenceType("GROUP").build())
                )
            );
    }

    @Test
    void should_keep_access_control_with_group_id() {
        groupQueryService.initWith(List.of(Group.builder().id("group-id").name("group-name").environmentId(ENV_ID).build()));

        var given = Set.of(AccessControl.builder().referenceType("GROUP").referenceId("group-id").build());

        var result = cut.validateAndSanitize(new ValidatePageAccessControlsDomainService.Input(AUDIT_INFO, given));

        assertThat(result.errors()).isEmpty();
        assertThat(result.value())
            .isNotEmpty()
            .hasValue(
                new ValidatePageAccessControlsDomainService.Input(
                    AUDIT_INFO,
                    Set.of(AccessControl.builder().referenceId("group-id").referenceType("GROUP").build())
                )
            );
    }

    @Test
    void should_remove_access_control_with_unknown_reference() {
        var given = Set.of(AccessControl.builder().referenceType("GROUP").referenceId("group-id").build());

        var result = cut.validateAndSanitize(new ValidatePageAccessControlsDomainService.Input(AUDIT_INFO, given));

        assertThat(result.errors()).isEmpty();
        assertThat(result.value()).isNotEmpty().hasValue(new ValidatePageAccessControlsDomainService.Input(AUDIT_INFO, Set.of()));
    }
}
