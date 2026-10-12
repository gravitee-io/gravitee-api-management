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
package io.gravitee.apim.rest.api.mcp.security.oauth;

import jakarta.annotation.Nullable;

/**
 * The organization a request to the MCP mount is for, known before any token is read.
 *
 * @param explicit whether the URL named the organization, by its path or its host; a token must then belong to it
 * @param resource the URL of this MCP endpoint as an OAuth resource, which a token's audience must contain; null when
 *        the installation configures no API URL, which leaves OAuth off
 */
public record McpMount(String organizationId, boolean explicit, @Nullable String resource) {
    public static final String METADATA_PATH = "/.well-known/oauth-protected-resource";

    public String metadataUrl() {
        return resource + METADATA_PATH;
    }
}
