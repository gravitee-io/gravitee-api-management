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
package io.gravitee.repository.elasticsearch.v4.analytics.engine;

/**
 * The Elasticsearch fields the LLM proxy reports, named once.
 *
 * <p>These strings are a contract with the gateway: the proxy writes them, and the query side reads them
 * from two places that do very different jobs — {@code HTTPFieldResolver}, which maps a metric, facet or
 * filter to the field answering it, and the aggregation builders, which need the raw field to build a shape.
 * Spelling them out in both meant a rename had to find every site, and a typo failed as an empty result
 * rather than a compile error.
 *
 * <p>Deliberately not derived from the resolver: that maps <i>enum constants</i> to fields, so a builder
 * asking it for a field would have to name a metric it is not computing.
 *
 * @author GraviteeSource Team
 */
public final class LlmProxyFields {

    private static final String PREFIX = "additional-metrics.";

    /** The conversation a request belongs to. */
    public static final String CONVERSATION_ID = PREFIX + "keyword_llm-proxy_conversation-id";

    /** The turn within it — one thing the user asked, however many calls answering it took. */
    public static final String TURN_ID = PREFIX + "keyword_llm-proxy_turn-id";

    /** Which call of that turn a request is, from 1. Absent on a side call. */
    public static final String CALL_NUMBER = PREFIX + "long_llm-proxy_call-number";

    /** Whether the user asked for an exchange, or the client made it for the conversation itself. */
    public static final String REQUEST_KIND = PREFIX + "keyword_llm-proxy_request-kind";

    /**
     * The tools whose results a request hands back — the batch the agent just ran.
     *
     * <p>Multi-valued, and the only field here that is. A terms aggregation over it therefore counts
     * <b>documents, not values</b>: a bucket reads "exchanges in which this tool ran", never "tool calls",
     * and the buckets sum to more than the number of requests. Nothing recorded can correct that, so
     * anything surfacing this number says "exchanges".
     */
    public static final String TOOL_NAMES = PREFIX + "keyword_llm-proxy_tool-names";

    public static final String MODEL = PREFIX + "keyword_llm-proxy_model";
    public static final String PROVIDER = PREFIX + "keyword_llm-proxy_provider";

    public static final String TOKENS_SENT = PREFIX + "long_llm-proxy_tokens-sent";
    public static final String TOKENS_RECEIVED = PREFIX + "long_llm-proxy_tokens-received";
    public static final String TOKENS_REASONING = PREFIX + "long_llm-proxy_tokens-reasoning";

    public static final String SENT_COST = PREFIX + "double_llm-proxy_sent-cost";
    public static final String RECEIVED_COST = PREFIX + "double_llm-proxy_received-cost";
    public static final String REASONING_COST = PREFIX + "double_llm-proxy_reasoning-cost";

    private LlmProxyFields() {}
}
