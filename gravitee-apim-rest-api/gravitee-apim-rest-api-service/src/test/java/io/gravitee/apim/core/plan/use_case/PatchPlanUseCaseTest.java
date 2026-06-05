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
package io.gravitee.apim.core.plan.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fakes.FakePolicyValidationDomainService;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.KafkaPortRangeCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.json.JsonDiffProcessor;
import io.gravitee.apim.core.json_patch.domain_service.JsonPatchDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.VerifyPlanPortRangesDomainService;
import io.gravitee.apim.core.plan.exception.PlanForApiNotFoundException;
import io.gravitee.apim.core.plan.exception.PlanPatchNotAllowedException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.use_case.PatchPlanUseCase.PlanFlowsConverter;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.domain_service.json_patch.JsonMergePatchServiceImpl;
import io.gravitee.apim.infra.domain_service.json_patch.JsonPatchServiceImpl;
import io.gravitee.apim.infra.domain_service.plan.PlanSynchronizationLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PatchPlanUseCaseTest {

    private static final String API_ID = "my-api";
    private static final String PLAN_ID = "my-plan";

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonPatchDomainService jsonPatchDomainService = new JsonPatchDomainService(
        new JsonMergePatchServiceImpl(),
        new JsonPatchServiceImpl()
    );
    private final FakePolicyValidationDomainService policyValidationDomainService = new FakePolicyValidationDomainService();
    private final EntrypointPluginQueryServiceInMemory entrypointConnectorPluginService = new EntrypointPluginQueryServiceInMemory();
    private final FlowValidationDomainService flowValidationDomainService = new FlowValidationDomainService(
        policyValidationDomainService,
        entrypointConnectorPluginService
    );
    private final ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final PlanValidatorDomainService planValidatorDomainService = new PlanValidatorDomainService(
        parametersQueryService,
        policyValidationDomainService,
        pageCrudService
    );
    private final SynchronizationService synchronizationService = new SynchronizationService(objectMapper);
    private final PlanSynchronizationService planSynchronizationService = new PlanSynchronizationLegacyWrapper(synchronizationService);
    private final JsonDiffProcessor jsonDiffProcessor = new JacksonJsonDiffProcessor();
    private final AuditDomainService auditDomainService = new AuditDomainService(auditCrudService, userCrudService, jsonDiffProcessor);
    private final ReorderPlanDomainService reorderPlanDomainService = new ReorderPlanDomainService(planQueryService, planCrudService);
    private final KafkaPortRangeCrudServiceInMemory kafkaPortRanges = new KafkaPortRangeCrudServiceInMemory();
    private final UpdatePlanDomainService updatePlanDomainService = new UpdatePlanDomainService(
        planQueryService,
        planCrudService,
        planValidatorDomainService,
        flowValidationDomainService,
        flowCrudService,
        auditDomainService,
        planSynchronizationService,
        reorderPlanDomainService,
        new VerifyPlanPortRangesDomainService(kafkaPortRanges),
        kafkaPortRanges
    );

    private PatchPlanUseCase useCase;

    @BeforeEach
    void setUp() {
        apiCrudService.reset();
        planCrudService.reset();
        planQueryService.reset();
        flowCrudService.reset();
        auditCrudService.reset();
        userCrudService.reset();
        var plans = List.of(PlanFixtures.aPlanHttpV4());
        useCase = new PatchPlanUseCase(
            apiCrudService,
            planCrudService,
            flowCrudService,
            updatePlanDomainService,
            jsonPatchDomainService,
            objectMapper,
            new PlanFlowsConverter() {
                @Override
                public JsonNode toCurrentFlowsNode(List<Flow> flows) {
                    return objectMapper.valueToTree(flows);
                }

                @Override
                public List<Flow> fromPatchedFlowsNode(JsonNode flowsNode) {
                    try {
                        return objectMapper.readerForListOf(Flow.class).readValue(flowsNode);
                    } catch (IOException e) {
                        throw new ValidationDomainException("Invalid value for field 'flows': " + e.getMessage(), e);
                    }
                }
            }
        );
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        planCrudService.initWith(plans);
        planQueryService.initWith(plans);
        parametersQueryService.initWith(
            List.of(
                Parameter.builder().key(Key.PLAN_SECURITY_KEYLESS_ENABLED.key()).value("true").build(),
                Parameter.builder().key(Key.PLAN_SECURITY_APIKEY_ENABLED.key()).value("true").build()
            )
        );
    }

    private PatchPlanUseCase.Input.InputBuilder baseInput() {
        return PatchPlanUseCase.Input.builder()
            .apiId(API_ID)
            .planId(PLAN_ID)
            .dryRun(false)
            .auditInfo(AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id"));
    }

    private PatchPlanUseCase.Output executeMerge(String body) {
        return useCase.execute(baseInput().patchType(PatchPlanUseCase.PatchType.MERGE_PATCH).patchBody(body).build());
    }

    private PatchPlanUseCase.Output executeJsonPatch(String body) {
        return useCase.execute(baseInput().patchType(PatchPlanUseCase.PatchType.JSON_PATCH).patchBody(body).build());
    }

    private void seed(Plan... plans) {
        planCrudService.reset();
        planQueryService.reset();
        var planList = List.of(plans);
        planCrudService.initWith(planList);
        planQueryService.initWith(planList);
    }

    @Nested
    class ApplyJsonPatchToAllowListedField {

        @Test
        void replace_name_returns_updated_plan_and_persists_one_update() {
            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed\"}]");

            assertThat(output.plan().getName()).isEqualTo("Renamed");
            assertThat(planCrudService.storage()).hasSize(1);
            assertThat(planCrudService.storage().get(0).getName()).isEqualTo("Renamed");
        }

        @Test
        void replace_description_updates_only_description() {
            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"new description\"}]");

            assertThat(output.plan().getDescription()).isEqualTo("new description");
            assertThat(output.plan().getName()).isEqualTo(PlanFixtures.aPlanHttpV4().getName());
        }

        @Test
        void replace_validation_updates_enum() {
            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/validation\",\"value\":\"MANUAL\"}]");

            assertThat(output.plan().getValidation()).isEqualTo(Plan.PlanValidationType.MANUAL);
        }

        @Test
        void replace_selection_rule_updates_definition_field() {
            var output = executeJsonPatch(
                "[{\"op\":\"replace\",\"path\":\"/selectionRule\",\"value\":\"#context.attributes['foo'] == 'bar'\"}]"
            );

            assertThat(output.plan().getPlanDefinitionHttpV4().getSelectionRule()).isEqualTo("#context.attributes['foo'] == 'bar'");
        }

        @Test
        void replace_comment_required_updates_boolean() {
            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/commentRequired\",\"value\":true}]");

            assertThat(output.plan().isCommentRequired()).isTrue();
        }

        @Test
        void replace_general_conditions_updates_text() {
            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/generalConditions\",\"value\":\"terms-and-conditions\"}]");

            assertThat(output.plan().getGeneralConditions()).isEqualTo("terms-and-conditions");
        }

        @Test
        void replace_excluded_groups_updates_list() {
            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/excludedGroups\",\"value\":[\"g1\",\"g2\"]}]");

            assertThat(output.plan().getExcludedGroups()).containsExactly("g1", "g2");
        }

        @Test
        void replace_characteristics_updates_list() {
            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/characteristics\",\"value\":[\"public\",\"v2\"]}]");

            assertThat(output.plan().getCharacteristics()).containsExactly("public", "v2");
        }

        @Test
        void remove_description_clears_the_field() {
            var output = executeJsonPatch("[{\"op\":\"remove\",\"path\":\"/description\"}]");

            assertThat(output.plan().getDescription()).isNull();
        }
    }

    @Nested
    class ApplyMergePatchToAllowListedField {

        @Test
        void merge_description_updates_only_description() {
            var originalName = PlanFixtures.aPlanHttpV4().getName();

            var output = executeMerge("{\"description\":\"new description\"}");

            assertThat(output.plan().getDescription()).isEqualTo("new description");
            assertThat(output.plan().getName()).isEqualTo(originalName);
        }

        @Test
        void merge_with_explicit_null_tags_clears_the_tag_set() {
            var seeded = PlanFixtures.aPlanHttpV4();
            seeded.getPlanDefinitionHttpV4().setTags(Set.of("tag1"));
            seed(seeded);

            var output = executeMerge("{\"tags\":null}");

            assertThat(output.plan().getPlanDefinitionHttpV4().getTags()).isNullOrEmpty();
        }

        @Test
        void merge_with_absent_key_preserves_existing_value() {
            var originalName = PlanFixtures.aPlanHttpV4().getName();

            var output = executeMerge("{\"description\":\"new description\"}");

            assertThat(output.plan().getName()).isEqualTo(originalName);
        }

        @Test
        void merge_selection_rule_on_plan_without_http_v4_definition_is_a_no_op() {
            seed(PlanFixtures.aPlanNativeV4());

            var output = executeMerge("{\"selectionRule\":\"#context.attributes['foo'] == 'bar'\"}");

            assertThat(output.plan().getPlanDefinitionHttpV4()).isNull();
        }

        @Test
        void merge_security_on_plan_without_http_v4_definition_is_a_no_op() {
            seed(PlanFixtures.aPlanNativeV4());

            var output = executeMerge("{\"security\":{\"type\":\"API_KEY\",\"configuration\":{\"foo\":\"bar\"}}}");

            assertThat(output.plan().getPlanDefinitionHttpV4()).isNull();
        }
    }

    @Nested
    class RejectStatusPatch {

        @Test
        void json_patch_on_status_throws_PlanPatchNotAllowedException_with_field_metadata() {
            assertThatThrownBy(() ->
                executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"CLOSED\"}]")
            ).isInstanceOfSatisfying(PlanPatchNotAllowedException.class, ex -> {
                assertThat(ex.getParameters()).containsEntry("field", "status");
                assertThat(ex.getMessage()).contains("use plan lifecycle endpoints");
            });
        }

        @Test
        void merge_patch_with_status_key_throws_PlanPatchNotAllowedException() {
            assertThatThrownBy(() -> executeMerge("{\"status\":\"CLOSED\"}")).isInstanceOfSatisfying(
                PlanPatchNotAllowedException.class,
                ex -> assertThat(ex.getParameters()).containsEntry("field", "status")
            );
        }

        @Test
        void json_patch_on_nested_status_path_is_rejected_at_top_level_status() {
            assertThatThrownBy(() ->
                executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/status/foo\",\"value\":\"X\"}]")
            ).isInstanceOfSatisfying(PlanPatchNotAllowedException.class, ex ->
                assertThat(ex.getParameters()).containsEntry("field", "status")
            );
        }

        @Test
        void json_patch_move_from_status_throws_PlanPatchNotAllowedException_with_field_metadata() {
            assertThatThrownBy(() ->
                executeJsonPatch("[{\"op\":\"move\",\"from\":\"/status\",\"path\":\"/name\"}]")
            ).isInstanceOfSatisfying(PlanPatchNotAllowedException.class, ex -> {
                assertThat(ex.getParameters()).containsEntry("field", "status");
                assertThat(ex.getMessage()).contains("use plan lifecycle endpoints");
            });
        }

        @Test
        void json_patch_copy_from_status_throws_PlanPatchNotAllowedException_with_field_metadata() {
            assertThatThrownBy(() ->
                executeJsonPatch("[{\"op\":\"copy\",\"from\":\"/status\",\"path\":\"/name\"}]")
            ).isInstanceOfSatisfying(PlanPatchNotAllowedException.class, ex -> {
                assertThat(ex.getParameters()).containsEntry("field", "status");
                assertThat(ex.getMessage()).contains("use plan lifecycle endpoints");
            });
        }

        @Test
        void json_patch_test_op_against_non_patchable_field_is_rejected() {
            assertThatThrownBy(() ->
                executeJsonPatch("[{\"op\":\"test\",\"path\":\"/status\",\"value\":\"PUBLISHED\"}]")
            ).isInstanceOfSatisfying(PlanPatchNotAllowedException.class, ex -> {
                assertThat(ex.getParameters()).containsEntry("field", "status");
                assertThat(ex.getMessage()).contains("use plan lifecycle endpoints");
            });
        }
    }

    @Nested
    class RejectUnlistedField {

        @Test
        void json_patch_on_id_throws_PlanPatchNotAllowedException_with_field_is_not_patchable() {
            assertThatThrownBy(() ->
                executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/id\",\"value\":\"new-id\"}]")
            ).isInstanceOfSatisfying(PlanPatchNotAllowedException.class, ex -> {
                assertThat(ex.getParameters()).containsEntry("field", "id");
                assertThat(ex.getMessage()).contains("field is not patchable");
            });
        }

        @Test
        void merge_patch_with_created_at_throws_PlanPatchNotAllowedException() {
            assertThatThrownBy(() -> executeMerge("{\"createdAt\":\"2024-01-01T00:00:00Z\"}")).isInstanceOfSatisfying(
                PlanPatchNotAllowedException.class,
                ex -> assertThat(ex.getParameters()).containsEntry("field", "createdAt")
            );
        }

        @Test
        void json_patch_on_security_type_is_ignored_and_leaves_the_type_frozen() {
            var originalType = planCrudService.storage().get(0).getPlanDefinitionHttpV4().getSecurity().getType();

            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/security/type\",\"value\":\"API_KEY\"}]");

            assertThat(output.plan().getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo(originalType);
        }

        @Test
        void json_patch_with_null_value_on_nested_security_pointer_leaves_top_level_security_unchanged() {
            var originalSecurity = planCrudService.storage().get(0).getPlanDefinitionHttpV4().getSecurity();

            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/security/type\",\"value\":null}]");

            var patchedSecurity = output.plan().getPlanDefinitionHttpV4().getSecurity();
            assertThat(patchedSecurity).isNotNull();
            assertThat(patchedSecurity.getType()).isEqualTo(originalSecurity.getType());
        }
    }

    @Nested
    class RejectMalformedPatch {

        @Test
        void json_patch_body_is_not_a_json_array() {
            assertThatThrownBy(() -> executeJsonPatch("{\"op\":\"replace\"}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("JSON Patch body must be a JSON array");
        }

        @Test
        void json_patch_op_missing_path_surfaces_index() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"add\",\"value\":\"x\"},{\"op\":\"replace\",\"value\":\"y\"}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("index 0")
                .hasMessageContaining("missing required 'path' field");
        }

        @Test
        void json_patch_move_op_missing_from_surfaces_index() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"move\",\"path\":\"/name\"}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("index 0")
                .hasMessageContaining("missing required 'from' field");
        }

        @Test
        void json_patch_move_op_from_does_not_start_with_slash() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"move\",\"from\":\"status\",\"path\":\"/name\"}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("index 0")
                .hasMessageContaining("'from' value 'status'")
                .hasMessageContaining("not a valid JSON Pointer");
        }

        @Test
        void json_patch_copy_op_from_is_just_slash_empty_top_level_field() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"copy\",\"from\":\"/\",\"path\":\"/name\"}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("index 0")
                .hasMessageContaining("'from' value '/'")
                .hasMessageContaining("does not target a specific field");
        }

        @Test
        void merge_patch_with_non_textual_description_object_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"description\":{\"nested\":\"obj\"}}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Invalid value for field 'description'");
        }

        @Test
        void merge_patch_with_non_boolean_comment_required_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"commentRequired\":\"yes\"}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Invalid value for field 'commentRequired'");
        }

        @Test
        void json_patch_op_path_is_non_textual_number() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"replace\",\"path\":42,\"value\":\"x\"}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("missing required 'path' field");
        }

        @Test
        void json_patch_op_path_does_not_start_with_slash() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"replace\",\"path\":\"name\",\"value\":\"x\"}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("not a valid JSON Pointer");
        }

        @Test
        void json_patch_op_path_is_just_slash_empty_top_level_field() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/\",\"value\":\"x\"}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("does not target a specific field");
        }

        @Test
        void merge_patch_body_is_not_a_json_object() {
            assertThatThrownBy(() -> executeMerge("[1,2,3]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("JSON Merge Patch body must be a JSON object");
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   " })
        void patch_body_is_blank(String body) {
            assertThatThrownBy(() -> useCase.execute(baseInput().patchType(PatchPlanUseCase.PatchType.MERGE_PATCH).patchBody(body).build()))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Patch body is required");
            assertThatThrownBy(() -> useCase.execute(baseInput().patchType(PatchPlanUseCase.PatchType.JSON_PATCH).patchBody(body).build()))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Patch body is required");
        }

        @Test
        void patch_body_is_unparsable_json() {
            assertThatThrownBy(() -> executeMerge("{not json"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Invalid patch body");
        }

        @Test
        void null_patch_type_is_rejected_instead_of_silently_defaulting_to_merge_patch() {
            assertThatThrownBy(() -> useCase.execute(baseInput().patchType(null).patchBody("{\"name\":\"Renamed\"}").build()))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("patchType");
        }
    }

    @Nested
    class RejectTooManyOps {

        @Test
        void json_patch_with_201_ops_is_rejected() {
            var body = buildJsonPatchOps(201);

            assertThatThrownBy(() -> executeJsonPatch(body))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("exceeds maximum of 200 operations");
        }

        @Test
        void json_patch_with_exactly_200_ops_is_accepted() {
            var body = buildJsonPatchOps(200);

            var output = executeJsonPatch(body);

            assertThat(output.plan().getName()).isEqualTo("n199");
        }

        private String buildJsonPatchOps(int count) {
            var joiner = new StringBuilder("[");
            IntStream.range(0, count).forEach(i -> {
                if (i > 0) joiner.append(',');
                joiner.append("{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"n").append(i).append("\"}");
            });
            joiner.append("]");
            return joiner.toString();
        }
    }

    @Nested
    class DryRun {

        @Test
        void dry_run_merge_patch_returns_updated_plan_but_does_not_persist() {
            var originalName = planCrudService.storage().get(0).getName();

            var output = useCase.execute(
                baseInput().patchType(PatchPlanUseCase.PatchType.MERGE_PATCH).patchBody("{\"name\":\"Renamed\"}").dryRun(true).build()
            );

            assertThat(output.plan().getName()).isEqualTo("Renamed");
            assertThat(planCrudService.storage().get(0).getName()).isEqualTo(originalName);
        }

        @Test
        void dry_run_rejects_domain_invalid_would_be_state_and_persists_nothing() {
            var originalTags = planCrudService.storage().get(0).getPlanDefinitionHttpV4().getTags();

            assertThatThrownBy(() ->
                useCase.execute(
                    baseInput()
                        .patchType(PatchPlanUseCase.PatchType.MERGE_PATCH)
                        .patchBody("{\"tags\":[\"forbidden-tag\"]}")
                        .dryRun(true)
                        .build()
                )
            ).isInstanceOf(ValidationDomainException.class);

            assertThat(planCrudService.storage().get(0).getPlanDefinitionHttpV4().getTags()).isEqualTo(originalTags);
            assertThat(auditCrudService.storage()).isEmpty();
        }

        @Test
        void dry_run_still_rejects_disallowed_patches() {
            assertThatThrownBy(() ->
                useCase.execute(
                    baseInput()
                        .patchType(PatchPlanUseCase.PatchType.JSON_PATCH)
                        .patchBody("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"CLOSED\"}]")
                        .dryRun(true)
                        .build()
                )
            ).isInstanceOf(PlanPatchNotAllowedException.class);
        }

        @Test
        void dry_run_with_valid_flows_does_not_persist_flows() {
            flowCrudService.savePlanFlows(PLAN_ID, List.of(Flow.builder().id("flow-id").name("Original").enabled(true).build()));

            useCase.execute(
                baseInput()
                    .patchType(PatchPlanUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"flows\":[{\"name\":\"Patched\",\"enabled\":true}]}")
                    .dryRun(true)
                    .build()
            );

            assertThat(flowCrudService.getPlanV4Flows(PLAN_ID)).hasSize(1);
            assertThat(flowCrudService.getPlanV4Flows(PLAN_ID).get(0).getId()).isEqualTo("flow-id");
        }

        @Test
        void dry_run_with_bogus_selector_discriminator_throws_ValidationDomainException_before_dryRun_guard() {
            var originalName = planCrudService.storage().get(0).getName();

            assertThatThrownBy(() ->
                useCase.execute(
                    baseInput()
                        .patchType(PatchPlanUseCase.PatchType.MERGE_PATCH)
                        .patchBody(
                            "{\"flows\":[{\"name\":\"Flow\",\"enabled\":true,\"selectors\":[{\"type\":\"BOGUS\",\"path\":\"/\",\"pathOperator\":\"EQUALS\"}]}]}"
                        )
                        .dryRun(true)
                        .build()
                )
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("flows");

            assertThat(planCrudService.storage().get(0).getName()).isEqualTo(originalName);
        }
    }

    @Nested
    class ScopeGuard {

        @Test
        void non_v4_api_throws_ValidationDomainException_with_v4_only_message() {
            apiCrudService.reset();
            apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2()));

            assertThatThrownBy(() -> executeMerge("{\"name\":\"X\"}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Plan PATCH is only supported for V4 API plans");
        }

        @Test
        void non_proxy_v4_api_throws_ValidationDomainException_with_http_proxy_only_message() {
            apiCrudService.reset();
            apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4()));

            assertThatThrownBy(() -> executeMerge("{\"name\":\"X\"}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Plan PATCH is only supported for HTTP Proxy API plans");
        }
    }

    @Nested
    class CrossApiNotFound {

        @Test
        void plan_with_mismatched_reference_id_throws_PlanForApiNotFoundException() {
            planCrudService.reset();
            var planForOtherApi = PlanFixtures.aPlanHttpV4().toBuilder().referenceId("OTHER-API").build();
            planCrudService.initWith(List.of(planForOtherApi));

            assertThatThrownBy(() -> executeMerge("{\"name\":\"X\"}")).isInstanceOfSatisfying(PlanForApiNotFoundException.class, ex -> {
                assertThat(ex.getMessage()).contains(PLAN_ID);
                assertThat(ex.getMessage()).contains(API_ID);
            });
        }

        @Test
        void plan_does_not_exist_throws_PlanForApiNotFoundException() {
            planCrudService.reset();

            assertThatThrownBy(() -> executeMerge("{\"name\":\"X\"}")).isInstanceOf(PlanForApiNotFoundException.class);
        }
    }

    @Nested
    class PerFieldTypeCoercion {

        @Test
        void merge_with_boolean_updates_comment_required() {
            var output = executeMerge("{\"commentRequired\":true}");

            assertThat(output.plan().isCommentRequired()).isTrue();
        }

        @Test
        void merge_with_enum_value_updates_validation() {
            var output = executeMerge("{\"validation\":\"MANUAL\"}");

            assertThat(output.plan().getValidation()).isEqualTo(Plan.PlanValidationType.MANUAL);
        }

        @Test
        void merge_with_invalid_enum_value_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"validation\":\"NOT_A_VALUE\"}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Invalid validation value: NOT_A_VALUE");
        }

        @Test
        void merge_with_string_list_updates_tags() {
            var output = executeMerge("{\"excludedGroups\":[\"a\",\"b\"]}");

            assertThat(output.plan().getExcludedGroups()).containsExactly("a", "b");
        }

        @Test
        void merge_with_non_string_list_entries_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"excludedGroups\":[{\"nested\":\"obj\"}]}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Invalid value for field 'excludedGroups'");
        }

        @Test
        void json_patch_with_explicit_null_string_list_clears_excluded_groups() {
            var seeded = PlanFixtures.aPlanHttpV4().toBuilder().excludedGroups(List.of("g1", "g2")).build();
            seed(seeded);

            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/excludedGroups\",\"value\":null}]");

            assertThat(output.plan().getExcludedGroups()).isNull();
        }

        @Test
        void merge_missing_comment_required_preserves_existing_boolean() {
            var seeded = PlanFixtures.aPlanHttpV4().toBuilder().commentRequired(true).build();
            seed(seeded);

            var output = executeMerge("{\"description\":\"x\"}");

            assertThat(output.plan().isCommentRequired()).isTrue();
        }
    }

    @Nested
    class BlankNameRejection {

        @Test
        void merge_with_empty_name_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"name\":\"\"}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("'name' cannot be blank");
        }

        @Test
        void merge_with_whitespace_only_name_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"name\":\"   \"}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("'name' cannot be blank");
        }

        @Test
        void merge_with_explicit_null_name_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"name\":null}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Field 'name' cannot be set to null");
        }
    }

    @Nested
    class TagsFieldProjection {

        @Test
        void merge_patch_updating_tags_persists_the_new_tag_set() {
            var output = executeMerge("{\"tags\":[\"tag1\"]}");

            assertThat(output.plan().getPlanDefinitionHttpV4().getTags()).containsExactly("tag1");
            assertThat(planCrudService.storage().get(0).getPlanDefinitionHttpV4().getTags()).containsExactly("tag1");
        }

        @Test
        void json_patch_add_on_tags_persists_the_new_tag_set() {
            var output = executeJsonPatch("[{\"op\":\"add\",\"path\":\"/tags\",\"value\":[\"tag1\"]}]");

            assertThat(output.plan().getPlanDefinitionHttpV4().getTags()).containsExactly("tag1");
        }
    }

    @Nested
    class SecurityFieldProjection {

        @Test
        void merge_patch_updates_security_configuration_but_keeps_the_type_frozen() {
            var originalType = planCrudService.storage().get(0).getPlanDefinitionHttpV4().getSecurity().getType();

            var output = executeMerge("{\"security\":{\"type\":\"API_KEY\",\"configuration\":{\"foo\":\"bar\"}}}");

            var security = output.plan().getPlanDefinitionHttpV4().getSecurity();
            assertThat(security).isNotNull();
            assertThat(security.getType()).isEqualTo(originalType);
            assertThat(security.getConfiguration()).contains("\"foo\":\"bar\"");
        }
    }

    @Nested
    class FlowsFieldProjection {

        @Test
        void merge_patch_setting_flows_to_empty_replaces_the_flow_list() {
            flowCrudService.savePlanFlows(PLAN_ID, List.of(new Flow()));

            var output = executeMerge("{\"flows\":[]}");

            assertThat(output.plan().getPlanDefinitionHttpV4().getFlows()).isNullOrEmpty();
            assertThat(flowCrudService.getPlanV4Flows(PLAN_ID)).isEmpty();
        }

        @Test
        void json_patch_replace_on_flows_replaces_the_flow_list() {
            flowCrudService.savePlanFlows(PLAN_ID, List.of(new Flow()));

            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/flows\",\"value\":[]}]");

            assertThat(output.plan().getPlanDefinitionHttpV4().getFlows()).isNullOrEmpty();
        }

        @Test
        void scalar_only_name_patch_leaves_flows_untouched() {
            flowCrudService.savePlanFlows(PLAN_ID, List.of(Flow.builder().id("flow-id").name("Original").enabled(true).build()));

            var output = executeMerge("{\"name\":\"Scalar Only\"}");

            assertThat(output.plan().getName()).isEqualTo("Scalar Only");
            assertThat(flowCrudService.getPlanV4Flows(PLAN_ID)).hasSize(1);
            assertThat(flowCrudService.getPlanV4Flows(PLAN_ID).get(0).getId()).isEqualTo("flow-id");
        }
    }

    @Nested
    class DryRunJsonPatch {

        @Test
        void dry_run_json_patch_returns_updated_plan_but_does_not_persist() {
            var originalName = planCrudService.storage().get(0).getName();

            var output = useCase.execute(
                baseInput()
                    .patchType(PatchPlanUseCase.PatchType.JSON_PATCH)
                    .patchBody("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Dry JSON Patched\"}]")
                    .dryRun(true)
                    .build()
            );

            assertThat(output.plan().getName()).isEqualTo("Dry JSON Patched");
            assertThat(planCrudService.storage().get(0).getName()).isEqualTo(originalName);
        }
    }

    @Nested
    class RemoveOpOnNonNullableScalar {

        @Test
        void json_patch_remove_on_name_is_rejected_like_replace_with_null() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"remove\",\"path\":\"/name\"}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Field 'name' cannot be set to null");
        }

        @Test
        void json_patch_remove_with_nested_pointer_on_nullable_field_does_not_clear_top_level_field() {
            var seeded = PlanFixtures.aPlanHttpV4().toBuilder().description("seeded description").build();
            seeded.getPlanDefinitionHttpV4().setTags(Set.of("tag1"));
            seed(seeded);

            var output = executeJsonPatch("[{\"op\":\"remove\",\"path\":\"/tags/0\"}]");

            assertThat(output.plan().getPlanDefinitionHttpV4().getTags()).isNotNull();
            assertThat(output.plan().getDescription()).isEqualTo("seeded description");
        }
    }

    @Nested
    class AuditLogging {

        @Test
        void successful_non_dry_run_patch_emits_one_audit_entry_for_the_plan() {
            executeMerge("{\"name\":\"Audited\"}");

            assertThat(auditCrudService.storage())
                .hasSize(1)
                .first()
                .satisfies(entry -> assertThat(entry.getProperties()).containsValue(PLAN_ID));
        }

        @Test
        void dry_run_patch_emits_no_audit_entry() {
            useCase.execute(
                baseInput().patchType(PatchPlanUseCase.PatchType.MERGE_PATCH).patchBody("{\"name\":\"DryAudit\"}").dryRun(true).build()
            );

            assertThat(auditCrudService.storage()).isEmpty();
        }
    }

    @Nested
    class DomainInvalidPostPatchState {

        @Test
        void valid_shaped_flow_patch_rejected_by_real_flow_validator_propagates_field_level_detail_and_persists_nothing() {
            var originalFlows = flowCrudService.getPlanV4Flows(PLAN_ID);

            assertThatThrownBy(() ->
                executeMerge("{\"flows\":[{\"name\":\"f1\",\"selectors\":[{\"type\":\"channel\",\"channel\":\"/c\"}]}]}")
            ).isInstanceOfSatisfying(ValidationDomainException.class, ex -> {
                assertThat(ex.getParameters()).containsEntry("flowName", "f1");
                assertThat(ex.getParameters()).containsEntry("invalidSelectors", "channel");
            });

            assertThat(auditCrudService.storage()).isEmpty();
            assertThat(flowCrudService.getPlanV4Flows(PLAN_ID)).isEqualTo(originalFlows);
        }
    }

    @Nested
    class MalformedStructuredFieldCoercion {

        @Test
        void merge_with_type_incoherent_flows_throws_ValidationDomainException_not_IllegalArgumentException() {
            assertThatThrownBy(() -> executeMerge("{\"flows\":[{\"name\":{\"nested\":\"object\"}}]}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Invalid value for field 'flows'");
        }

        @Test
        void merge_with_type_incoherent_security_throws_ValidationDomainException_not_IllegalArgumentException() {
            assertThatThrownBy(() -> executeMerge("{\"security\":[1,2,3]}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("security");
        }

        @Test
        void merge_with_type_incoherent_tags_throws_ValidationDomainException_not_IllegalArgumentException() {
            assertThatThrownBy(() -> executeMerge("{\"tags\":\"not-an-array\"}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("tags");
        }

        @Test
        void merge_flows_with_bogus_selector_discriminator_throws_ValidationDomainException_referencing_flows_and_does_not_persist() {
            var originalName = planCrudService.storage().get(0).getName();

            assertThatThrownBy(() ->
                executeMerge(
                    "{\"flows\":[{\"name\":\"Flow\",\"enabled\":true,\"selectors\":[{\"type\":\"BOGUS\",\"path\":\"/\",\"pathOperator\":\"EQUALS\"}]}]}"
                )
            ).isInstanceOfSatisfying(ValidationDomainException.class, ex -> assertThat(ex.getMessage()).contains("flows"));
            assertThat(planCrudService.storage().get(0).getName()).isEqualTo(originalName);
        }
    }

    @Nested
    class PlanIsolationFullFields {

        @Test
        void patching_plan_a_does_not_mutate_plan_b_tags_or_characteristics() {
            var planA = PlanFixtures.aPlanHttpV4();
            var planB = PlanFixtures.aPlanHttpV4().toBuilder().id("plan-b").name("Plan B").build();
            planB.getPlanDefinitionHttpV4().setTags(null);
            seed(planA, planB);

            useCase.execute(
                baseInput()
                    .planId(planA.getId())
                    .patchType(PatchPlanUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"tags\":[\"tag1\"],\"characteristics\":[\"c1\"]}")
                    .build()
            );

            var planBAfter = planCrudService
                .storage()
                .stream()
                .filter(p -> "plan-b".equals(p.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(planBAfter.getPlanDefinitionHttpV4().getTags()).isNullOrEmpty();
            assertThat(planBAfter.getCharacteristics()).isNullOrEmpty();
        }
    }

    @Nested
    class RfcNullClearing {

        private void seedFullyPopulatedPlan() {
            var seeded = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .description("seeded description")
                .generalConditions("seeded-gc")
                .excludedGroups(List.of("g1", "g2"))
                .characteristics(List.of("c1", "c2"))
                .build();
            seeded.getPlanDefinitionHttpV4().setSelectionRule("#context.attributes['x'] == 'y'");
            seeded.getPlanDefinitionHttpV4().setTags(Set.of("tag1"));
            seed(seeded);
        }

        @Test
        void merge_explicit_null_clears_description() {
            seedFullyPopulatedPlan();

            var output = executeMerge("{\"description\":null}");

            assertThat(output.plan().getDescription()).isNull();
        }

        @Test
        void merge_explicit_null_clears_general_conditions() {
            seedFullyPopulatedPlan();

            var output = executeMerge("{\"generalConditions\":null}");

            assertThat(output.plan().getGeneralConditions()).isNull();
        }

        @Test
        void merge_explicit_null_clears_selection_rule() {
            seedFullyPopulatedPlan();

            var output = executeMerge("{\"selectionRule\":null}");

            assertThat(output.plan().getPlanDefinitionHttpV4().getSelectionRule()).isNull();
        }

        @Test
        void merge_explicit_null_clears_excluded_groups() {
            seedFullyPopulatedPlan();

            var output = executeMerge("{\"excludedGroups\":null}");

            assertThat(output.plan().getExcludedGroups()).isNull();
        }

        @Test
        void merge_explicit_null_clears_characteristics() {
            seedFullyPopulatedPlan();

            var output = executeMerge("{\"characteristics\":null}");

            assertThat(output.plan().getCharacteristics()).isNull();
        }

        @Test
        void merge_explicit_null_clears_tags() {
            seedFullyPopulatedPlan();

            var output = executeMerge("{\"tags\":null}");

            assertThat(output.plan().getPlanDefinitionHttpV4().getTags()).isNullOrEmpty();
        }

        @Test
        void merge_explicit_null_clears_flows() {
            seedFullyPopulatedPlan();
            flowCrudService.savePlanFlows(PLAN_ID, List.of(new Flow()));

            var output = executeMerge("{\"flows\":null}");

            assertThat(output.plan().getPlanDefinitionHttpV4().getFlows()).isNullOrEmpty();
            assertThat(flowCrudService.getPlanV4Flows(PLAN_ID)).isEmpty();
        }

        @Test
        void json_patch_replace_null_clears_description() {
            seedFullyPopulatedPlan();

            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/description\",\"value\":null}]");

            assertThat(output.plan().getDescription()).isNull();
        }

        @Test
        void json_patch_add_null_clears_description() {
            seedFullyPopulatedPlan();

            var output = executeJsonPatch("[{\"op\":\"add\",\"path\":\"/description\",\"value\":null}]");

            assertThat(output.plan().getDescription()).isNull();
        }

        @Test
        void json_patch_replace_null_clears_flows() {
            seedFullyPopulatedPlan();
            flowCrudService.savePlanFlows(PLAN_ID, List.of(new Flow()));

            var output = executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/flows\",\"value\":null}]");

            assertThat(output.plan().getPlanDefinitionHttpV4().getFlows()).isNullOrEmpty();
            assertThat(flowCrudService.getPlanV4Flows(PLAN_ID)).isEmpty();
        }

        @Test
        void merge_explicit_null_validation_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"validation\":null}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Field 'validation' cannot be set to null");
        }

        @Test
        void merge_explicit_null_security_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"security\":null}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Field 'security' cannot be set to null");
        }

        @Test
        void merge_explicit_null_comment_required_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeMerge("{\"commentRequired\":null}"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Field 'commentRequired' cannot be set to null");
        }

        @Test
        void json_patch_replace_null_name_throws_ValidationDomainException() {
            assertThatThrownBy(() -> executeJsonPatch("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":null}]"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("Field 'name' cannot be set to null");
        }
    }
}
