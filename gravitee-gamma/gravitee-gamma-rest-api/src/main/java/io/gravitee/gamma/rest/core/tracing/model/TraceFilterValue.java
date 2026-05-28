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
package io.gravitee.gamma.rest.core.tracing.model;

/**
 * One element of a filter's allowed-value list. {@link #label} is optional and only meaningful for
 * filters where the stored {@link #value} is an opaque id (no such filter today; reserved for the
 * follow-up PR when keyword filters with id values are added).
 *
 * @author GraviteeSource Team
 */
public record TraceFilterValue(String value, String label) {}
