package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContainer;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GraviteePortalPageContentValidatorService implements PortalPageContentValidator {

    private final GraviteeMarkdownValidator graviteeMarkdownValidator;

    @Override
    public boolean appliesTo(PortalPageContent existingContent) {
        return existingContent instanceof GraviteeMarkdownContainer;
    }

    @Override
    public void validate(UpdatePortalPageContent updateContent) {
        graviteeMarkdownValidator.validateNotEmpty(updateContent.getContent());
    }
}
