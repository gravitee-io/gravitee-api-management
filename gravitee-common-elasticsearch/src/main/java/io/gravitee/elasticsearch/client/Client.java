/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.elasticsearch.client;

import io.gravitee.elasticsearch.exception.ElasticsearchException;
import io.gravitee.elasticsearch.model.CountResponse;
import io.gravitee.elasticsearch.model.Health;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.bulk.BulkResponse;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Client {

    Single<ElasticsearchInfo> getInfo() throws ElasticsearchException;
    Single<Health> getClusterHealth();
    Single<BulkResponse> bulk(List<Buffer> data);
    Completable putTemplate(String templateName, String template);
    Completable putPipeline(String templateName, String template);
    Single<SearchResponse> search(String indexes, String type, String query);
    Single<CountResponse> count(String indexes, String type, String query);
}
