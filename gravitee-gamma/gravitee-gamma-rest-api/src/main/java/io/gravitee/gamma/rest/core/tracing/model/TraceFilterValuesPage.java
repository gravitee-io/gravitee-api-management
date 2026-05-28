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

import java.util.List;

/**
 * Domain page shape for filter values. Page number and size are out-of-band — they're caller-side
 * input, not part of the page state itself; the use case returns the data slice plus the total
 * count and the REST layer assembles the wire response with the request's echo of page / perPage.
 *
 * @author GraviteeSource Team
 */
public record TraceFilterValuesPage(List<TraceFilterValue> data, long totalElements) {}
