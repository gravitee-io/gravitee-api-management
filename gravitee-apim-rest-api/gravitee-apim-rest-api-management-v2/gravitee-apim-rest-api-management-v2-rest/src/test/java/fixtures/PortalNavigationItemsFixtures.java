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
package fixtures;

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.BaseUpdatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApiProduct;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationSubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType;
import io.gravitee.rest.api.management.v2.rest.model.PortalVisibility;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApiProduct;
import java.util.List;
import java.util.UUID;

public class PortalNavigationItemsFixtures {

    static final String MY_FOLDER = "My Folder";
    static final String SUBSCRIPTION_FORM_TITLE = "Subscription Form";

    private PortalNavigationItemsFixtures() {}

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationPage() {
        var title = "My Page";
        var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        var contentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003");
        return new CreatePortalNavigationPage()
            .portalPageContentId(contentId)
            .type(PortalNavigationItemType.PAGE)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(1)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationFolder() {
        var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        return new CreatePortalNavigationFolder()
            .type(PortalNavigationItemType.FOLDER)
            .id(id)
            .title(MY_FOLDER)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(2)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationLink() {
        var title = "My Link";
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var url = "http://example.com";
        return new CreatePortalNavigationLink()
            .url(url)
            .type(PortalNavigationItemType.LINK)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(3)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationApi() {
        var title = "My Link";
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var url = "apiId";
        return new CreatePortalNavigationApi()
            .apiId(url)
            .type(PortalNavigationItemType.API)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(3)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static CreatePortalNavigationApiProduct aCreatePortalNavigationApiProduct() {
        var apiProduct = new CreatePortalNavigationApiProduct();
        apiProduct.setApiProductId(UUID.fromString("00000000-0000-0000-0000-000000000019"));
        apiProduct.setType(PortalNavigationItemType.API_PRODUCT);
        apiProduct.setId(UUID.fromString("00000000-0000-0000-0000-000000000018"));
        apiProduct.setTitle("My API Product");
        apiProduct.setArea(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
        apiProduct.setOrder(4);
        apiProduct.setParentId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        apiProduct.setVisibility(PortalVisibility.PUBLIC);
        return apiProduct;
    }

    public static CreatePortalNavigationApiProduct aCreatePortalNavigationApiProduct(List<UUID> categoryIds) {
        var apiProduct = aCreatePortalNavigationApiProduct();
        apiProduct.setCategoryIds(categoryIds);
        return apiProduct;
    }

    public static BaseUpdatePortalNavigationItem anUpdatePortalNavigationApi() {
        return new UpdatePortalNavigationApi()
            .type(PortalNavigationItemType.API)
            .title("Updated Api")
            .order(1)
            .published(false)
            .visibility(PortalVisibility.PRIVATE);
    }

    public static UpdatePortalNavigationApiProduct anUpdatePortalNavigationApiProduct() {
        var apiProduct = new UpdatePortalNavigationApiProduct();
        apiProduct.setType(PortalNavigationItemType.API_PRODUCT);
        apiProduct.setTitle("Updated API Product");
        apiProduct.setOrder(1);
        apiProduct.setPublished(false);
        apiProduct.setVisibility(PortalVisibility.PRIVATE);
        return apiProduct;
    }

    public static UpdatePortalNavigationApiProduct anUpdatePortalNavigationApiProduct(List<UUID> categoryIds) {
        var apiProduct = anUpdatePortalNavigationApiProduct();
        apiProduct.setCategoryIds(categoryIds);
        return apiProduct;
    }

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationSubscriptionForm() {
        var title = SUBSCRIPTION_FORM_TITLE;
        var id = UUID.fromString("00000000-0000-0000-0000-000000000021");
        var contentId = UUID.fromString("00000000-0000-0000-0000-000000000020");
        return new CreatePortalNavigationSubscriptionForm()
            .portalPageContentId(contentId)
            .type(PortalNavigationItemType.SUBSCRIPTION_FORM)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static PortalNavigationItem aPortalNavigationSubscriptionForm(String organizationId, String environmentId) {
        return PortalNavigationSubscriptionForm.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000021"))
            .organizationId(organizationId)
            .environmentId(environmentId)
            .title(SUBSCRIPTION_FORM_TITLE)
            .segment(PortalNavigationItem.slugify(SUBSCRIPTION_FORM_TITLE).value())
            .area(PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .portalPageContentId(PortalPageContentId.of("00000000-0000-0000-0000-000000000020"))
            .validationConstraints(SubscriptionFormFieldConstraints.empty())
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .published(false)
            .build();
    }

    public static BaseCreatePortalNavigationItem aPrivateCreatePortalNavigationPage() {
        var title = "My Page";
        var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        var contentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003");
        return new CreatePortalNavigationPage()
            .portalPageContentId(contentId)
            .type(PortalNavigationItemType.PAGE)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(1)
            .parentId(parentId)
            .visibility(PortalVisibility.PRIVATE);
    }

    public static PortalNavigationItem aPortalNavigationPage(String organizationId, String environmentId) {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001"))
            .organizationId(organizationId)
            .environmentId(environmentId)
            .title("My Page")
            .segment(PortalNavigationItem.slugify("My Page").value())
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .portalPageContentId(PortalPageContentId.of("00000000-0000-0000-0000-000000000003"))
            .parentId(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000002"))
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .published(false)
            .build();
    }

    public static PortalNavigationItem aPortalNavigationFolder(String organizationId, String environmentId) {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001"))
            .organizationId(organizationId)
            .environmentId(environmentId)
            .title(MY_FOLDER)
            .segment(PortalNavigationItem.slugify(MY_FOLDER).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(2)
            .parentId(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000002"))
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .published(false)
            .build();
    }

    public static PortalNavigationItem aPortalNavigationLink(String organizationId, String environmentId) {
        return PortalNavigationLink.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001"))
            .organizationId(organizationId)
            .environmentId(environmentId)
            .title("My Link")
            .segment(PortalNavigationItem.slugify("My Link").value())
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .url("http://example.com")
            .parentId(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000002"))
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .published(false)
            .build();
    }

    public static PortalNavigationItem aPortalNavigationApi(String organizationId, String environmentId) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001"))
            .organizationId(organizationId)
            .environmentId(environmentId)
            .title("My Link")
            .segment(PortalNavigationItem.slugify("My Link").value())
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .apiId("apiId")
            .parentId(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000002"))
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .published(false)
            .build();
    }

    public static PortalNavigationItem aPortalNavigationApiProduct(String organizationId, String environmentId) {
        return PortalNavigationApiProduct.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000018"))
            .organizationId(organizationId)
            .environmentId(environmentId)
            .title("My API Product")
            .segment(PortalNavigationItem.slugify("My API Product").value())
            .area(PortalArea.TOP_NAVBAR)
            .order(4)
            .apiProductId("00000000-0000-0000-0000-000000000019")
            .parentId(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000002"))
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .published(false)
            .build();
    }

    public static PortalNavigationItem aPrivatePortalNavigationPage(String organizationId, String environmentId) {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001"))
            .organizationId(organizationId)
            .environmentId(environmentId)
            .title("My Page")
            .segment(PortalNavigationItem.slugify("My Page").value())
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .portalPageContentId(PortalPageContentId.of("00000000-0000-0000-0000-000000000003"))
            .parentId(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000002"))
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE)
            .published(false)
            .build();
    }
}
