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
package io.gravitee.repository.elasticsearch;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume Waignier (zenika)
 * @author Sebastien Devaux (zenika)
 * @author GraviteeSource Team
 */
public abstract class AbstractElasticsearchRepository {

    /**
     * Elasticsearch component to perform HTTP request.
     */
    @Autowired
    protected Client client;

    /**
     * Templating component
     */
    @Autowired
    protected FreeMarkerComponent freeMarkerComponent;

    /**
     * Util component used to compute index name.
     */
    @Autowired
    protected IndexNameGenerator indexNameGenerator;

    @Autowired
    protected ElasticsearchInfo info;

    protected String getQueryIndexesFromDefinitionVersions(
        Type v2Index,
        Type v4Index,
        RepositoryConfiguration configuration,
        QueryContext queryContext,
        List<DefinitionVersion> definitionVersions
    ) {
        var isDefinitionVersionsNullOrEmpty = definitionVersions == null || definitionVersions.isEmpty();

        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var indexV2Request = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), v2Index, clusters);
        var indexV4Metrics = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), v4Index, clusters);

        var indexes = new ArrayList<String>();

        if (isDefinitionVersionsNullOrEmpty || definitionVersions.contains(DefinitionVersion.V4)) {
            indexes.add(indexV4Metrics);
        }
        if (
            isDefinitionVersionsNullOrEmpty ||
            definitionVersions.contains(DefinitionVersion.V2) ||
            definitionVersions.contains(DefinitionVersion.V1)
        ) {
            indexes.add(indexV2Request);
        }

        return String.join(",", indexes);
    }
}
