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
package io.gravitee.reporter.elasticsearch.indexer;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.bulk.BulkResponse;
import io.gravitee.elasticsearch.model.bulk.Failure;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.reactivex.core.Vertx;
import java.util.List;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class DocumentBulkProcessor implements Subscriber<List<Buffer>> {

    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(DocumentBulkProcessor.class);

    private Subscription subscription;

    private final Client client;

    private final Vertx vertx;

    DocumentBulkProcessor(Client client, Vertx vertx) {
        this.client = client;
        this.vertx = vertx;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(List<Buffer> items) {
        try {
            client
                .bulk(items)
                .onErrorResumeNext(t -> {
                    logger.error("Unexpected error while indexing data", t);
                    Failure failure = new Failure();
                    failure.setReason(t.getMessage());
                    BulkResponse bulkResponse = new BulkResponse();
                    bulkResponse.setErrors(true);
                    bulkResponse.setError(failure);
                    return Single.just(bulkResponse);
                })
                .subscribe();
        } catch (Exception ex) {
            logger.error("Unexpected error while bulking data with the ES client", ex);
        } finally {
            subscription.request(1);
        }
    }

    @Override
    public void onError(Throwable t) {
        logger.error("Unexpected error while indexing data", t);
    }

    @Override
    public void onComplete() {
        // Nothing to do here
    }
}
