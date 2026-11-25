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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.TERMS;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addContainsIfAny;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addTerm;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addTerms;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addTermsIfAny;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addTimeRange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses;
import io.gravitee.repository.elasticsearch.v4.shared.AuthzEntityRefClauses;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import java.util.Set;

/**
 * @author GraviteeSource Team
 */
public final class SearchAuthzDecisionLogsQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String NESTED = "nested";
    private static final String IGNORE_UNMAPPED = "ignore_unmapped";
    private static final String PATH = "path";
    private static final String PREFIX = "prefix";
    private static final String SHOULD = "should";
    private static final String MINIMUM_SHOULD_MATCH = "minimum_should_match";
    private static final String RULE_INDEX_SEPARATOR = "#";

    private SearchAuthzDecisionLogsQueryAdapter() {}

    private static void addEntityRefsIfAny(
        ArrayNode filters,
        AuthzEntityRefClauses entityRefs,
        String typeField,
        String idField,
        Set<String> references
    ) {
        if (references == null || references.isEmpty()) {
            return;
        }
        filters.addRawValue(new RawValue(entityRefs.matching(typeField, idField, references).encode()));
    }

    /**
     * Nested objects are indexed as separate Lucene documents, so a plain terms clause on the
     * sub-field matches nothing at all — the wrapper is what makes the field reachable.
     *
     * <p>{@code ignore_unmapped} keeps an index matching the pattern without that nested mapping from failing
     * the whole query silently, since the response model carries no {@code _shards}.
     */
    private static void addMatchedPoliciesIfAny(ArrayNode filters, String path, String field, Set<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        var qualifiedField = path + "." + field;
        ArrayNode exactNames = MAPPER.createArrayNode();
        names.forEach(exactNames::add);
        ArrayNode should = MAPPER.createArrayNode();
        should.add(MAPPER.createObjectNode().set(TERMS, MAPPER.createObjectNode().set(qualifiedField, exactNames)));
        names.forEach(name -> should.add(ruleIndexPrefixClause(qualifiedField, name)));
        ObjectNode bool = MAPPER.createObjectNode();
        bool.set(SHOULD, should);
        bool.put(MINIMUM_SHOULD_MATCH, 1);
        ObjectNode nested = MAPPER.createObjectNode();
        nested.put(PATH, path);
        nested.put(IGNORE_UNMAPPED, true);
        nested.set(QUERY, MAPPER.createObjectNode().set(BOOL, bool));
        filters.add(MAPPER.createObjectNode().set(NESTED, nested));
    }

    private static ObjectNode ruleIndexPrefixClause(String qualifiedField, String name) {
        ObjectNode prefix = MAPPER.createObjectNode();
        prefix.put(qualifiedField, name + RULE_INDEX_SEPARATOR);
        return MAPPER.createObjectNode().set(PREFIX, prefix);
    }

    public static String adapt(QueryContext queryContext, AuthzDecisionLogQuery query) {
        ArrayNode filters = DecisionQueryClauses.scopedFilters(queryContext, AuthzDecisionLogFields.AUTHZ);
        addTerm(filters, AuthzDecisionLogFields.PHASE, AuthzDecisionLogFields.RESOLVED);
        addTerms(filters, AuthzDecisionLogFields.API_ID, query.getApiIds());

        // Keyword fields: exact term matching is the whole translation, once a subject or resource is split into type and id.
        addTermsIfAny(filters, AuthzDecisionLogFields.DECISION, query.getDecisions());
        var entityRefs = new AuthzEntityRefClauses();
        addEntityRefsIfAny(
            filters,
            entityRefs,
            AuthzDecisionLogFields.SUBJECT_TYPE,
            AuthzDecisionLogFields.SUBJECT_ID,
            query.getSubjectIds()
        );
        addTermsIfAny(filters, AuthzDecisionLogFields.ACTION, query.getActions());
        addEntityRefsIfAny(
            filters,
            entityRefs,
            AuthzDecisionLogFields.RESOURCE_TYPE,
            AuthzDecisionLogFields.RESOURCE_ID,
            query.getResourceIds()
        );
        addTermsIfAny(filters, AuthzDecisionLogFields.CALLER, query.getCallers());
        addTermsIfAny(filters, AuthzDecisionLogFields.STATUS, query.getStatuses());
        addTermsIfAny(filters, AuthzDecisionLogFields.INDETERMINATE_CAUSE, query.getIndeterminateCauses());
        addTermsIfAny(filters, AuthzDecisionLogFields.ERROR_TYPE, query.getErrorTypes());
        addTermsIfAny(filters, AuthzDecisionLogFields.TARGET_PDP_ID, query.getTargetPdpIds());
        addTermsIfAny(filters, AuthzDecisionLogFields.POLICY_GENERATION, query.getPolicyGenerations());
        addTermsIfAny(filters, AuthzDecisionLogFields.REQUEST_ID, query.getRequestIds());
        addMatchedPoliciesIfAny(
            filters,
            AuthzDecisionLogFields.MATCHED_RULES,
            AuthzDecisionLogFields.MATCHED_RULE_NAME,
            query.getMatchedPolicyNames()
        );
        // Reasons are sentences: the user filters on a fragment, never the whole string.
        addContainsIfAny(filters, AuthzDecisionLogFields.REASONS, query.getReasonContains());
        addTimeRange(filters, query.getFrom(), query.getTo());

        return DecisionQueryClauses.newestFirstPage(filters, query.getPage(), query.getSize());
    }
}
