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
package io.gravitee.repository.elasticsearch.v4.log.adapter.authz;

/**
 * Indexed field names of an {@code AuthzEventMetrics} document, as written by the reporter's
 * FreeMarker templates. Kept in one place so the query and the response adapter cannot drift.
 *
 * @author GraviteeSource Team
 */
final class AuthzDecisionLogFields {

    static final String DOC_TYPE = "doc-type";
    static final String API_ID = "api-id";
    static final String ORG_ID = "org-id";
    static final String ENV_ID = "env-id";
    static final String GW_ID = "gw-id";
    static final String EVENT_ID = "event-id";
    static final String REQUEST_ID = "request-id";
    static final String OPERATION = "operation";
    static final String STATUS = "status";
    static final String CALLER = "caller";
    static final String TARGET_PDP_ID = "target-pdp-id";
    static final String POLICY_GENERATION = "policy-generation";
    static final String DECISION = "decision";
    static final String MATCHED_POLICIES = "matched-policies";
    static final String MATCHED_POLICY_NAME = "name";
    static final String REASONS = "reasons";
    static final String SUBJECT_TYPE = "subject-type";
    static final String SUBJECT_ID = "subject-id";
    static final String ACTION = "action";
    static final String RESOURCE_TYPE = "resource-type";
    static final String RESOURCE_ID = "resource-id";
    static final String BATCH_ID = "batch-id";
    static final String BATCH_INDEX = "batch-index";
    static final String BATCH_SIZE = "batch-size";
    static final String SEARCH_TYPE = "search-type";
    static final String RESULT_COUNT = "result-count";
    static final String DURATION_NANOS = "duration-nanos";

    private AuthzDecisionLogFields() {}
}
