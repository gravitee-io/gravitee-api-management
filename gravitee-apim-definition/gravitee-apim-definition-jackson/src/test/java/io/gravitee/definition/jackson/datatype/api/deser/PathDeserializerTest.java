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

package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.listener.http.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathDeserializerTest {

    GraviteeMapper mapper = new GraviteeMapper();

    @Test
    public void deserialize_all_fields_present() throws JsonProcessingException {
        String pathJson = "{\"host\":\"http://localhost\",\"path\":\"/test\",\"overrideAccess\":\"true\"}";
        Path path = mapper.readValue(pathJson, Path.class);
        assertAll(
                () -> assertThat(path.getHost()).isEqualTo("http://localhost"),
                () -> assertThat(path.getPath()).isEqualTo("/test"),
                () -> assertTrue(path.isOverrideAccess())
        );
    }

    @Test
    public void deserialize_should_remove_trailing_slash() throws JsonProcessingException {
        String pathJson = "{\"path\":\"/test/\"}";
        Path path = mapper.readValue(pathJson, Path.class);
        assertThat(path.getPath()).isEqualTo("/test");
    }
}
