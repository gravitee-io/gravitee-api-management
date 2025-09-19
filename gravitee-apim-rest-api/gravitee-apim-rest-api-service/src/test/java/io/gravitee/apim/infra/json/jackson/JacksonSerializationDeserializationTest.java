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
package io.gravitee.apim.infra.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.gravitee.apim.core.json.JsonProcessingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class JacksonSerializationDeserializationTest {

    @Nested
    class Serialization {

        JacksonJsonSerializer serializer = new JacksonJsonSerializer(JsonMapperFactory.build(false));

        @Test
        void should_not_serialize_null_value() throws JsonProcessingException {
            var object = MyPojo.builder().aString("my-string").build();

            var result = serializer.serialize(object);

            System.out.println(result);
            assertThat(result).doesNotContain("dateTime");
        }

        @Test
        void should_not_serialize_custom_methods_starting_with_is() throws JsonProcessingException {
            var object = MyPojo.builder().aString("my-string").build();

            var result = serializer.serialize(object);

            assertThat(result).doesNotContain("ok");
        }

        @Test
        void should_not_serialize_custom_methods_starting_with_get() throws JsonProcessingException {
            var object = MyPojo.builder().aString("my-string").build();

            var result = serializer.serialize(object);

            System.out.println(result);
            assertThat(result).doesNotContain("order");
        }

        @Test
        void should_not_serialize_custom_methods_starting_with_set() throws JsonProcessingException {
            var object = MyPojo.builder().aString("my-string").build();

            var result = serializer.serialize(object);

            System.out.println(result);
            assertThat(result).doesNotContain("time");
        }

        @Test
        void should_serialize_JavaTime_attribute() throws JsonProcessingException {
            var object = MyPojo.builder().dateTime(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC)).build();

            var result = serializer.serialize(object);

            assertThat(result).isEqualTo(
                """
                {"dateTime":1580674922.000000000}"""
            );
        }
    }

    @Nested
    class Deserialization {

        JacksonJsonDeserializer deserializer = new JacksonJsonDeserializer(JsonMapperFactory.build(false));

        @Test
        void should_throw_when_unknown_properties() {
            var throwable = Assertions.catchThrowable(() ->
                deserializer.deserialize("{\"aString\":\"aString\",\"aBoolean\":true, \"unknown\":123}", MyPojo.class)
            );

            assertThat(throwable).hasCauseInstanceOf(UnrecognizedPropertyException.class);
        }

        @Test
        void should_not_deserialize_custom_methods_starting_with_setter() {
            var throwable = Assertions.catchThrowable(() ->
                deserializer.deserialize("{\"aString\":\"aString\",\"aBoolean\":true, \"time\":123}", MyPojo.class)
            );

            assertThat(throwable).hasCauseInstanceOf(UnrecognizedPropertyException.class);
        }

        @Test
        void should_deserialize_JavaTime_attribute() throws JsonProcessingException {
            var result = deserializer.deserialize("{\"dateTime\":1580675922.000000000}", MyPojo.class);

            assertThat(result).isEqualTo(
                MyPojo.builder().dateTime(Instant.parse("2020-02-02T20:38:42.00Z").atZone(ZoneOffset.UTC)).build()
            );
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class MyPojo {

        private String aString;
        private ZonedDateTime dateTime;

        public boolean isOk() {
            return true;
        }

        public int getOrder() {
            return 1;
        }

        public void setTime(long ignoredTime) {
            throw new UnsupportedOperationException();
            // do nothing
        }
    }
}
