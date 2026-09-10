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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation;

import io.gravitee.repository.elasticsearch.v4.analytics.engine.LlmProxyFields;
import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * The conversation vocabulary of the LLM proxy, expressed as aggregations.
 *
 * <p>Three words, and the shapes that count them. A <b>conversation</b> is one continuous exchange; a
 * <b>turn</b> is one thing the user asked; a <b>call</b> is one request to the model, of which a turn using
 * tools takes several. Each is a distinct field the proxy reports, and knowing which field answers which
 * question is what this class holds — so the query adapter can ask for "turns per conversation" without
 * knowing what a turn is stored as.
 *
 * <p>The shapes themselves are generic: {@link CardinalityBuilder} counts distinct values,
 * {@link PerDistinctBuilder} divides one aggregation by the distinct count of a field, and the fields are
 * named once in {@link LlmProxyFields}. What is LLM-specific, and what lives here, is the pairing: which
 * field goes on which side of which ratio.
 *
 * @author GraviteeSource Team
 */
public class LLMConversationBuilder {

    private final CardinalityBuilder conversations = new CardinalityBuilder(LlmProxyFields.CONVERSATION_ID);
    private final CardinalityBuilder turns = new CardinalityBuilder(LlmProxyFields.TURN_ID);
    private final PerDistinctBuilder perConversation = new PerDistinctBuilder(LlmProxyFields.CONVERSATION_ID);
    private final PerDistinctBuilder perTurn = new PerDistinctBuilder(LlmProxyFields.TURN_ID);
    private final LLMTotalCostBuilder cost = new LLMTotalCostBuilder();

    /** How many conversations the requests belonged to. */
    public Map<String, JsonObject> buildConversations(String aggName) {
        return conversations.build(aggName);
    }

    /** How many turns were taken across them. */
    public Map<String, JsonObject> buildTurns(String aggName) {
        return turns.build(aggName);
    }

    /** How much each person asked: turns divided by the conversations they were spread across. */
    public Map<String, JsonObject> buildTurnsPerConversation(String aggName) {
        return perConversation.build(aggName, turnCount());
    }

    /** How hard each question was to answer: model calls divided by the turns that asked for them. */
    public Map<String, JsonObject> buildCallsPerTurn(String aggName) {
        return perTurn.build(aggName, callCount());
    }

    /** What one full exchange cost, side calls included — they are the conversation's cost too. */
    public Map<String, JsonObject> buildCostPerConversation(String aggName) {
        return perConversation.build(aggName, cost.sum());
    }

    /** A turn is one thing the user asked, however many calls it took to answer. */
    private JsonObject turnCount() {
        return new JsonObject().put("cardinality", new JsonObject().put("field", LlmProxyFields.TURN_ID));
    }

    /**
     * Calls, counted over the turn field rather than over every document.
     *
     * <p>A client fires calls for a conversation that nobody asked for — a suggestion, a recap, a classifier
     * run — and those carry no turn. They belong to the conversation and to its cost, but counting them here
     * would divide traffic nobody requested by the turns somebody did.
     */
    private JsonObject callCount() {
        return new JsonObject().put("value_count", new JsonObject().put("field", LlmProxyFields.TURN_ID));
    }
}
