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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.portal_page.exception.PortalPageSpecificationException;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import java.util.function.Predicate;

public class PageExistsSpecification<T> {

    private final Predicate<T> existenceChecker;

    public PageExistsSpecification(Predicate<T> existenceChecker) {
        this.existenceChecker = existenceChecker;
    }

    public boolean satisfies(T locator) {
        return existenceChecker.test(locator);
    }

    public String getErrorMessage() {
        return "Page does not exist";
    }

    public void throwIfNotSatisfied(T locator) {
        if (!satisfies(locator)) {
            throw new PortalPageSpecificationException(getErrorMessage());
        }
    }

    public static PageExistsSpecification<PortalViewContext> byPortalViewContext(Predicate<PortalViewContext> existenceChecker) {
        return new PageExistsSpecification<>(existenceChecker);
    }

    public static PageExistsSpecification<PageId> byPageId(Predicate<PageId> existenceChecker) {
        return new PageExistsSpecification<>(existenceChecker);
    }
}
