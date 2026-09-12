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
package io.gravitee.rest.api.service.impl.search.lucene.utils;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DEFINITION_VERSION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_INTEGRATION_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_PROVIDER_ORGANIZATION_LOWERCASE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LuceneTransformerUtils {

    public static void appendDefinitionVersion(Document doc, DefinitionVersion definitionVersion) {
        // A legacy row with no stored definition version is a V2 api, and is indexed as one so a V2 filter finds it.
        var indexedDefinitionVersion = Objects.requireNonNullElse(definitionVersion, DefinitionVersion.V2);
        doc.add(new StringField(FIELD_DEFINITION_VERSION, indexedDefinitionVersion.getLabel(), Field.Store.NO));
    }

    public static void appendIntegrationId(Document doc, OriginContext originContext, String apiId) {
        if (originContext instanceof OriginContext.Integration integration) {
            if (integration.integrationId() != null) {
                doc.add(new StringField(FIELD_INTEGRATION_ID, integration.integrationId(), Field.Store.NO));
            } else {
                log.warn(
                    "Api {} has an integration origin context with no integration id; indexing it without an integration id term",
                    apiId
                );
            }
        }
    }

    public static void appendProviderOrganization(Document doc, String organization) {
        if (isBlank(organization)) {
            return;
        }
        doc.add(new StringField(FIELD_PROVIDER_ORGANIZATION_LOWERCASE, organization.toLowerCase(), Field.Store.NO));
    }

    public static String generateApiType(Api api) {
        boolean isTcpApi =
            api.getType() == ApiType.PROXY &&
            Objects.nonNull(api.getApiListeners()) &&
            api
                .getApiListeners()
                .stream()
                .anyMatch(listener -> listener.getType() == ListenerType.TCP);
        return generateApiType(api.getDefinitionVersion(), api.getType(), isTcpApi);
    }

    public static String generateApiType(GenericApiEntity api) {
        if (api instanceof ApiEntity) {
            return generateApiType((ApiEntity) api);
        }

        return api.getDefinitionVersion().name();
    }

    private static String generateApiType(ApiEntity api) {
        boolean isTcpApi =
            api.getType() == ApiType.PROXY &&
            Objects.nonNull(api.getListeners()) &&
            api
                .getListeners()
                .stream()
                .anyMatch(listener -> listener.getType() == ListenerType.TCP);
        return generateApiType(api.getDefinitionVersion(), api.getType(), isTcpApi);
    }

    private static String generateApiType(DefinitionVersion definitionVersion, ApiType apiType, boolean isTcpApi) {
        if (definitionVersion == DefinitionVersion.V4) {
            String type = switch (apiType) {
                case A2A_PROXY -> "A2A_PROXY";
                case AUTHZ -> "AUTHZ";
                case EDGE -> "EDGE";
                case LLM_PROXY -> "LLM_PROXY";
                case MCP_PROXY -> "MCP_PROXY";
                case MESSAGE -> "MESSAGE";
                case NATIVE -> "KAFKA";
                case PROXY -> isTcpApi ? "TCP_PROXY" : "HTTP_PROXY";
            };
            return definitionVersion.name() + "_" + type;
        }
        return definitionVersion.name();
    }
}
