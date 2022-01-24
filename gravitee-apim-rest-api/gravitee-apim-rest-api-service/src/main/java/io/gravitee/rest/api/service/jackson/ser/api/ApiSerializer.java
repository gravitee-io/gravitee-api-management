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
package io.gravitee.rest.api.service.jackson.ser.api;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ApiSerializer extends StdSerializer<ApiEntity> {

    public static String METADATA_EXPORT_VERSION = "exportVersion";
    public static String METADATA_FILTERED_FIELDS_LIST = "filteredFieldsList";
    protected ApplicationContext applicationContext;

    protected ApiSerializer(Class<ApiEntity> t) {
        super(t);
    }

    public abstract Version version();

    public boolean canHandle(ApiEntity apiEntity) {
        return version().getVersion().equals(apiEntity.getMetadata().get(METADATA_EXPORT_VERSION));
    }

    @Override
    public void serialize(ApiEntity apiEntity, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        if (apiEntity.getName() != null) {
            jsonGenerator.writeObjectField("name", apiEntity.getName());
        }

        if (apiEntity.getEnvironmentId() != null) {
            jsonGenerator.writeObjectField("environment_id", apiEntity.getEnvironmentId());
        }

        if (apiEntity.getVersion() != null) {
            jsonGenerator.writeObjectField("version", apiEntity.getVersion());
        }
        if (apiEntity.getDescription() != null) {
            jsonGenerator.writeObjectField("description", apiEntity.getDescription());
        }
        if (apiEntity.getVisibility() != null) {
            jsonGenerator.writeObjectField("visibility", apiEntity.getVisibility());
        }
        if (apiEntity.getTags() != null && !apiEntity.getTags().isEmpty()) {
            jsonGenerator.writeArrayFieldStart("tags");
            for (String tag : apiEntity.getTags()) {
                jsonGenerator.writeObject(tag);
            }
            jsonGenerator.writeEndArray();
        }
        if (apiEntity.getPicture() != null) {
            jsonGenerator.writeObjectField("picture", apiEntity.getPicture());
        }
        if (apiEntity.getPaths() != null) {
            jsonGenerator.writeObjectFieldStart("paths");
            for (Map.Entry<String, List<Rule>> entry : apiEntity.getPaths().entrySet()) {
                jsonGenerator.writeObjectField(entry.getKey(), entry.getValue());
            }
            jsonGenerator.writeEndObject();
        }

        if (apiEntity.getGraviteeDefinitionVersion() != null) {
            jsonGenerator.writeObjectField("gravitee", apiEntity.getGraviteeDefinitionVersion());
        }

        if (apiEntity.getFlowMode() != null) {
            jsonGenerator.writeObjectField("flow_mode", apiEntity.getFlowMode().toString().toUpperCase());
        }

        if (apiEntity.getFlows() != null) {
            jsonGenerator.writeObjectField("flows", apiEntity.getFlows());
        }

        if (apiEntity.getServices() != null && !apiEntity.getServices().isEmpty()) {
            jsonGenerator.writeObjectField("services", apiEntity.getServices());
        }

        if (apiEntity.getResources() != null) {
            jsonGenerator.writeArrayFieldStart("resources");
            for (Resource resource : apiEntity.getResources()) {
                jsonGenerator.writeObject(resource);
            }
            jsonGenerator.writeEndArray();
        }

        if (apiEntity.getProperties() != null && apiEntity.getProperties().getValues() != null) {
            jsonGenerator.writeObjectField("properties", apiEntity.getProperties().getProperties());
        } else {
            jsonGenerator.writeObjectField("properties", emptyList());
        }

        if (apiEntity.getCategories() != null && !apiEntity.getCategories().isEmpty()) {
            jsonGenerator.writeArrayFieldStart("categories");
            for (String category : apiEntity.getCategories()) {
                jsonGenerator.writeObject(category);
            }
            jsonGenerator.writeEndArray();
        }

        if (apiEntity.getLabels() != null && !apiEntity.getLabels().isEmpty()) {
            jsonGenerator.writeArrayFieldStart("labels");
            for (String label : apiEntity.getLabels()) {
                jsonGenerator.writeObject(label);
            }
            jsonGenerator.writeEndArray();
        }

        // handle filtered fields list
        List<String> filteredFieldsList = (List<String>) apiEntity.getMetadata().get(METADATA_FILTERED_FIELDS_LIST);

        if (filteredFieldsList != null) {
            if (!filteredFieldsList.contains("groups")) {
                if (apiEntity.getGroups() != null && !apiEntity.getGroups().isEmpty()) {
                    Set<GroupEntity> groupEntities = applicationContext.getBean(GroupService.class).findByIds(apiEntity.getGroups());
                    jsonGenerator.writeObjectField("groups", groupEntities.stream().map(GroupEntity::getName).collect(Collectors.toSet()));
                }
            }

            // members
            if (!filteredFieldsList.contains("members")) {
                Set<MemberEntity> memberEntities = applicationContext
                    .getBean(MembershipService.class)
                    .getMembersByReference(MembershipReferenceType.API, apiEntity.getId());
                List<Member> members = new ArrayList<>(memberEntities == null ? 0 : memberEntities.size());
                if (memberEntities != null) {
                    final UserService userService = applicationContext.getBean(UserService.class);
                    memberEntities
                        .stream()
                        .filter(m -> m.getType() == MembershipMemberType.USER)
                        .forEach(
                            m -> {
                                UserEntity userEntity = userService.findById(m.getId());
                                if (userEntity != null) {
                                    Member member = new Member();
                                    member.setRoles(m.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toList()));
                                    member.setSource(userEntity.getSource());
                                    member.setSourceId(userEntity.getSourceId());
                                    members.add(member);
                                }
                            }
                        );
                }
                jsonGenerator.writeObjectField("members", members);
            }

            // pages
            if (!filteredFieldsList.contains("pages")) {
                List<PageEntity> pages = applicationContext
                    .getBean(PageService.class)
                    .search(new PageQuery.Builder().api(apiEntity.getId()).build(), true, GraviteeContext.getCurrentEnvironment());

                if (this.version().getVersion().startsWith("1.")) {
                    pages =
                        pages
                            .stream()
                            .filter(
                                pageEntity ->
                                    !pageEntity.getType().equals(PageType.LINK.name()) &&
                                    !pageEntity.getType().equals(PageType.TRANSLATION.name()) &&
                                    !pageEntity.getType().equals(PageType.SYSTEM_FOLDER.name()) &&
                                    !pageEntity.getType().equals(PageType.MARKDOWN_TEMPLATE.name()) &&
                                    !pageEntity.getType().equals(PageType.ASCIIDOC.name())
                            )
                            .collect(Collectors.toList());
                } else if (this.version().getVersion().equals("3.0")) {
                    pages =
                        pages
                            .stream()
                            .filter(
                                pageEntity ->
                                    !pageEntity.getType().equals(PageType.MARKDOWN_TEMPLATE.name()) &&
                                    !pageEntity.getType().equals(PageType.ASCIIDOC.name())
                            )
                            .collect(Collectors.toList());
                } else if (this.version().getVersion().equals("3.7")) {
                    pages =
                        pages
                            .stream()
                            .filter(pageEntity -> !pageEntity.getType().equals(PageType.ASCIIDOC.name()))
                            .collect(Collectors.toList());
                }
                jsonGenerator.writeObjectField("pages", pages == null ? Collections.emptyList() : pages);
                List<MediaEntity> apiMedia = applicationContext.getBean(MediaService.class).findAllByApiId(apiEntity.getId());
                if (apiMedia != null && !apiMedia.isEmpty()) {
                    jsonGenerator.writeObjectField("apiMedia", apiMedia);
                }
            }

            // plans
            if (!filteredFieldsList.contains("plans")) {
                Set<PlanEntity> plans = applicationContext.getBean(PlanService.class).findByApi(apiEntity.getId());
                Set<PlanEntity> plansToAdd = plans == null
                    ? Collections.emptySet()
                    : plans.stream().filter(p -> !PlanStatus.CLOSED.equals(p.getStatus())).collect(Collectors.toSet());
                jsonGenerator.writeObjectField("plans", plansToAdd);
            }

            // metadata
            if (!filteredFieldsList.contains("metadata")) {
                List<ApiMetadataEntity> apiMetadata = applicationContext.getBean(ApiMetadataService.class).findAllByApi(apiEntity.getId());
                if (apiMetadata != null && !apiMetadata.isEmpty()) {
                    jsonGenerator.writeObjectField("metadata", apiMetadata);
                }
            }
        }
    }

    public enum Version {
        DEFAULT("default"), // AsciiDoc in 3.10
        V_1_15("1.15"),
        V_1_20("1.20"),
        V_1_25("1.25"),
        V_3_0("3.0"), // System folders & Links & translations
        V_3_7("3.7"); // Markdown template

        private final String version;

        Version(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    class Member {

        private String username;
        private String source;
        private String sourceId;
        private String role;
        private List<String> roles;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    private static Pattern uid = Pattern.compile("uid=(.*?),");

    public static String getUsernameFromSourceId(String sourceId) {
        if (sourceId == null) {
            return null;
        }

        Matcher matcher = uid.matcher(sourceId);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return sourceId;
    }
}
