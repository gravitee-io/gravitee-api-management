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
package io.gravitee.apim.core.analytics_engine.model;

/**
 * Identifies the calling surface of an analytics query so the query context can be scoped with the
 * right semantics:
 * <ul>
 *     <li>{@link #MANAGEMENT}: scope by management permissions (admin / environment API read / API memberships).</li>
 *     <li>{@link #PORTAL}: scope by portal visibility (published / public / subscribed APIs).</li>
 * </ul>
 * The analytics use cases are shared singletons served to both the Management and the Portal REST
 * surfaces; the scope must therefore travel with each call to route it to the right context loader.
 *
 * @author GraviteeSource Team
 */
public enum AnalyticsScope {
    MANAGEMENT,
    PORTAL,
}
