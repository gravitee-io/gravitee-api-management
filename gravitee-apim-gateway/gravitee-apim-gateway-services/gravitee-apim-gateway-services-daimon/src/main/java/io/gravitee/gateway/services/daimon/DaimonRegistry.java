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
package io.gravitee.gateway.services.daimon;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

public class DaimonRegistry {

    private final Map<String, DeviceInfo> devices = new ConcurrentHashMap<>();

    public void register(DeviceInfo device) {
        device.setRegisteredAt(Instant.now());
        device.setLastSeen(Instant.now());
        devices.put(device.getDeviceId(), device);
    }

    public boolean heartbeat(String deviceId, HeartbeatStats stats) {
        DeviceInfo device = devices.get(deviceId);
        if (device == null) {
            return false;
        }
        device.setLastSeen(Instant.now());
        device.setStats(stats);
        return true;
    }

    public Collection<DeviceInfo> allDevices() {
        return devices.values();
    }

    @Data
    public static class DeviceInfo {

        private String deviceId;
        private String hostname;
        private String user;
        private String version;
        private String os;
        private String[] capabilities;
        private Instant registeredAt;
        private Instant lastSeen;
        private HeartbeatStats stats;
    }

    @Data
    public static class HeartbeatStats {

        private int requestsTotal;
        private int requestsBlocked;
        private int tokensTotal;
    }
}
