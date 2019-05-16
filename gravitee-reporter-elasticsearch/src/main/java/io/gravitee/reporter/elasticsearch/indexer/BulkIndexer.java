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
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.buffer.Buffer;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.Vertx;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class BulkIndexer extends AbstractIndexer {

	/**
	 * Elasticsearch client.
	 */
	@Autowired
	private Client client;

	@Autowired
	private Vertx vertx;

    /**
	 * Configuration of Elasticsearch (cluster name, addresses, ...)
	 */
	@Autowired
	private ReporterConfiguration configuration;

	private final PublishProcessor<Reportable> bulkProcessor = PublishProcessor.create();

	@PostConstruct
	public void initialize() {
		bulkProcessor
				.onBackpressureBuffer()
				.observeOn(Schedulers.io())
				.map(this::transform)
				.buffer(
						configuration.getFlushInterval(),
						TimeUnit.SECONDS,
						configuration.getBulkActions())
				.filter(payload -> !payload.isEmpty())
				.subscribe(new Subscriber<List<Buffer>>() {
					private Subscription subscription;

					@Override
					public void onSubscribe(Subscription subscription) {
						this.subscription = subscription;
						subscription.request(1);
					}

					@Override
					public void onNext(List<Buffer> items) {
						client.bulk(items)
								.subscribeOn(RxHelper.scheduler(vertx.getDelegate()))
								.subscribe(new DisposableSingleObserver<BulkResponse>() {
									@Override
									public void onSuccess(BulkResponse bulkResponse) {
										dispose();
									}

									@Override
									public void onError(Throwable t) {
										logger.error("Unexpected error while indexing data", t);
									}
								});

						subscription.request(1);
					}

					@Override
					public void onError(Throwable t) {
						logger.error("Unexpected error while indexing data", t);
					}

					@Override
					public void onComplete() {
						// Nothing to do here
					}
				});
	}

	@Override
	public void index(Reportable reportable) {
		bulkProcessor.onNext(reportable);
	}
}