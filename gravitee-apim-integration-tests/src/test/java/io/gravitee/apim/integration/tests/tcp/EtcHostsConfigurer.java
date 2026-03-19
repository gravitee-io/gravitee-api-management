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
package io.gravitee.apim.integration.tests.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;

/**
 * Class allowing to generate a custom equivalent of "/etc/hosts" file.
 * It creates a temporary file to be use for the test lifetime, and is closed at the end of the test.
 * This file is then used thanks to the JVM option: "-Djdk.net.hosts.file".
 * ⚠️ Ideally, it has to be set as a JVM option and not set at runtime, because Java initializes networks classes really soon during starting.
 */
public class EtcHostsConfigurer implements Closeable {

    private final Path hosts;
    private final Map<String, List<String>> hostsMap = new HashMap<>();

    @SneakyThrows
    public EtcHostsConfigurer() {
        hosts = Files.createTempFile("hosts", "");
    }

    /**
     * Adds a custom alias for a given alias.
     * For example, calling <pre>addHost("localhost", "my-custom-domain")</pre> will generate a line with <pre>localhost    my-custom-domain</pre>
     * Calling this method multiple times for the same host will result in concatenating aliases for the line of the selected host.
     * @param host is the host to attach an alias
     * @param alias is the alias for the mentioned host
     * @return this instance of configurer, to chain calls
     */
    public EtcHostsConfigurer addHost(String host, String alias) {
        // Utiliser tempfile
        hostsMap.compute(host, (key, val) -> {
            if (val == null) {
                val = new ArrayList<>();
            }
            val.add(alias);
            return val;
        });
        return this;
    }

    /**
     * Generates and write the hosts file
     * @return the path of the hosts file as String
     */
    @SneakyThrows
    public String generateHostsFile() {
        StringBuilder sb = new StringBuilder();
        hostsMap.forEach((key, value) -> {
            sb.append(key).append("         ").append(String.join(" ", value)).append("\n");
        });
        Files.write(hosts, sb.toString().getBytes());
        return hosts.toString();
    }

    @Override
    public void close() throws IOException {
        Files.delete(hosts);
    }
}
