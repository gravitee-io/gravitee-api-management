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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.common.utils.UUID;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.util.Collection;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GetDeployedPortHelper {

    public static final String HTTP_PORTS_CONFIG_KEY = "http_ports";
    public static final String TCP_PORTS_CONFIG_KEY = "tcp_ports";

    public static String buildAnDedicatedAddr() {
        return UUID.random().toString();
    }

    public static Handler<Message<Object>> handler(Map<String, Integer> populated) {
        return msg ->
            new JsonObject(msg.body().toString())
                .forEach(e -> {
                    if (e.getValue() instanceof Number n) {
                        populated.put(e.getKey(), n.intValue());
                    }
                });
    }

    public static String serialize(Collection<Map.Entry<String, Integer>> ports) {
        var json = new JsonObject();
        ports.forEach(port -> json.put(port.getKey(), port.getValue()));
        return json.toString();
    }
}
