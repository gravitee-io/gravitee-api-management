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
package fixtures.core.model;

import io.gravitee.apim.core.media.model.Media;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class MediaFixtures {

    private MediaFixtures() {}

    private static final Supplier<Media.MediaBuilder> BASE = () ->
        Media.builder()
            .hash("DB0A773F02AF003348F1B09734717266")
            .fileName("my-media.jpeg")
            .data("dummy-data".getBytes(StandardCharsets.UTF_8))
            .type("image")
            .subType("jpeg")
            .size(123456L);

    public static Media aMedia() {
        return BASE.get().build();
    }
}
