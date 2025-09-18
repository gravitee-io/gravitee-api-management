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
package io.gravitee.apim.core.api.model.utils;

public final class MigrationWarnings {

    private MigrationWarnings() {}

    public static final String API_NOT_V2_DEFINITION =
        "Unable to migrate an API which is not a v2 definition. Please ensure the API is of type V2 before migration";
    public static final String API_OUT_OF_SYNC =
        "This API is out of sync. Please deploy your API before migration to ensure a smooth transition";
    public static final String V4_EMULATION_ENGINE_REQUIRED =
        "Only APIs with the “V4 Emulation Engine” enabled can be deployed. Please enable the V4 Emulation Engine before migrating your API";
    public static final String V2_API_NOT_NULL = "V2 API should not be null, for migrating to V4";
    public static final String DOC_WITH_TRANSLATIONS =
        "Unable to migrate the API as it has document %s, with translations. Please ensure your API does not have documents with translations";
    public static final String DOC_WITH_ACCESS_CONTROL =
        "Unable to migrate the API as it has document %s, with Access Control. Please ensure your API does not have documents with Access Control";

    public static final String DOC_WITH_ATTACHED_RESOURCES =
        "Unable to migrate the API as it has document %s, with Attached Resources. Please ensure your API does not have documents with attached resources";

    public static final String ENDPOINT_GROUP_PARSE_ERROR =
        "Unable to migrate the API, as an error occurred while parsing the configuration for endpoint group, %s. Please contact support and attach your API definition with the support ticket for debugging";

    public static final String HEALTHCHECK_ENDPOINT_PARSE_ERROR =
        "Unable to migrate the API, as an error occurred while parsing the healthcheck configuration for endpoint, %s. Please contact support and attach your API definition with the support ticket for debugging";

    public static final String ENDPOINT_PARSE_ERROR =
        "Unable to migrate the API, as an error occurred while parsing the endpoint configuration. Please contact support and attach your API definition with the support ticket for debugging";

    public static final String SERVICE_NOT_SUPPORTED = "Unable to migrate the API as the service, %s, is not supported by the API";

    public static final String HEALTHCHECK_ASSERTION =
        "Unable to migrate the API as it has Health Check for %s, with more than one assertion. Please ensure the Health Check configuration has only one assertion";

    public static final String HEALTHCHECK_GENERAL_PARSE_ERROR =
        "Unable to migrate the API, as an error occurred while parsing the healthcheck configuration. Please contact support and attach your API definition with the support ticket for debugging";

    public static final String HEALTHCHECK_STEPS =
        "Unable to migrate the API as it has Health Check for %s with more than one step. Please ensure the Health Check configuration has only one step";

    public static final String DYNAMIC_PROPERTY_PARSE_ERROR =
        "Unable to migrate the API as an error occurred while parsing the Dynamic Property configuration. Please contact support and attach your API definition with the support ticket for debugging";

    public static final String SERVICE_DISCOVERY_NOT_SUPPORTED =
        "Unable to migrate the Service Discovery Configuration, as Service discovery provider %s is not supported for migration. Only consul-service-discovery is supported";

    public static final String SERVICE_DISCOVERY_LIMITATION =
        "Service discovery configuration can be migrated, but the configuration page will not be available in the V4 API";

    public static final String DYNAMIC_PROPERTY_HTTP_ONLY =
        "Unable to migrate the API as Dynamic properties configuration only supports HTTP provider";

    public static final String POLICY_NOT_COMPATIBLE = "Unable to migrate the API as Policy %s is not compatible with V4 APIs";

    public static final String NON_GRAVITEE_POLICY =
        "Unable to migrate the API as Policy %s is not a Gravitee policy. Please ensure your API uses Gravitee policies compatible with V4 API before migrating to V4";

    public static final String GROOVY_MISSING_SCRIPTS =
        "Unable to migrate the API as scripts are missing in the groovy policy configuration. Please ensure there are no missing scripts in the groovy policy configuration";

    public static final String GROOVY_MULTIPLE_SCRIPTS =
        "Unable to migrate the API as multiple groovy scripts are found in groovy policy configuration (non 'content' scripts are ignored if a 'content' script is present). Please ensure there is only one script in the groovy policy configuration";

    public static final String GROOVY_PARSE_ERROR =
        "Unable to migrate the API as an error occurred while parsing the groovy policy configuration. Please contact support and attach your API definition with the support ticket for debugging";
}
