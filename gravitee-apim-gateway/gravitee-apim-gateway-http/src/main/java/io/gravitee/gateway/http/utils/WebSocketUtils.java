/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.http.utils;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.http.HttpMethod;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class WebSocketUtils {

    public static boolean isWebSocket(String method, String connectionHeader, String upgradeHeader) {
        boolean isUpgrade = false;
        if (connectionHeader != null) {
            String[] connectionParts = connectionHeader.split(",");
            for (int i = 0; i < connectionParts.length && !isUpgrade; ++i) {
                isUpgrade = HttpHeaderValues.UPGRADE.contentEqualsIgnoreCase(connectionParts[i].trim());
            }
        }
        return HttpMethod.GET.name().equals(method) && isUpgrade && HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader);
    }
}
