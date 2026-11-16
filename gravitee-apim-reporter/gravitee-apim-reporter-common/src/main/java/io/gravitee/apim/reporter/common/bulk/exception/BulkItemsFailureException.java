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
package io.gravitee.apim.reporter.common.bulk.exception;

import java.io.Serial;
import java.util.List;

/**
 * Signals that the target accepted the bulk but failed to write some of its reports.
 * Reports that are not listed have been written.
 */
public class BulkItemsFailureException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -2454839120771239563L;

    private final transient List<ItemFailure> failures;

    public BulkItemsFailureException(List<ItemFailure> failures) {
        super("%d report(s) of the bulk could not be written".formatted(failures.size()));
        this.failures = List.copyOf(failures);
    }

    public List<ItemFailure> failures() {
        return failures;
    }

    /**
     * @param position the position of the report in the bulk, starting at 0.
     * @param retryable whether sending the report again may succeed.
     * @param reason why the target did not write the report.
     */
    public record ItemFailure(int position, boolean retryable, String reason) {}
}
