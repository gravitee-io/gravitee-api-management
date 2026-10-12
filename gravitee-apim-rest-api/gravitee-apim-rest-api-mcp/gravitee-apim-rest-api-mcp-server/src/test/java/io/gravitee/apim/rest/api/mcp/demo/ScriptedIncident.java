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
package io.gravitee.apim.rest.api.mcp.demo;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.model.FailedRequest;
import io.gravitee.apim.rest.api.mcp.core.model.StatusGroup;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TimelinePoint;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.ChangeLog;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.RequestLogs;
import io.gravitee.apim.rest.api.mcp.core.port.TrafficAnalytics;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The demo scenario, computed instead of measured. Two environments: in production, "Orders" and "Stock" under
 * steady traffic and the API key of the one application of Orders revoked at a given instant while its client
 * keeps sending it; in staging, a quiet "Stock". A third environment, dev, the caller may not read.
 */
public class ScriptedIncident implements TrafficAnalytics, ApiDirectory, RequestLogs, ChangeLog, EnvironmentDirectory {

    public static final EnvRef PROD = new EnvRef("3c1f-prod", "prod", "Production");
    public static final EnvRef STAGING = new EnvRef("9a7e-staging", "staging", "Staging");
    public static final EnvRef DEV = new EnvRef("51d2-dev", "dev", "Dev");
    public static final ApiRef ORDERS = new ApiRef("0f3c9a52-orders", "Orders");
    public static final ApiRef STOCK = new ApiRef("7b1e44d0-stock", "Stock");
    public static final ApiRef STOCK_STAGING = new ApiRef("c4d8e0a1-stock", "Stock");

    private static final double ORDERS_PER_SECOND = 8;
    private static final double STOCK_PER_SECOND = 5;
    private static final double STAGING_PER_SECOND = 1;
    private static final String CLIENT_IP = "10.244.0.31";
    private static final String GATEWAY = "gateway-7d9f";

    private final Instant revokedAt;

