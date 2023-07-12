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
package io.gravitee.plugin.entrypoint.websocket;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * WebSocket close status (copied from vertx).
 */
public final class WebSocketCloseStatus {

    public static final WebSocketCloseStatus NORMAL_CLOSURE = new WebSocketCloseStatus(1000, "Bye");

    public static final WebSocketCloseStatus ENDPOINT_UNAVAILABLE = new WebSocketCloseStatus(1001, "Endpoint unavailable");

    public static final WebSocketCloseStatus PROTOCOL_ERROR = new WebSocketCloseStatus(1002, "Protocol error");

    public static final WebSocketCloseStatus INVALID_MESSAGE_TYPE = new WebSocketCloseStatus(1003, "Invalid message type");

    public static final WebSocketCloseStatus INVALID_PAYLOAD_DATA = new WebSocketCloseStatus(1007, "Invalid payload data");

    public static final WebSocketCloseStatus POLICY_VIOLATION = new WebSocketCloseStatus(1008, "Policy violation");

    public static final WebSocketCloseStatus MESSAGE_TOO_BIG = new WebSocketCloseStatus(1009, "Message too big");

    public static final WebSocketCloseStatus MANDATORY_EXTENSION = new WebSocketCloseStatus(1010, "Mandatory extension");

    public static final WebSocketCloseStatus SERVER_ERROR = new WebSocketCloseStatus(1011, "Server error");

    public static final WebSocketCloseStatus SERVICE_RESTART = new WebSocketCloseStatus(1012, "Service Restart");

    public static final WebSocketCloseStatus TRY_AGAIN_LATER = new WebSocketCloseStatus(1013, "Try Again Later");

    public static final WebSocketCloseStatus BAD_GATEWAY = new WebSocketCloseStatus(1014, "Bad Gateway");

    public static final WebSocketCloseStatus EMPTY = new WebSocketCloseStatus(1005, "Empty");

    public static final WebSocketCloseStatus ABNORMAL_CLOSURE = new WebSocketCloseStatus(1006, "Abnormal closure");

    public static final WebSocketCloseStatus TLS_HANDSHAKE_FAILED = new WebSocketCloseStatus(1015, "TLS handshake failed");

    private final int statusCode;
    private final String reasonText;

    private WebSocketCloseStatus(int statusCode, String reasonText) {
        this.statusCode = statusCode;
        this.reasonText = checkNotNull(reasonText, "reasonText");
    }

    public int code() {
        return statusCode;
    }

    public String reasonText() {
        return reasonText;
    }
}
