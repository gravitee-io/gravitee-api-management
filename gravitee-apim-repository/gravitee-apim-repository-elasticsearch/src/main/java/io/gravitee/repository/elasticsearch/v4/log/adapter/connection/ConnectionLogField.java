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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

/**
 * Centralization of fields used for Connection Logs.
 * <p>
 * Some fields are shared between indexes, i.e. @timestamp.
 * While others have different values depending upon the index.
 * <p>
 * There are currently two indexes possible for queries: <br>
 * - Request -- used by V2 APIs <br>
 * - Metrics -- used by V4 APIs
 *
 */
public class ConnectionLogField {

    // Fields with same name in both indexes: Request + Metrics
    public static final String TIMESTAMP = "@timestamp";
    public static final String STATUS = "status";
    public static final String GATEWAY = "gateway";
    public static final String URI = "uri";

    // Fields with different names in each index
    public static final Field REQUEST_ID = new Field("_id", "request-id");
    public static final Field APPLICATION_ID = new Field("application", "application-id");
    public static final Field API_ID = new Field("api", "api-id");
    public static final Field PLAN_ID = new Field("plan", "plan-id");
    public static final Field CLIENT_IDENTIFIER = new Field("subscription", "client-identifier");
    public static final Field TRANSACTION_ID = new Field("transaction", "transaction-id");
    public static final Field HTTP_METHOD = new Field("method", "http-method");
    public static final Field GATEWAY_RESPONSE_TIME = new Field("response-time", "gateway-response-time-ms");
    public static final Field ENTRYPOINT_ID = new Field(null, "entrypoint-id");
    public static final Field REQUEST_ENDED = new Field(null, "request-ended");

    public record Field(String v2Request, String v4Metrics) {}
}
