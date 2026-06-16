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
package io.gravitee.apim.rest.api.automation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.rest.api.automation.model.NavigationPath;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiMapperTest {

    @Test
    void should_map_automation_navigation_path_to_core_with_all_fields() {
        var automation = new NavigationPath().path("/reference").displayName("Reference").order(1);

        var core = ApiMapper.INSTANCE.map(automation);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(core.path()).isEqualTo("/reference");
            soft.assertThat(core.displayName()).isEqualTo("Reference");
            soft.assertThat(core.order()).isEqualTo(1);
        });
    }

    @Test
    void should_map_automation_navigation_path_to_core_with_nulls_when_fields_missing() {
        var automation = new NavigationPath().path("/guides");

        var core = ApiMapper.INSTANCE.map(automation);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(core.path()).isEqualTo("/guides");
            soft.assertThat(core.displayName()).isNull();
            soft.assertThat(core.order()).isNull();
        });
    }

    @Test
    void should_return_null_when_mapping_null_automation_navigation_path() {
        assertThat(ApiMapper.INSTANCE.map((NavigationPath) null)).isNull();
    }

    @Test
    void should_map_core_navigation_path_to_automation_with_all_fields() {
        var core = new io.gravitee.apim.core.portal.model.NavigationPath("/reference", "Reference", 1);

        var automation = ApiMapper.INSTANCE.mapNavigationPath(core);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(automation.getPath()).isEqualTo("/reference");
            soft.assertThat(automation.getDisplayName()).isEqualTo("Reference");
            soft.assertThat(automation.getOrder()).isEqualTo(1);
        });
    }

    @Test
    void should_map_core_navigation_path_to_automation_leaving_absent_fields_null() {
        var core = new io.gravitee.apim.core.portal.model.NavigationPath("/guides", null, null);

        var automation = ApiMapper.INSTANCE.mapNavigationPath(core);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(automation.getPath()).isEqualTo("/guides");
            soft.assertThat(automation.getDisplayName()).isNull();
            soft.assertThat(automation.getOrder()).isNull();
        });
    }

    @Test
    void should_return_null_when_mapping_null_core_navigation_path() {
        assertThat(ApiMapper.INSTANCE.mapNavigationPath(null)).isNull();
    }
}
