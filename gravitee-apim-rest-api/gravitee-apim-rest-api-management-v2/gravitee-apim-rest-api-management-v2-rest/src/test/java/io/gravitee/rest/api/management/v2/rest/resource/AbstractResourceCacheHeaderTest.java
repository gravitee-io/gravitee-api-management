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
package io.gravitee.rest.api.management.v2.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class AbstractResourceCacheHeaderTest {

    private static final AbstractResource RESOURCE = new AbstractResource() {};

    @Nested
    class WithCacheHeaders {

        @Test
        void emits_etag_and_last_modified_with_correct_epoch_millis_formula() {
            Date date = new Date(12345L);
            Response.ResponseBuilder builder = Response.ok();

            Response.ResponseBuilder returned = RESOURCE.applyCacheHeaders(builder, date);
            Response response = returned.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returned).isSameAs(builder);
                softly.assertThat(response.getEntityTag()).isEqualTo(new EntityTag("12345"));
                softly.assertThat(response.getLastModified()).isNotNull();
                softly.assertThat(response.getLastModified().getTime()).isEqualTo(12345L);
            });
        }

        @Test
        void suppresses_both_headers_when_updatedAt_is_null_without_throwing() {
            Response response = RESOURCE.applyCacheHeaders(Response.ok("body"), null).build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getEntityTag()).isNull();
                softly.assertThat(response.getLastModified()).isNull();
            });
        }

        @Test
        void emits_headers_for_zero_epoch_date() {
            Response response = RESOURCE.applyCacheHeaders(Response.ok(), new Date(0L)).build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getEntityTag()).isEqualTo(new EntityTag("0"));
                softly.assertThat(response.getLastModified()).isNotNull();
                softly.assertThat(response.getLastModified().getTime()).isEqualTo(0L);
            });
        }

        @Test
        void etag_value_and_last_modified_encode_same_instant() {
            Date date = new Date(1_700_000_000_000L);
            Response response = RESOURCE.applyCacheHeaders(Response.ok(), date).build();

            long etagMillis = Long.parseLong(response.getEntityTag().getValue());
            assertThat(etagMillis).isEqualTo(response.getLastModified().getTime());
        }
    }
}
