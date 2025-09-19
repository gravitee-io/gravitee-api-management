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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.MembershipFixtures;
import fixtures.core.model.MetadataFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiIndexerDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "api-id";
    private static final String USER_ID = "user-id";
    private static final PrimaryOwnerEntity PRIMARY_OWNER = new PrimaryOwnerEntity(
        USER_ID,
        "jane.doe@gravitee.io",
        "Jane Doe",
        PrimaryOwnerEntity.Type.USER
    );

    ApiCategoryQueryServiceInMemory apiCategoryQueryService = new ApiCategoryQueryServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    ApiIndexerDomainService service;

    @BeforeEach
    void setUp() {
        service = new ApiIndexerDomainService(
            new ApiMetadataDecoderDomainService(apiMetadataQueryService, new FreemarkerTemplateProcessor()),
            new ApiPrimaryOwnerDomainService(
                null,
                new GroupQueryServiceInMemory(),
                null,
                membershipQueryService,
                roleQueryService,
                userCrudService
            ),
            apiCategoryQueryService,
            null
        );

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
    }

    @Nested
    class ToIndexableApi {

        @Test
        void should_build_indexable_api_from_api_provided() {
            Api apiToIndex = givenApi(ApiFixtures.aProxyApiV4());
            var result = service.toIndexableApi(apiToIndex, PRIMARY_OWNER);

            assertThat(result).extracting(IndexableApi::getApi).isSameAs(apiToIndex);
        }

        @Test
        void should_build_indexable_api_with_decoded_metadata() {
            Api apiToIndex = givenApi(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build());
            givenApiMetadata(
                MetadataFixtures.anApiMetadata(API_ID, "api-name", "${api.name}", "my-value", "api-name", Metadata.MetadataFormat.STRING),
                MetadataFixtures.anApiMetadata(
                    API_ID,
                    "support",
                    "${(api.primaryOwner.email)!''}",
                    "my-value",
                    "email-support",
                    Metadata.MetadataFormat.MAIL
                )
            );

            var result = service.toIndexableApi(apiToIndex, PRIMARY_OWNER);

            assertThat(result)
                .extracting(IndexableApi::getDecodedMetadata)
                .isEqualTo(Map.ofEntries(Map.entry("api-name", apiToIndex.getName()), Map.entry("support", PRIMARY_OWNER.email())));
        }

        @Test
        void should_build_indexable_api_with_categories() {
            Api apiToIndex = givenApi(
                ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).categories(Set.of("category1", "category2")).build()
            );
            givenApiCategories(
                Category.builder().id("category1").key("category-key-1").build(),
                Category.builder().id("category2").key("category-key-2").build()
            );

            var result = service.toIndexableApi(apiToIndex, PRIMARY_OWNER);

            assertThat(result).extracting(IndexableApi::getCategoryKeys).isEqualTo(Set.of("category-key-1", "category-key-2"));
        }

        @Test
        void should_fetch_primary_owner_if_not_defined() {
            Api apiToIndex = givenApi(
                ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).categories(Set.of("category1", "category2")).build()
            );
            givenExistingUsers(
                BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build()
            );
            givenExistingMembership(MembershipFixtures.anApiPrimaryOwnerUserMembership(API_ID, USER_ID, ORGANIZATION_ID));

            var result = service.toIndexableApi(new Indexer.IndexationContext(ORGANIZATION_ID, ENVIRONMENT_ID), apiToIndex);

            assertThat(result).extracting(IndexableApi::getPrimaryOwner).isEqualTo(PRIMARY_OWNER);
        }

        @Test
        void should_build_indexable_api_without_primary_owner_when_fetching_fails() {
            Api apiToIndex = givenApi(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build());
            givenApiMetadata(
                MetadataFixtures.anApiMetadata(API_ID, "api-name", "${api.name}", "my-value", "api-name", Metadata.MetadataFormat.STRING),
                MetadataFixtures.anApiMetadata(
                    API_ID,
                    "support",
                    "${(api.primaryOwner.email)!''}",
                    "my-value",
                    "email-support",
                    Metadata.MetadataFormat.MAIL
                )
            );

            var result = service.toIndexableApi(new Indexer.IndexationContext(ORGANIZATION_ID, ENVIRONMENT_ID), apiToIndex);

            assertThat(result).isEqualTo(
                new IndexableApi(
                    apiToIndex,
                    null,
                    Map.ofEntries(Map.entry("api-name", apiToIndex.getName()), Map.entry("support", "")),
                    Set.of()
                )
            );
        }
    }

    private Api givenApi(Api api) {
        return api;
    }

    private void givenApiMetadata(ApiMetadata... metadata) {
        apiMetadataQueryService.initWithApiMetadata(Arrays.asList(metadata));
    }

    private void givenApiCategories(Category... categories) {
        apiCategoryQueryService.initWith(Arrays.asList(categories));
    }

    private void givenExistingUsers(BaseUserEntity... users) {
        userCrudService.initWith(Arrays.asList(users));
    }

    private void givenExistingMembership(Membership... memberships) {
        membershipQueryService.initWith(Arrays.asList(memberships));
    }
}
