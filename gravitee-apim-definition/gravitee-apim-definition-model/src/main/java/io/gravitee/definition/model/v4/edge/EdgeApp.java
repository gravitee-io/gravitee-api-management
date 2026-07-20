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
import java.util.List;

/**
 * App-centric interception unit: a single application (e.g. Claude Code, Cursor) that owns the
 * domains it talks to and the routes its intercepted traffic follows.
 *
 * @param name human-readable application identifier (e.g. {@code Claude Code}, {@code Cursor})
 * @param domains vendor backend hostnames owned by this app, keyed for SNI/DNS interception
 * @param routes path-to-api_path mappings applied to this app's intercepted traffic
 * @param format usage-decoder format used to parse token usage from the response
 *               (e.g. {@code anthropic-messages}, {@code openai-chat}), decoupled from the vendor label
 * @param vendor vendor label reported upstream for accounting/attribution, independent of the decoder {@code format}
 */
public record EdgeApp(String name, List<DnsDomain> domains, List<RouteMapping> routes, String format, String vendor) implements
    Serializable {}
