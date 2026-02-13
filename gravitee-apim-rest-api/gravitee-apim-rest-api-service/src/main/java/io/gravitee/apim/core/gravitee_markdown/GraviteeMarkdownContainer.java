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
package io.gravitee.apim.core.gravitee_markdown;

/**
 * Contract for domain entities that contain Gravitee Markdown (GMD) content.
 *
 * <p>GMD is a custom markdown format with embedded web components (gmd-input, gmd-card, etc.)
 * used across multiple domains in the platform (portal pages, subscription forms, etc.).</p>
 *
 * <p>This interface enables dependency inversion: the {@code gravitee_markdown} domain
 * provides validation logic that operates on this contract, while remaining agnostic
 * to specific domain implementations like {@code SubscriptionForm} or {@code GraviteeMarkdownPageContent}.</p>
 *
 * @author Gravitee.io Team
 */
@FunctionalInterface
public interface GraviteeMarkdownContainer {
    /**
     * Returns the GMD content held by this container.
     *
     * @return the GMD content string, may be null
     */
    String getGmdContent();
}
