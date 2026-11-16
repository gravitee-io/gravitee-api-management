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
package io.gravitee.apim.reporter.elasticsearch.mapping.datastream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.utils.Type;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.net.ConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DataStreamIndexPreparerTest {

    @Mock
    private Client client;

    private DataStreamIndexPreparer cut;

    @BeforeEach
    void setUp() {
        var freeMarkerComponent = FreeMarkerComponent.builder()
            .classLoader(DataStreamIndexPreparerTest.class.getClassLoader())
            .classLoaderTemplateBase("freemarker")
            .build();
        cut = new DataStreamIndexPreparer(
            new ReporterConfiguration(),
            new PipelineConfiguration(freeMarkerComponent),
            freeMarkerComponent,
            client,
            DataStreamLifecycle.elasticsearch(ReporterConfiguration.DEFAULT_INDEX_LIFECYCLE_POLICY_PROPERTY_NAME, true)
        );
        lenient().when(client.getAlias(anyString())).thenReturn(Maybe.empty());
        lenient().when(client.putIndexTemplate(anyString(), anyString())).thenReturn(Completable.complete());
        lenient().when(client.putPipeline(anyString(), anyString())).thenReturn(Completable.complete());
    }

    @Test
    void should_put_one_template_per_data_stream_when_no_alias_has_their_name() {
        // When
        cut.prepare().test().assertComplete();

        // Then
        for (Type type : Type.TYPES) {
            verify(client).putIndexTemplate(eq("gravitee-" + type.getType()), anyString());
        }
    }

    @Test
    void should_refuse_the_data_streams_when_aliases_left_by_the_ilm_mode_have_their_names() {
        // Given
        when(client.getAlias("gravitee-request")).thenReturn(Maybe.just(new ObjectMapper().createObjectNode()));
        when(client.getAlias("gravitee-v4-log")).thenReturn(Maybe.just(new ObjectMapper().createObjectNode()));

        // When / Then: Elasticsearch would write the documents to the indices of the alias instead of a data stream
        cut
            .prepare()
            .test()
            .assertError(error -> {
                assertThat(error.getMessage()).contains(
                    "[gravitee-request, gravitee-v4-log]",
                    "'reporters.elasticsearch.index_mode' is 'data_stream'",
                    "'index.lifecycle.indexing_complete'",
                    "'reporters.elasticsearch.index'"
                );
                return true;
            });
        verify(client, never()).putIndexTemplate(anyString(), anyString());
    }

    @Test
    void should_put_the_templates_when_elasticsearch_cannot_be_reached_to_read_the_aliases() {
        // Given: the client reports a failed connection as an error, and any other status than 200 as no alias
        when(client.getAlias("gravitee-request")).thenReturn(Maybe.error(new ConnectException("Connection refused")));

        // When
        cut.prepare().test().assertComplete();

        // Then
        verify(client).putIndexTemplate(eq("gravitee-request"), anyString());
    }
}
