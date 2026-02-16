package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;

public interface PortalPageContentValidator {
    boolean appliesTo(PortalPageContent existingContent);

    void validateForUpdate(UpdatePortalPageContent updateContent);
}
