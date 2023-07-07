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

import io.gravitee.rest.api.management.v2.rest.model.Rule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class })
public interface RuleMapper {
    RuleMapper INSTANCE = Mappers.getMapper(RuleMapper.class);

    @Mapping(target = "operation", source = "policy.name")
    @Mapping(target = "configuration", source = "policy.configuration", qualifiedByName = "deserializeConfiguration")
    Rule map(io.gravitee.definition.model.Rule rule);

    @Mapping(target = "policy.name", source = "operation")
    @Mapping(target = "policy.configuration", source = "configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.Rule map(Rule rule);

    List<Rule> mapToApiModel(List<io.gravitee.definition.model.Rule> rule);
    List<io.gravitee.definition.model.Rule> mapToEntity(List<Rule> rule);
}
