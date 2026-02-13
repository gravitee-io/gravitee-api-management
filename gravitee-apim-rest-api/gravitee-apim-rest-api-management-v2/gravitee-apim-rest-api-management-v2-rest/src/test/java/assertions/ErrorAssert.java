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
package assertions;

import io.gravitee.rest.api.management.v2.rest.model.Error;
import java.util.Objects;
import org.assertj.core.api.AbstractObjectAssert;

public class ErrorAssert extends AbstractObjectAssert<ErrorAssert, Error> {

    public ErrorAssert(Error error) {
        super(error, ErrorAssert.class);
    }

    public ErrorAssert hasHttpStatus(int status) {
        isNotNull();

        if (!Objects.equals(actual.getHttpStatus(), status)) {
            failWithMessage("Expected error http status to be <%s> but was <%s>", status, actual.getHttpStatus());
        }
        return this;
    }

    public ErrorAssert hasMessage(String message) {
        isNotNull();

        if (!Objects.equals(actual.getMessage(), message)) {
            failWithMessage("Expected error message to be <%s> but was <%s>", message, actual.getMessage());
        }
        return this;
    }

    public ErrorAssert hasMessageContaining(String substring) {
        isNotNull();

        if (actual.getMessage() == null || !actual.getMessage().contains(substring)) {
            failWithMessage("Expected error message to contain <%s> but was <%s>", substring, actual.getMessage());
        }
        return this;
    }
}
