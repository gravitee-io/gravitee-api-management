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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform.SubscriptionFormGlobalDefaultMongoUpgrader.GLOBAL_DEFAULT_FORM_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SubscriptionFormGlobalDefaultMongoUpgraderTest extends AbstractManagementRepositoryTest {

    private static final String FORMS_COLLECTION = "subscription_forms";
    private static final String APIS_COLLECTION = "apis";

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private SubscriptionFormGlobalDefaultMongoUpgrader upgrader;
    private String formsCollection;
    private String apisCollection;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collections directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new SubscriptionFormGlobalDefaultMongoUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        var prefix = environment.getProperty("management.mongodb.prefix", "");
        formsCollection = prefix + FORMS_COLLECTION;
        apisCollection = prefix + APIS_COLLECTION;
        clearCollections();
    }

    @AfterEach
    public void cleanUp() {
        clearCollections();
    }

    // deleteMany, not drop: drop() also removes the indexes created once at context bootstrap, which the
    // notablescan-guarded queries then have no plan to use.
    private void clearCollections() {
        mongoTemplate.getCollection(formsCollection).deleteMany(new Document());
        mongoTemplate.getCollection(apisCollection).deleteMany(new Document());
    }

    private void givenAForm(String formId, String environmentId, String name, boolean enabled, List<String> apiIds) {
        mongoTemplate
            .getCollection(formsCollection)
            .insertOne(
                new Document("_id", formId)
                    .append("environmentId", environmentId)
                    .append("name", name)
                    .append("normalizedName", name.toLowerCase())
                    .append("enabled", enabled)
                    .append("apiIds", apiIds)
            );
    }

    private void givenAnApi(String apiId, String environmentId) {
        mongoTemplate.getCollection(apisCollection).insertOne(new Document("_id", apiId).append("environmentId", environmentId));
    }

    private List<String> apiIdsOf(String formId) {
        return mongoTemplate.getCollection(formsCollection).find(new Document("_id", formId)).first().getList("apiIds", String.class);
    }

    @Test
    public void upgrade_should_dedicate_an_enabled_global_default_form_to_every_api_of_its_environment() throws Exception {
        givenAForm("form-global", "env-1", GLOBAL_DEFAULT_FORM_NAME, true, List.of());
        givenAnApi("api-1", "env-1");
        givenAnApi("api-2", "env-1");
        givenAnApi("api-other-env", "env-2");

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        assertThat(apiIdsOf("form-global")).containsExactlyInAnyOrder("api-1", "api-2");
    }

    @Test
    public void upgrade_should_leave_a_disabled_global_default_form_mapped_to_no_api() throws Exception {
        givenAForm("form-global", "env-1", GLOBAL_DEFAULT_FORM_NAME, false, List.of());
        givenAnApi("api-1", "env-1");

        upgrader.upgrade();

        assertThat(apiIdsOf("form-global")).isEmpty();
    }

    @Test
    public void upgrade_should_not_map_an_api_already_dedicated_to_another_form() throws Exception {
        givenAForm("form-global", "env-1", GLOBAL_DEFAULT_FORM_NAME, true, List.of());
        givenAForm("form-partner", "env-1", "Partner", true, List.of("api-1"));
        givenAnApi("api-1", "env-1");
        givenAnApi("api-2", "env-1");

        upgrader.upgrade();

        assertThat(apiIdsOf("form-global")).containsExactly("api-2");
    }

    @Test
    public void upgrade_should_leave_a_form_already_mapped_to_apis_untouched() throws Exception {
        givenAForm("form-global", "env-1", GLOBAL_DEFAULT_FORM_NAME, true, List.of("api-1"));
        givenAnApi("api-1", "env-1");
        givenAnApi("api-2", "env-1");

        upgrader.upgrade();

        assertThat(apiIdsOf("form-global")).containsExactly("api-1");
    }

    @Test
    public void upgrade_should_leave_any_other_form_untouched() throws Exception {
        givenAForm("form-partner", "env-1", "Partner", true, List.of());
        givenAnApi("api-1", "env-1");

        upgrader.upgrade();

        assertThat(apiIdsOf("form-partner")).isEmpty();
    }
}
