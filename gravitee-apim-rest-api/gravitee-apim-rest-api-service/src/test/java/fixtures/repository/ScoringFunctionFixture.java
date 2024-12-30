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
package fixtures.repository;

import io.gravitee.repository.management.model.ScoringFunction;
import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;

public class ScoringFunctionFixture {

    private ScoringFunctionFixture() {}

    public static final Supplier<ScoringFunction.ScoringFunctionBuilder> BASE = () ->
        ScoringFunction
            .builder()
            .id("function-id")
            .name("function-name")
            .payload("function-payload")
            .referenceId("reference-id")
            .referenceType("ENVIRONMENT")
            .createdAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));

    public static ScoringFunction aFunction() {
        return BASE.get().build();
    }
}
