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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import lombok.CustomLog;

/**
 * Navigation trees are shallow by design; this bound only exists so that a corrupted parent chain
 * cannot loop forever. Anything that builds a chain — the repository import — must stay under it,
 * because the walks give up rather than fail when they reach it.
 */
@CustomLog
public final class ValidationDepth {

    public static final int MAX_TREE_DEPTH = 50;

    private ValidationDepth() {}

    /** Past the bound the restriction silently stops being enforced: give up loudly. */
    static boolean exceeded(int depth, PortalNavigationItemId itemId, String restriction) {
        if (depth < MAX_TREE_DEPTH) {
            return false;
        }
        log.warn("Stopped walking the navigation tree at [id={}] after {} levels: {}", itemId.json(), MAX_TREE_DEPTH, restriction);
        return true;
    }
}
