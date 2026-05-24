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
package io.gravitee.gamma.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.authz.engine.schema.AuthzSchema;
import io.gravitee.authz.engine.schema.AuthzSchemaParser;
import io.gravitee.authz.engine.schema.EntityTypeSchema;
import io.gravitee.authz.engine.schema.SchemaType;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.authorization.api.AuthzAuditPort;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyStatus;
import io.gravitee.gamma.authorization.repository.InMemoryAuthzEntityRepository;
import io.gravitee.gamma.authorization.repository.InMemoryAuthzPolicyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthzSchemaServiceImplTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final String ENV = "env-1";
    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", ENV, "alice");

    private InMemoryAuthzEntityRepository entityRepository;
    private InMemoryAuthzPolicyRepository policyRepository;
    private AuthzSchemaServiceImpl schemaService;
    private AuthzEntityServiceImpl entityService;
    private AuthzPolicyServiceImpl policyService;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        entityRepository = new InMemoryAuthzEntityRepository();
        policyRepository = new InMemoryAuthzPolicyRepository();
        schemaService = new AuthzSchemaServiceImpl(entityRepository, policyRepository);
        AuthzEntityIdValidator validator = new AuthzEntityIdValidator();
        AuthzEventPublisher events = mock(AuthzEventPublisher.class);
        AuthzAuditPort audit = mock(AuthzAuditPort.class);
        entityService = new AuthzEntityServiceImpl(entityRepository, policyRepository, validator, schemaService, events, audit);
        policyService = new AuthzPolicyServiceImpl(policyRepository, validator, schemaService, events, audit);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void empty_environment_returns_empty_schema_block() {
        String schema = schemaService.currentGaplSchema(ENV);

        assertThat(schema).isEqualTo("// No entities or policies defined yet.\n");
    }

    @Test
    void single_api_entity_emits_an_Api_type_with_attributes() {
        entityRepository.save(resource("id-1", "api.123", Map.of("owner", "team-a", "active", true)));

        String schema = schemaService.currentGaplSchema(ENV);

        assertThat(schema).contains("entity Api {").contains("active: Bool").contains("owner: String");
    }

    @Test
    void attributes_are_unioned_across_entities_of_the_same_prefix() {
        entityRepository.save(resource("id-1", "api.123", Map.of("owner", "team-a")));
        entityRepository.save(resource("id-2", "api.456", Map.of("region", "eu")));

        String schema = schemaService.currentGaplSchema(ENV);

        int ownerIdx = schema.indexOf("owner: String");
        int regionIdx = schema.indexOf("region: String");
        assertThat(ownerIdx).isPositive();
        assertThat(regionIdx).isPositive();
        assertThat(ownerIdx).isLessThan(regionIdx);
    }

    @Test
    void distinct_prefixes_yield_distinct_type_blocks() {
        entityRepository.save(resource("id-1", "api.123", Map.of("k", "v")));
        entityRepository.save(resource("id-2", "mcp.123.tool-a", Map.of("k", "v")));
        entityRepository.save(principal("id-3", "idp.am.alice", Map.of("email", "a@b.c")));

        String schema = schemaService.currentGaplSchema(ENV);

        assertThat(schema).contains("entity Api {").contains("entity Mcp {").contains("entity Idp {");
    }

    @Test
    void numeric_and_boolean_attribute_values_map_to_GAPL_primitives() {
        entityRepository.save(resource("id-1", "api.123", Map.of("active", true, "score", 42, "rate", 3.14, "tag", "alpha")));

        String schema = schemaService.currentGaplSchema(ENV);

        assertThat(schema).contains("active: Bool").contains("rate: decimal").contains("score: Long").contains("tag: String");
    }

    @Test
    void result_is_cached_and_subsequent_calls_return_the_same_string_reference() {
        entityRepository.save(resource("id-1", "api.123", Map.of("k", "v")));

        String first = schemaService.currentGaplSchema(ENV);
        String second = schemaService.currentGaplSchema(ENV);

        assertThat(second).isSameAs(first);
        assertThat(schemaService.cachedEnvironments()).contains(ENV);
    }

    @Test
    void invalidate_drops_the_cached_entry_and_a_subsequent_call_recomputes() {
        entityRepository.save(resource("id-1", "api.123", Map.of("k", "v")));
        String first = schemaService.currentGaplSchema(ENV);

        schemaService.invalidate(ENV);
        assertThat(schemaService.cachedEnvironments()).doesNotContain(ENV);

        entityRepository.save(resource("id-2", "mcp.123.tool-a", Map.of("k", "v")));
        String second = schemaService.currentGaplSchema(ENV);

        assertThat(second).isNotEqualTo(first).contains("entity Mcp {");
    }

    @Test
    void entityService_upsert_invalidates_the_schema_cache() {
        String empty = schemaService.currentGaplSchema(ENV);
        assertThat(empty).isEqualTo("// No entities or policies defined yet.\n");

        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim")
        );

        String afterUpsert = schemaService.currentGaplSchema(ENV);
        assertThat(afterUpsert).isNotEqualTo(empty).contains("entity Api {");
    }

    @Test
    void entityService_cascade_delete_invalidates_the_schema_cache() {
        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim")
        );
        String afterUpsert = schemaService.currentGaplSchema(ENV);
        assertThat(afterUpsert).contains("entity Api {");

        entityService.delete(CALLER, "api.123");

        String afterDelete = schemaService.currentGaplSchema(ENV);
        assertThat(afterDelete).isEqualTo("// No entities or policies defined yet.\n");
    }

    @Test
    void entityService_update_invalidates_the_schema_cache() {
        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v1"), List.of(), "apim")
        );
        schemaService.currentGaplSchema(ENV);

        entityService.update(CALLER, "api.123", new UpdateAuthzEntityCommand(Map.of("k", "v1", "extra", "added"), null));

        String schema = schemaService.currentGaplSchema(ENV);
        assertThat(schema).contains("extra: String");
    }

    @Test
    void invalidate_with_null_environmentId_is_a_safe_noop() {
        schemaService.currentGaplSchema(ENV);
        schemaService.invalidate(null);
        assertThat(schemaService.cachedEnvironments()).contains(ENV);
    }

    @Test
    void schema_is_per_environment() {
        entityRepository.save(resource("id-1", "api.123", Map.of("k", "v")));

        String envSchema = schemaService.currentGaplSchema(ENV);
        String otherSchema = schemaService.currentGaplSchema("env-other");

        assertThat(envSchema).contains("entity Api {");
        assertThat(otherSchema).isEqualTo("// No entities or policies defined yet.\n");
    }

    @Test
    void resource_policy_emits_an_empty_type_block_when_no_matching_entity_exists() {
        policyRepository.save(resourcePolicy("p-1", "service.foo"));

        String schema = schemaService.currentGaplSchema(ENV);

        assertThat(schema).contains("entity Service {}");
    }

    @Test
    void resource_policy_for_existing_typeName_reuses_the_block_with_entity_attributes() {
        entityRepository.save(resource("e-1", "service.api", Map.of("region", "eu")));
        policyRepository.save(resourcePolicy("p-1", "service.other"));

        String schema = schemaService.currentGaplSchema(ENV);

        int firstService = schema.indexOf("entity Service {");
        int secondService = schema.indexOf("entity Service {", firstService + 1);
        assertThat(firstService).isNotNegative();
        assertThat(secondService).isEqualTo(-1);
        assertThat(schema).contains("region: String");
    }

    @Test
    void global_policy_with_null_entityId_does_not_affect_the_schema() {
        policyRepository.save(globalPolicy("g-1"));

        String schema = schemaService.currentGaplSchema(ENV);

        assertThat(schema).isEqualTo("// No entities or policies defined yet.\n");
    }

    @Test
    void resource_policy_with_entityId_without_dot_does_not_add_a_type_block() {
        policyRepository.save(resourcePolicy("p-1", "api-1"));

        String schema = schemaService.currentGaplSchema(ENV);

        assertThat(schema).isEqualTo("// No entities or policies defined yet.\n");
    }

    @Test
    void policyService_create_invalidates_the_schema_cache() {
        String empty = schemaService.currentGaplSchema(ENV);
        assertThat(empty).isEqualTo("// No entities or policies defined yet.\n");

        policyService.create(CALLER, new CreateAuthzPolicyCommand(ENV, "p1", AuthzPolicyKind.RESOURCE, "service.foo", ""));

        String after = schemaService.currentGaplSchema(ENV);
        assertThat(after).isNotEqualTo(empty).contains("entity Service {");
    }

    @Test
    void policyService_update_invalidates_the_schema_cache() {
        AuthzPolicy created = policyService.create(
            CALLER,
            new CreateAuthzPolicyCommand(ENV, "p1", AuthzPolicyKind.RESOURCE, "service.foo", "")
        );
        schemaService.currentGaplSchema(ENV);
        assertThat(schemaService.cachedEnvironments()).contains(ENV);

        policyService.update(CALLER, created.id(), new UpdateAuthzPolicyCommand("renamed", ""));

        assertThat(schemaService.cachedEnvironments()).doesNotContain(ENV);
    }

    @Test
    void policyService_deploy_invalidates_the_schema_cache() {
        AuthzPolicy created = policyService.create(
            CALLER,
            new CreateAuthzPolicyCommand(ENV, "p1", AuthzPolicyKind.RESOURCE, "service.foo", "")
        );
        schemaService.currentGaplSchema(ENV);
        assertThat(schemaService.cachedEnvironments()).contains(ENV);

        policyService.deploy(CALLER, created.id());

        assertThat(schemaService.cachedEnvironments()).doesNotContain(ENV);
    }

    @Test
    void policyService_disable_invalidates_the_schema_cache() {
        AuthzPolicy created = policyService.create(
            CALLER,
            new CreateAuthzPolicyCommand(ENV, "p1", AuthzPolicyKind.RESOURCE, "service.foo", "")
        );
        policyService.deploy(CALLER, created.id());
        schemaService.currentGaplSchema(ENV);
        assertThat(schemaService.cachedEnvironments()).contains(ENV);

        policyService.disable(CALLER, created.id());

        assertThat(schemaService.cachedEnvironments()).doesNotContain(ENV);
    }

    @Test
    void policyService_delete_invalidates_the_schema_cache() {
        AuthzPolicy created = policyService.create(
            CALLER,
            new CreateAuthzPolicyCommand(ENV, "p1", AuthzPolicyKind.RESOURCE, "service.foo", "")
        );
        schemaService.currentGaplSchema(ENV);
        assertThat(schemaService.cachedEnvironments()).contains(ENV);

        policyService.delete(CALLER, created.id());

        assertThat(schemaService.cachedEnvironments()).doesNotContain(ENV);
    }

    // ── Round-trip: generated GAPL must be parseable by the authz-engine ──

    @Test
    void empty_schema_parses_cleanly_through_AuthzSchemaParser() {
        AuthzSchema parsed = AuthzSchemaParser.parse(schemaService.currentGaplSchema(ENV));

        assertThat(parsed).isNotNull();
        assertThat(parsed.entityTypes()).isEmpty();
    }

    @Test
    void generated_schema_parses_and_primitive_attributes_resolve_to_primitive_types() {
        entityRepository.save(resource("id-1", "api.123", Map.of("active", true, "count", 42L, "rate", 3.14, "name", "x")));

        String schema = schemaService.currentGaplSchema(ENV);
        AuthzSchema parsed = AuthzSchemaParser.parse(schema);

        EntityTypeSchema apiType = parsed.getEntityType("Api");
        assertThat(apiType).as("entity Api must be declared in the generated schema").isNotNull();

        for (var attribute : apiType.attributes().entrySet()) {
            SchemaType type = attribute.getValue().type();
            assertThat(type)
                .as(
                    "Attribute '%s' resolved to %s — primitive type names must not fall through to EntityRefType",
                    attribute.getKey(),
                    type.getClass().getSimpleName()
                )
                .isNotInstanceOf(SchemaType.EntityRefType.class);
        }
    }

    @Test
    void schema_with_resource_policy_only_parses_and_emits_an_empty_entity_block() {
        policyRepository.save(resourcePolicy("p-1", "service.foo"));

        AuthzSchema parsed = AuthzSchemaParser.parse(schemaService.currentGaplSchema(ENV));

        EntityTypeSchema service = parsed.getEntityType("Service");
        assertThat(service).isNotNull();
        assertThat(service.attributes()).isEmpty();
    }

    private static AuthzEntity resource(String id, String entityId, Map<String, Object> attributes) {
        return new AuthzEntity(id, entityId, AuthzEntityKind.RESOURCE, attributes, List.of(), "apim", ENV, NOW, NOW);
    }

    private static AuthzEntity principal(String id, String entityId, Map<String, Object> attributes) {
        return new AuthzEntity(id, entityId, AuthzEntityKind.PRINCIPAL, attributes, List.of(), "gravitee_am_default", ENV, NOW, NOW);
    }

    private static AuthzPolicy globalPolicy(String id) {
        return new AuthzPolicy(id, id + "-name", AuthzPolicyKind.GLOBAL, null, "", AuthzPolicyStatus.DRAFT, ENV, NOW, NOW);
    }

    private static AuthzPolicy resourcePolicy(String id, String entityId) {
        return new AuthzPolicy(id, id + "-name", AuthzPolicyKind.RESOURCE, entityId, "", AuthzPolicyStatus.DRAFT, ENV, NOW, NOW);
    }
}
