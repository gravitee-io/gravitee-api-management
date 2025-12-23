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
package io.gravitee.reporter.elasticsearch.mapping.es7;

import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.mapping.AbstractIndexPreparer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ES7IndexPreparer extends AbstractIndexPreparer {

    public ES7IndexPreparer(
        final ReporterConfiguration configuration,
        final PipelineConfiguration pipelineConfiguration,
        final FreeMarkerComponent freeMarkerComponent,
        final Client client
    ) {
        super(configuration, pipelineConfiguration, freeMarkerComponent, client, "/es7x");
    }

    @Override
    protected boolean useOldClient(boolean dataStream) {
        return !dataStream;
    }
}
