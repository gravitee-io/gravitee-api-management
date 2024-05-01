/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.plugin.endpoint.database.proxy.client;

import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.plugin.endpoint.database.proxy.configuration.DatabaseProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.database.proxy.configuration.DatabaseProxyEndpointConnectorSharedConfiguration;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseClientFactory {

    private Pool jdbcPool;
    private final AtomicBoolean jdbcClientCreated = new AtomicBoolean(false);

    /*
    This would be equivalent to what is in the HTTP version if necessary.
    public JDBCPool getOrBuildJdbcClient(
        final ExecutionContext ctx,
        final DatabaseProxyEndpointConnectorConfiguration configuration,
        final DatabaseProxyEndpointConnectorSharedConfiguration sharedConfiguration
        ) {
        if (jdbcClient == null) {
            synchronized (this) {
                // Double-checked locking.
                if (jdbcClientCreated.compareAndSet(false, true)) {
                    jdbcClient = buildJdbcClient(ctx, configuration, sharedConfiguration);
                }
            }
        }
        return jdbcClient;
    }

     */


    // TODO add pool options to config
    public Pool getOrBuildJdbcClient(
        final ExecutionContext ctx,
        final DatabaseProxyEndpointConnectorConfiguration configuration,
        final DatabaseProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        PgConnectOptions options = new PgConnectOptions()
            .setPort(5432)
            .setHost("localhost")
            .setDatabase(configuration.getDatabase())
            .setUser(configuration.getUsername())
            .setPassword(configuration.getPassword());

        PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(5);

        return PgBuilder
            .pool()
            .with(poolOptions)
            .connectingTo(options)
            .using(ctx.getComponent(Vertx.class).getDelegate())
            .build();
    }


}
