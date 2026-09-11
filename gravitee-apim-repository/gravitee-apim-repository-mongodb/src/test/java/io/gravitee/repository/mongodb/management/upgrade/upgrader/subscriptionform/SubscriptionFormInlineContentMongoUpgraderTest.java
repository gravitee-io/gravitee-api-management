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

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform.SubscriptionFormInlineContentMongoUpgrader.CONTENT;
import static io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform.SubscriptionFormInlineContentMongoUpgrader.GMD_CONTENT;
import static io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform.SubscriptionFormInlineContentMongoUpgrader.PORTAL_PAGE_CONTENT_ID;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SubscriptionFormInlineContentMongoUpgraderTest extends AbstractManagementRepositoryTest {

    private static final String FORMS_COLLECTION = "subscription_forms";
    private static final String PAGE_CONTENTS_COLLECTION = "portal_page_contents";
    private static final String DEFINITION = "<gmd-card><gmd-input name=\"company\" label=\"Company\"/></gmd-card>";

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private SubscriptionFormInlineContentMongoUpgrader upgrader;
    private String formsCollection;
    private String pageContentsCollection;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collections directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new SubscriptionFormInlineContentMongoUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        var prefix = environment.getProperty("management.mongodb.prefix", "");
        formsCollection = prefix + FORMS_COLLECTION;
        pageContentsCollection = prefix + PAGE_CONTENTS_COLLECTION;
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
        mongoTemplate.getCollection(pageContentsCollection).deleteMany(new Document());
    }

    private void givenAMigratedForm(String formId, String contentId) {
        givenAFormPointingAt(formId, contentId);
        mongoTemplate.getCollection(pageContentsCollection).insertOne(new Document("_id", contentId).append(CONTENT, DEFINITION));
    }

    private void givenAFormPointingAt(String formId, String contentId) {
        mongoTemplate
            .getCollection(formsCollection)
            .insertOne(new Document("_id", formId).append(GMD_CONTENT, null).append(PORTAL_PAGE_CONTENT_ID, contentId));
    }

    private Document findForm(String id) {
        return mongoTemplate.getCollection(formsCollection).find(new Document("_id", id)).first();
    }

    private Document findPageContent(String id) {
        return mongoTemplate.getCollection(pageContentsCollection).find(new Document("_id", id)).first();
    }

    @Test
    public void upgrade_should_bring_the_definition_back_into_the_form() throws Exception {
        givenAMigratedForm("form-migrated", "content-migrated");

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        var form = findForm("form-migrated");
        assertThat(form.getString(GMD_CONTENT)).isEqualTo(DEFINITION);
        assertThat(form.containsKey(PORTAL_PAGE_CONTENT_ID)).isFalse();
    }

    @Test
    public void upgrade_should_delete_the_page_content_it_restored() throws Exception {
        givenAMigratedForm("form-migrated", "content-migrated");

        upgrader.upgrade();

        assertThat(findPageContent("content-migrated")).isNull();
    }

    @Test
    public void upgrade_should_leave_a_form_that_was_never_migrated_untouched() throws Exception {
        mongoTemplate.getCollection(formsCollection).insertOne(new Document("_id", "form-inline").append(GMD_CONTENT, DEFINITION));

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        assertThat(findForm("form-inline").getString(GMD_CONTENT)).isEqualTo(DEFINITION);
    }

    @Test
    public void upgrade_should_leave_a_page_content_no_form_points_at_alone() throws Exception {
        givenAMigratedForm("form-migrated", "content-migrated");
        mongoTemplate.getCollection(pageContentsCollection).insertOne(new Document("_id", "content-of-a-page").append(CONTENT, "# A page"));

        upgrader.upgrade();

        assertThat(findPageContent("content-of-a-page")).isNotNull();
    }

    @Test
    public void upgrade_should_delete_a_form_whose_page_content_is_gone() throws Exception {
        givenAFormPointingAt("form-dangling", "content-gone");
        givenAMigratedForm("form-migrated", "content-migrated");

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        assertThat(findForm("form-dangling")).isNull();
        assertThat(findForm("form-migrated").getString(GMD_CONTENT)).isEqualTo(DEFINITION);
    }

    @Test
    public void upgrade_should_delete_a_form_whose_page_content_holds_no_definition() throws Exception {
        givenAFormPointingAt("form-empty", "content-empty");
        mongoTemplate.getCollection(pageContentsCollection).insertOne(new Document("_id", "content-empty"));

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        assertThat(findForm("form-empty")).isNull();
        assertThat(findPageContent("content-empty")).isNull();
    }
}
