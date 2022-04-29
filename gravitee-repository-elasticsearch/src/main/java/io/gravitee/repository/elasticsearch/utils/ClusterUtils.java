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
package io.gravitee.repository.elasticsearch.utils;

import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;

import java.util.stream.Stream;

public class ClusterUtils {

    private static final String TENANT_FIELD = "tenant";
    private static final String[] ALL_CLUSTERS = {"*"};

    public static String[] extractClusterIndexPrefixes(io.gravitee.repository.analytics.query.AbstractQuery query, RepositoryConfiguration configuration) {
        if (!configuration.hasCrossClusterMapping()) {
            return new String[0];
        }

        // Extract tenant(s) filtering
        if (query != null && query.query() != null) {
            return extractClusterIndexPrefixes(query.query().filter(), configuration);
        }

        return ALL_CLUSTERS;
    }

    public static String[] extractClusterIndexPrefixes(io.gravitee.repository.healthcheck.query.AbstractQuery query, RepositoryConfiguration configuration) {
        if (!configuration.hasCrossClusterMapping()) {
            return new String[0];
        }

        // Extract tenant(s) filtering
        if (query != null && query.query() != null) {
            return extractClusterIndexPrefixes(query.query().filter(), configuration);
        }

        return ALL_CLUSTERS;
    }

    private static String[] extractClusterIndexPrefixes(String filter, RepositoryConfiguration configuration) {
        if (filter != null) {
            // Extract tenant(s) filtering
            int idx = filter.indexOf(TENANT_FIELD);
            if (idx != -1) {
                idx += TENANT_FIELD.length() + 1;
                int lastParenthesis = filter.indexOf(')', idx);
                int and = filter.indexOf(" AND", idx);
                String tenantQuery = filter.substring(idx,
                        and < 0
                                ? lastParenthesis < 0
                                ? filter.length()
                                : lastParenthesis
                                :and
            );
                return Stream
                        .of(tenantQuery.split(" OR "))
                        .map(tenant -> {
                            //clear the tenant name
                            tenant = tenant
                                    .replaceAll("\\\\","")
                                    .replaceAll("\\(","")
                                    .replaceAll("\\)","")
                                    .replaceAll("\"","");
                            return configuration.getCrossClusterMapping().get(tenant);
                        })
                        .toArray(String[]::new);
            }
        }
        return ALL_CLUSTERS;
    }

    public static String[] extractClusterIndexPrefixes(RepositoryConfiguration configuration) {
        if (!configuration.hasCrossClusterMapping()) {
            return new String[0];
        }

        return ALL_CLUSTERS;
    }
}
