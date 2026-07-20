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
 * @param pathPrefix incoming request path prefix that triggers this mapping
 * @param apiPath gateway api_path the matched traffic is forwarded to
 */
public record RouteMapping(String pathPrefix, String apiPath) implements Serializable {}
