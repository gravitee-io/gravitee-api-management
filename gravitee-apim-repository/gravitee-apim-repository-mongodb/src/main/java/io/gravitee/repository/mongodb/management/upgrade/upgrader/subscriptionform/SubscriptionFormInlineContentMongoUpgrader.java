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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem.PortalNavigationItemDefaultVisibilityMongoUpgrader;
import java.util.ArrayList;
import lombok.CustomLog;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Brings the subscription form definition back into the form document and drops the page content it had
 * been moved to, for the environments that already booted on 4.13.0-SNAPSHOT. Mirrors the JDBC changeset
 * {@code 4.13.0_19_store_subscription_form_definition_inline_again}, down to deleting a form whose
 * definition cannot be recovered so that {@code DefaultSubscriptionFormUpgrader} (order 714) reseeds the
 * environment during the same startup. Forms that were never migrated carry their definition inline
 * already and are left alone.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class SubscriptionFormInlineContentMongoUpgrader extends MongoUpgrader {

    /**
     * Chained on the highest Mongo upgrader rather than on a neighbour of its own collection: this upgrader
     * depends on none of them, and every other chain already sits one step below.
     */
    public static final int SUBSCRIPTION_FORM_INLINE_CONTENT_MONGO_UPGRADER_ORDER =
        PortalNavigationItemDefaultVisibilityMongoUpgrader.PORTAL_NAVIGATION_ITEM_DEFAULT_VISIBILITY_MONGO_UPGRADER_ORDER + 1;

    static final String PORTAL_PAGE_CONTENT_ID = "portalPageContentId";
    static final String GMD_CONTENT = "gmdContent";
    static final String CONTENT = "content";

    private static final Document ID_INDEX = new Document("_id", 1);

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var forms = this.getCollection("subscription_forms");
        var pageContents = this.getCollection("portal_page_contents");

        // The pointer carries no index, and the databases these tests run against forbid collection scans:
        // walking the _id index visits every form without asking for one.
        var migratedForms = forms.find(Filters.ne(PORTAL_PAGE_CONTENT_ID, null)).hint(ID_INDEX);

        var readContentIds = new ArrayList<String>();
        for (Document form : migratedForms) {
            var formId = form.get("_id");
            var contentId = form.getString(PORTAL_PAGE_CONTENT_ID);
            readContentIds.add(contentId);

            // Nothing serializes the upgraders across nodes, so every write is conditioned on the form
            // still being the one this cursor read: a form another node has already restored — pointer
            // gone — matches nothing and is left alone rather than restored twice or, worse, deleted.
            var stillToRestore = Filters.and(Filters.eq("_id", formId), Filters.eq(PORTAL_PAGE_CONTENT_ID, contentId));

            var pageContent = pageContents.find(Filters.eq("_id", contentId)).first();
            var definition = pageContent == null ? null : pageContent.getString(CONTENT);
            if (definition == null) {
                if (forms.deleteOne(stillToRestore).getDeletedCount() > 0) {
                    log.warn(
                        "Subscription form [{}] has no definition left in its page content [{}]: deleted it, the environment gets a default form back",
                        formId,
                        contentId
                    );
                }
                continue;
            }

            forms.updateOne(stillToRestore, Updates.combine(Updates.set(GMD_CONTENT, definition), Updates.unset(PORTAL_PAGE_CONTENT_ID)));
        }

        // Each form owned its page content, so the ones just read have no other reader.
        return readContentIds.isEmpty() || pageContents.deleteMany(Filters.in("_id", readContentIds)).wasAcknowledged();
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_FORM_INLINE_CONTENT_MONGO_UPGRADER_ORDER;
    }
}
