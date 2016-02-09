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
package io.gravitee.gateway.standalone.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class SocketUtils {

    private static List<Integer> BIND_PORTS = new ArrayList<>();

    /**
     * Returns a free port number on localhost, or -1 if unable to find a free port.
     *
     * @return a free port number on localhost, or -1 if unable to find a free port
     */
    public static int generateFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            int port = socket.getLocalPort();
            BIND_PORTS.add(port);
            return port;
        } catch (IOException e) {
        }
        return -1;
    }

    public static void clearUsedPorts() {
        BIND_PORTS.clear();
    }

    public static List<Integer> getBindPorts() {
        return new ArrayList<>(BIND_PORTS);
    }
}
