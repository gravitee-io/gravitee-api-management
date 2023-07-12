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
package io.gravitee.plugin.entrypoint.http.get.utils;

import io.gravitee.common.http.MediaType;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IntegrationTestMethodSourceProvider {

    public static Stream<Arguments> provideBadAcceptHeaders() {
        return Stream.of(
            Arguments.of(List.of(MediaType.APPLICATION_GRPC)),
            // Injecting multivalue ACCEPT as one entry
            Arguments.of(List.of(MediaType.APPLICATION_GRPC + "," + MediaType.APPLICATION_JWT)),
            // Here it's like we have two entries of ACCEPT header
            Arguments.of(List.of(MediaType.APPLICATION_GRPC + "," + MediaType.APPLICATION_JWT, MediaType.APPLICATION_ATOM_XML)),
            // Dealing with quality
            Arguments.of(
                List.of(
                    MediaType.APPLICATION_GRPC + ";q=0.9," + MediaType.APPLICATION_JWT + ";q=0.8",
                    MediaType.APPLICATION_ATOM_XML + ";q=0.1"
                )
            )
        );
    }

    public static Stream<Arguments> provideValidAcceptHeaders() {
        return Stream.of(
            Arguments.of(MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_JSON)),
            Arguments.of(MediaType.APPLICATION_JSON, List.of(MediaType.WILDCARD)),
            Arguments.of(MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_GRPC + ";q=0.8", MediaType.WILDCARD + ";q=0.9")),
            Arguments.of(MediaType.TEXT_PLAIN, List.of("")),
            Arguments.of(MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_GRPC + "," + MediaType.APPLICATION_JSON)),
            Arguments.of(
                MediaType.APPLICATION_XML,
                List.of(MediaType.APPLICATION_GRPC + "," + MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
            ),
            Arguments.of(
                MediaType.TEXT_PLAIN,
                List.of(MediaType.APPLICATION_GRPC + ";q=0.9," + MediaType.APPLICATION_JWT + ";q=0.8", MediaType.TEXT_PLAIN + ";q=0.1")
            )
        );
    }
}
