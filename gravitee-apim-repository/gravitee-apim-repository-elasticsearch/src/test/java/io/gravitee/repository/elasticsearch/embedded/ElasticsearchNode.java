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
package io.gravitee.repository.elasticsearch.embedded;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

/**
 * Elasticsearch server for the test.
 *
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 */
public class ElasticsearchNode {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ElasticsearchNode.class);

    /**
     * ES node.
     */
    private Node node;

    private int httpPort;

    /**
     * Start ES.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @PostConstruct
    private void init() throws Exception {
        this.start();
        Thread.sleep(2000);
    }

    /**
     * Start ES node.
     */
    private void start() throws Exception {
        this.httpPort = generateFreePort();

        final Settings settings = Settings
            .builder()
            .put("cluster.name", "gravitee_test")
            .put("node.name", "test")
            .put("http.port", httpPort)
            .put("http.type", "netty4")
            //.put("transport.type", "local")
            .put("path.data", "target/data_gravitee_" + httpPort)
            .put("path.home", "target/data_gravitee_" + httpPort)
            .build();

        this.node = new PluginConfigurableNode(settings, Collections.singletonList(Netty4Plugin.class)).start();

        logger.info("Elasticsearch server for test started");
    }

    public int getHttpPort() {
        return httpPort;
    }

    public Node getNode() {
        return node;
    }

    /**
     * Stop ES
     */
    @PreDestroy
    private void shutdown() throws Exception {
        // remove all index
        this.node.client().admin().indices().delete(new DeleteIndexRequest("_all")).actionGet();

        this.node.close();

        File dataDir = Paths.get(node.settings().get("path.data")).toFile();
        FileSystemUtils.deleteRecursively(dataDir);
    }

    private int generateFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            int port = socket.getLocalPort();
            return port;
        } catch (IOException e) {}
        return -1;
    }

    private static class PluginConfigurableNode extends Node {

        public PluginConfigurableNode(Settings settings, Collection<Class<? extends Plugin>> classpathPlugins) {
            super(InternalSettingsPreparer.prepareEnvironment(settings, null), classpathPlugins);
        }
    }
}
