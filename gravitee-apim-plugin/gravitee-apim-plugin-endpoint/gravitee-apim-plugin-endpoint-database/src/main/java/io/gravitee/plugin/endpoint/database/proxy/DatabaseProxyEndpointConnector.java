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

package io.gravitee.plugin.endpoint.database.proxy;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.sync.EndpointSyncConnector;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.plugin.endpoint.database.proxy.client.DatabaseClientFactory;
import io.gravitee.plugin.endpoint.database.proxy.configuration.DatabaseProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.database.proxy.configuration.DatabaseProxyEndpointConnectorSharedConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;

@Slf4j
public class DatabaseProxyEndpointConnector extends EndpointSyncConnector {

    private static final String ENDPOINT_ID = "database";
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.REQUEST_RESPONSE);

    protected final DatabaseProxyEndpointConnectorConfiguration configuration;
    protected final DatabaseProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    private DatabaseClientFactory databaseClientFactory;

    private String resolvedTable;

    public DatabaseProxyEndpointConnector(
        DatabaseProxyEndpointConnectorConfiguration configuration,
        DatabaseProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        this.configuration = configuration;
        this.sharedConfiguration = sharedConfiguration;
        this.databaseClientFactory = new DatabaseClientFactory();
        this.resolvedTable = resolveTable();
    }

    @Override
    public String id() {
        return ENDPOINT_ID;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Completable connect(final ExecutionContext ctx) {
        try {
            final Request request = ctx.request();
            final Response response = ctx.response();

            final Pool client = databaseClientFactory
                .getOrBuildJdbcClient(ctx, this.configuration, this.sharedConfiguration);

            return Single.just(
                client
                    .preparedQuery(buildQueryStringFromContext(ctx))
                    .execute()
                    .onFailure(
                        Throwable::printStackTrace // Todo actually do something here :)
                    )
                    .onSuccess(databaseResponse -> {
                        response.chunks(
                            Flowable.just(Buffer.buffer(databaseResponse.toString()))
                        );
                    // Todo start here
                    })
            ).ignoreElement();

        } catch (Exception e) {
            return Completable.error(e);
        }
    }

    private String buildQueryStringFromContext(ExecutionContext ctx) {
        /*
        We would add the features here to dynamically generate the query based on the input.
        E.g. a where clause on the select statement based on a query parameter or attribute.
        */
        return switch (ctx.request().method()) {
            case POST -> "INSERT INTO " + this.resolvedTable + " VALUES (4, 'Anthony', 10)";
            case GET -> "SELECT * FROM graviteers";
            default -> "SELECT * FROM " + this.resolvedTable; // Todo change
        };
    }

    private String resolveTable() {
        // Depending on database type we may need to prefix the table.
        // For postgres POC we just return the table name. Eventually there will be a switch statement here.
        // return String.format("%s.%s", configuration.getDatabase(), configuration.getTable());
        return this.configuration.getTable();
    }

}
