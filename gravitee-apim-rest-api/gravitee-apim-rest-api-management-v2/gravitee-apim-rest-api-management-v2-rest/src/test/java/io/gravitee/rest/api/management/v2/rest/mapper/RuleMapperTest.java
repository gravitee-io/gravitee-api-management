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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.RuleFixtures;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.management.v2.rest.model.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.model.Rule;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class RuleMapperTest {

    private final RuleMapper ruleMapper = Mappers.getMapper(RuleMapper.class);

    @Test
    void shouldMapToRuleEntity() throws JsonProcessingException {
        var rule = fixtures.RuleFixtures.oneRule();

        var ruleEntity = ruleMapper.map(rule);
        assertThat(ruleEntity).isNotNull();
        assertThat(ruleEntity.isEnabled()).isEqualTo(rule.getEnabled());
        assertThat(ruleEntity.getDescription()).isEqualTo(rule.getDescription());
        assertThat(ruleEntity.getPolicy()).isNotNull();
        assertThat(ruleEntity.getPolicy().getName()).isEqualTo(rule.getOperation());
        assertConfigurationAreEqual(rule, ruleEntity);
        assertMethodsAreEqual(rule.getMethods(), ruleEntity.getMethods());
    }

    @Test
    void shouldMapTuRule() throws JsonProcessingException {
        var ruleEntity = RuleFixtures.oneModelRule();

        var rule = ruleMapper.map(ruleEntity);
        assertThat(rule).isNotNull();
        assertThat(rule.getEnabled()).isEqualTo(ruleEntity.isEnabled());
        assertThat(rule.getDescription()).isEqualTo(ruleEntity.getDescription());
        assertThat(rule.getOperation()).isEqualTo(ruleEntity.getPolicy().getName());
        assertConfigurationAreEqual(rule, ruleEntity);
        assertMethodsAreEqual(rule.getMethods(), ruleEntity.getMethods());
    }

    private static void assertConfigurationAreEqual(Rule rule, io.gravitee.definition.model.Rule ruleEntity)
        throws JsonProcessingException {
        assertThat(ruleEntity.getPolicy().getConfiguration()).isEqualTo(new GraviteeMapper().writeValueAsString(rule.getConfiguration()));
    }

    private static void assertMethodsAreEqual(List<HttpMethod> ruleMethods, Set<io.gravitee.common.http.HttpMethod> ruleEntityMethods) {
        assertThat(ruleMethods).asList().hasSameSizeAs(ruleEntityMethods);
        assertThat(ruleMethods.stream().map(HttpMethod::name).collect(Collectors.toList())).isEqualTo(
            ruleEntityMethods.stream().map(io.gravitee.common.http.HttpMethod::name).collect(Collectors.toList())
        );
    }
}