    public ScriptedIncident(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    @Override
    public Environments environments(McpCaller caller) {
        return new Environments(List.of(PROD, STAGING), List.of(DEV));
    }

    @Override
    public List<ApiRef> visibleTo(AuditInfo caller) {
        return inProd(caller) ? List.of(ORDERS, STOCK) : List.of(STOCK_STAGING);
    }

    @Override
    public TrafficStats total(AuditInfo caller, TimeWindow window, TrafficScope scope) {
        if (!inProd(caller)) {
            return staging(window.from(), window.to(), scope.statusGroup());
        }
        var orders = orders(window.from(), window.to(), scope.statusGroup());
        if (ORDERS.id().equals(scope.apiId())) {
            return orders;
        }
        var stock = stock(window.from(), window.to(), scope.statusGroup());
        if (STOCK.id().equals(scope.apiId())) {
            return stock;
        }
        return TrafficStats.of(orders.requests() + stock.requests(), orders.clientErrors(), 0, 46.0, 41.0);
    }

    @Override
    public List<TrafficSample> by(AuditInfo caller, TimeWindow window, TrafficScope scope, Dimension dimension) {
        var samples = new ArrayList<TrafficSample>();
        if (!inProd(caller)) {
            var stagingStock = staging(window.from(), window.to(), scope.statusGroup());
            switch (dimension) {
                case API -> add(samples, true, STOCK_STAGING.id(), STOCK_STAGING.name(), stagingStock);
                case STATUS -> add(samples, true, "200", "200 OK", stagingStock);
                case APPLICATION -> add(samples, true, "1", "Unknown", stagingStock);
                default -> add(samples, true, "staging-" + dimension.name().toLowerCase(), "staging", stagingStock);
            }
            return samples;
        }
        boolean withOrders = scope.apiId() == null || ORDERS.id().equals(scope.apiId());
        boolean withStock = scope.apiId() == null || STOCK.id().equals(scope.apiId());
        var rejected = rejected(window.from(), window.to());
        var accepted = accepted(window.from(), window.to(), scope.statusGroup());
        var stock = stock(window.from(), window.to(), scope.statusGroup());
        switch (dimension) {
            case API -> {
                add(samples, withOrders, ORDERS.id(), ORDERS.name(), orders(window.from(), window.to(), scope.statusGroup()));
                add(samples, withStock, STOCK.id(), STOCK.name(), stock);
            }
            case APPLICATION -> {
                add(samples, withOrders, "1", "Unknown", rejected);
                add(samples, withOrders, "c1d2-mobile-app", "Mobile app", accepted);
                add(samples, withStock, "e5f6-warehouse", "Warehouse", stock);
            }
            case PLAN -> {
                add(samples, withOrders, "1", "1", rejected);
                add(samples, withOrders, "a7-api-key", "API Key", accepted);
                add(samples, withStock, "b8-keyless", "Keyless", stock);
            }
            case STATUS -> {
                add(samples, withOrders, "401", "401 Unauthorized", rejected);
                add(
                    samples,
                    withOrders || withStock,
                    "200",
                    "200 OK",
                    sum(withOrders ? accepted : TrafficStats.NONE, withStock ? stock : TrafficStats.NONE)
                );
            }
            case CONSUMER_IP -> {
                add(samples, withOrders, CLIENT_IP, CLIENT_IP, orders(window.from(), window.to(), scope.statusGroup()));
                add(samples, withStock, "10.244.0.44", "10.244.0.44", stock);
            }
            case PATH -> {
                add(samples, withOrders, "/orders", "/orders", orders(window.from(), window.to(), scope.statusGroup()));
                add(samples, withStock, "/stock", "/stock", stock);
            }
            default -> add(samples, true, GATEWAY, GATEWAY, total(caller, window, scope));
        }
        return samples;
    }

    @Override
    public List<TimelinePoint> timeline(AuditInfo caller, TimeWindow window, Duration interval, TrafficScope scope) {
        var points = new ArrayList<TimelinePoint>();
        for (var at = window.from(); at.isBefore(window.to()); at = at.plus(interval)) {
            points.add(new TimelinePoint(at, total(caller, new TimeWindow(at, at.plus(interval)), scope)));
        }
        return points;
    }

    @Override
    public Page failures(AuditInfo caller, TimeWindow window, Query query, int limit) {
        boolean concerned =
            inProd(caller) &&
            (query.apiId() == null || ORDERS.id().equals(query.apiId())) &&
            query.statusGroup() != StatusGroup.SERVER_ERROR &&
            (query.errorKey() == null || "API_KEY_INVALID".equals(query.errorKey()));
        long total = concerned ? rejected(window.from(), window.to()).requests() : 0;
        var requests = new ArrayList<FailedRequest>();
        for (int i = 0; i < Math.min(limit, total); i++) {
            requests.add(
                new FailedRequest(
                    window.to().minusMillis(125L * (i + 1)),
                    ORDERS,
                    "GET",
                    "/orders",
                    401,
                    "API_KEY_INVALID",
                    "api-key",
                    "API Key is not valid or is expired / revoked.",
                    "Unknown",
                    null,
                    GATEWAY,
                    2,
                    "req-" + (9000 - i)
                )
            );
        }
        return new Page(total, requests);
    }

    @Override
    public List<Change> deployments(AuditInfo caller, String apiId, TimeWindow window) {
        return List.of();
    }

    @Override
    public List<Change> audit(AuditInfo caller, String apiId, TimeWindow window) {
        boolean inWindow = !revokedAt.isBefore(window.from()) && revokedAt.isBefore(window.to());
        return ORDERS.id().equals(apiId) && inWindow
            ? List.of(
                new Change(
                    revokedAt,
                    Change.Source.AUDIT,
                    "APIKEY_REVOKED",
                    "Jane Admin",
                    Map.of("API_KEY", "5e0a-key-id", "APPLICATION", "Mobile app")
                )
            )
            : List.of();
    }

    private static boolean inProd(AuditInfo caller) {
        return PROD.id().equals(caller.environmentId());
    }

    private TrafficStats orders(Instant from, Instant to, StatusGroup group) {
        return sum(accepted(from, to, group), group == StatusGroup.SERVER_ERROR ? TrafficStats.NONE : rejected(from, to));
    }

    /** Requests of "Orders" the gateway let through: all of them until the key is revoked. */
    private TrafficStats accepted(Instant from, Instant to, StatusGroup group) {
        long requests = group == null ? Math.round(seconds(from, earliest(to, revokedAt)) * ORDERS_PER_SECOND) : 0;
        return requests == 0 ? TrafficStats.NONE : TrafficStats.of(requests, 0, 0, 48.0, 44.0);
    }

    /** Requests of "Orders" answered 401 by the gateway: all of them once the key is revoked. */
    private TrafficStats rejected(Instant from, Instant to) {
        long requests = Math.round(seconds(latest(from, revokedAt), to) * ORDERS_PER_SECOND);
        return requests == 0 ? TrafficStats.NONE : TrafficStats.of(requests, requests, 0, 2.0, null);
    }

    private static TrafficStats stock(Instant from, Instant to, StatusGroup group) {
        long requests = group == null ? Math.round(seconds(from, to) * STOCK_PER_SECOND) : 0;
        return requests == 0 ? TrafficStats.NONE : TrafficStats.of(requests, 0, 0, 43.0, 39.0);
    }

    private static TrafficStats staging(Instant from, Instant to, StatusGroup group) {
        long requests = group == null ? Math.round(seconds(from, to) * STAGING_PER_SECOND) : 0;
        return requests == 0 ? TrafficStats.NONE : TrafficStats.of(requests, 0, 0, 51.0, 47.0);
    }

    private static TrafficStats sum(TrafficStats one, TrafficStats other) {
        if (other.requests() == 0) {
            return one;
        }
        if (one.requests() == 0) {
            return other;
        }
        return TrafficStats.of(
            one.requests() + other.requests(),
            one.clientErrors() + other.clientErrors(),
            one.serverErrors() + other.serverErrors(),
            one.responseTimeP95Ms(),
            one.endpointResponseTimeP95Ms()
        );
    }

    private static void add(List<TrafficSample> samples, boolean wanted, String key, String name, TrafficStats stats) {
        if (wanted && stats.requests() > 0) {
            samples.add(new TrafficSample(key, name, stats));
        }
    }

    private static double seconds(Instant from, Instant to) {
        return to.isAfter(from) ? Duration.between(from, to).toMillis() / 1000.0 : 0;
    }

    private static Instant earliest(Instant one, Instant other) {
        return one.isBefore(other) ? one : other;
    }

    private static Instant latest(Instant one, Instant other) {
        return one.isAfter(other) ? one : other;
    }
}
