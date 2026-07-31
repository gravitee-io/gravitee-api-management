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
package io.gravitee.definition.model.v4.edge;

import java.io.Serializable;

/**
 * Maps an intercepted request path to the gateway api_path it is forwarded to.
 *
 * <p>Traffic that no route matches is not routed: it is forwarded to its original backend, so an app only
 * intercepts what it explicitly declares.
 *
 * @param path incoming request path that triggers this mapping. A bare path (e.g. {@code /v1/messages}) is an
 *             exact match, on the path only, query parameters excluded. A path suffixed with {@code *}
 *             (e.g. {@code /v1/messages*}) is a prefix match, the prefix being the substring before the {@code *}.
 *             A catch-all route ({@code *} or {@code /*}) explicitly sends all of the app's traffic to its
 *             {@code apiPath}.
 * @param apiPath gateway api_path the matched traffic is forwarded to
 */
public record RouteMapping(String path, String apiPath) implements Serializable {}
