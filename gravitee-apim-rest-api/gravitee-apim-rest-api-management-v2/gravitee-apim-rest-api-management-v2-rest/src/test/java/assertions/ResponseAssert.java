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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.management.v2.rest.model.Error;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.ObjectAssert;

public class ResponseAssert extends AbstractObjectAssert<ResponseAssert, Response> {

    public ResponseAssert(Response apiEntity) {
        super(apiEntity, ResponseAssert.class);
    }

    public ResponseAssert hasStatus(int status) {
        isNotNull();
        if (actual.getStatus() != status) {
            failWithMessage("Expected response status to be <%s> but was <%s>", status, actual.getStatus());
        }
        return this;
    }

    public ResponseAssert hasHeader(String name, String value) {
        isNotNull();
        assertThat(actual.getHeaderString(name)).isEqualTo(value);
        return this;
    }

    public ErrorAssert asError() {
        isNotNull();
        var error = actual.readEntity(Error.class);
        return new ErrorAssert(error);
    }

    public <T> ObjectAssert<T> asEntity(Class<T> clazz) {
        isNotNull();
        T entity = actual.readEntity(clazz);
        return AssertionsForClassTypes.assertThat(entity);
    }
}
