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
package io.gravitee.gamma.module.platform.fixture;

import inmemory.AuditCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialAuditDomainService;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialOwnerDomainService;
import io.gravitee.gamma.module.platform.core.credential.domain_service.CredentialSecretsDomainService;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialStrategy;
import io.gravitee.gamma.module.platform.core.credential.model.NewCredentialCommand;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialSecretStore;
import io.gravitee.gamma.module.platform.core.credential.use_case.CreateCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.DeleteCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.GetCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.ListCredentialConsumersUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.RotateCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.SearchCredentialsUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.UpdateCredentialUseCase;
import io.gravitee.gamma.module.platform.inmemory.InMemoryCredentialConsumers;
import io.gravitee.gamma.module.platform.inmemory.InMemoryCredentialRepository;
import io.gravitee.gamma.module.platform.inmemory.InMemoryCredentialSecretStore;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class CredentialFixture {

    public static final String ORG_ID = "org-id";
    public static final String ENV_ID = "env-id";
    public static final String OWNER_ID = "owner-id";
    public static final String ADMIN_ID = "admin-id";
    public static final String OTHER_USER_ID = "other-user-id";
    public static final String FOREIGN_USER_ID = "foreign-user-id";
    public static final Instant NEXT_YEAR = Instant.parse("2024-10-22T10:15:30Z");

    public InMemoryCredentialRepository credentialRepository = new InMemoryCredentialRepository();
    public InMemoryCredentialSecretStore secretStore = new InMemoryCredentialSecretStore();
    public InMemoryCredentialConsumers consumers = new InMemoryCredentialConsumers();
    public AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    public UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    public CreateCredentialUseCase createCredentialUseCase;
    public GetCredentialUseCase getCredentialUseCase;
    public SearchCredentialsUseCase searchCredentialsUseCase;
    public UpdateCredentialUseCase updateCredentialUseCase;
    public RotateCredentialUseCase rotateCredentialUseCase;
    public DeleteCredentialUseCase deleteCredentialUseCase;
    public ListCredentialConsumersUseCase listCredentialConsumersUseCase;

    public CredentialFixture() {
        userCrudService.initWith(
            List.of(user(OWNER_ID, ORG_ID), user(ADMIN_ID, ORG_ID), user(OTHER_USER_ID, ORG_ID), user(FOREIGN_USER_ID, "other-org"))
        );
        build();
    }

    /** Rewires the use cases, for a test that swapped one of the public in-memory fields. */
    public void build() {
        build(secretStore);
    }

    /** Rewires the use cases on another secret store, such as the host's own secret use cases. */
    public void build(CredentialSecretStore store) {
        var audit = new CredentialAuditDomainService(
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor())
        );
        var secrets = new CredentialSecretsDomainService(store);
        var owners = new CredentialOwnerDomainService(userCrudService);
        createCredentialUseCase = new CreateCredentialUseCase(credentialRepository, secrets, owners, audit);
        getCredentialUseCase = new GetCredentialUseCase(credentialRepository);
        searchCredentialsUseCase = new SearchCredentialsUseCase(credentialRepository);
        updateCredentialUseCase = new UpdateCredentialUseCase(credentialRepository, owners, audit);
        rotateCredentialUseCase = new RotateCredentialUseCase(credentialRepository, secrets, audit);
        deleteCredentialUseCase = new DeleteCredentialUseCase(credentialRepository, secrets, consumers, audit);
        listCredentialConsumersUseCase = new ListCredentialConsumersUseCase(credentialRepository, consumers);
    }

    public CredentialActor owner() {
        return actor(OWNER_ID, false, EnumSet.allOf(SecretAccess.class));
    }

    public CredentialActor admin() {
        return actor(ADMIN_ID, true, EnumSet.allOf(SecretAccess.class));
    }

    public CredentialActor otherUser() {
        return actor(OTHER_USER_ID, false, EnumSet.allOf(SecretAccess.class));
    }

    public CredentialActor ownerWithSecretAccess(SecretAccess... access) {
        return actor(OWNER_ID, false, access.length == 0 ? EnumSet.noneOf(SecretAccess.class) : EnumSet.copyOf(List.of(access)));
    }

    /** An admin with every secret permission, acting in another organization or environment than the credentials. */
    public static CredentialActor adminIn(String organizationId, String environmentId) {
        var auditInfo = AuditInfo.builder()
            .organizationId(organizationId)
            .environmentId(environmentId)
            .actor(AuditActor.builder().userId(ADMIN_ID).build())
            .build();
        return new CredentialActor(auditInfo, true, EnumSet.allOf(SecretAccess.class));
    }

    public static CredentialActor actor(String userId, boolean admin, Set<SecretAccess> secretAccess) {
        var auditInfo = AuditInfo.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .actor(AuditActor.builder().userId(userId).build())
            .build();
        return new CredentialActor(auditInfo, admin, secretAccess);
    }

    /** A bearer credential that references a secret the user already has. */
    public Credential givenACredential() {
        return givenACredential(builder -> builder);
    }

    public Credential givenACredential(UnaryOperator<NewCredentialCommand.NewCredentialCommandBuilder> customizer) {
        var defaults = NewCredentialCommand.builder()
            .name("OpenAI production")
            .strategy(CredentialStrategy.BEARER)
            .serviceId("openai")
            .references(Map.of("token", "/vault/secret/openai:token"));
        return create(customizer.apply(defaults).build());
    }

    /** A bearer credential whose value Gravitee stores. */
    public Credential givenAManagedCredential() {
        return givenAManagedCredential(builder -> builder);
    }

    public Credential givenAManagedCredential(UnaryOperator<NewCredentialCommand.NewCredentialCommandBuilder> customizer) {
        var defaults = NewCredentialCommand.builder()
            .name("Anthropic production")
            .strategy(CredentialStrategy.BEARER)
            .values(Map.of("token", "sk-initial"));
        return create(customizer.apply(defaults).build());
    }

    private Credential create(NewCredentialCommand command) {
        return createCredentialUseCase
            .execute(new CreateCredentialUseCase.Input(UUID.randomUUID().toString(), owner(), command))
            .credential();
    }

    private static BaseUserEntity user(String id, String organizationId) {
        return BaseUserEntity.builder().id(id).organizationId(organizationId).firstname(id).build();
    }
}
