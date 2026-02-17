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
package io.gravitee.apim.reporter.elasticsearch.bulk;

import io.gravitee.apim.reporter.common.bulk.compressor.CompressedBulk;
import io.gravitee.apim.reporter.common.bulk.exception.SendReportException;
import io.gravitee.apim.reporter.common.bulk.sender.BulkSender;
import io.gravitee.common.service.AbstractService;
import io.gravitee.elasticsearch.client.Client;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class ElasticBulkSender extends AbstractService<BulkSender> implements BulkSender {

    private final Client elasticClient;

    @Override
    public Completable send(final CompressedBulk bulk) {
        return elasticClient
            .bulk(bulk.compressed().getDelegate())
            .flatMapCompletable(bulkResponse -> {
                if (bulkResponse.getErrors()) {
                    return Completable.error(buildException(bulkResponse.getError().getReason(), null));
                }
                return Completable.complete();
            })
            .onErrorReturn(e -> buildException(e.getMessage(), e))
            .ignoreElement();
    }

    private Exception buildException(final String errorMessage, final Throwable throwable) {
        final String message = "Unable to send bulk data to elasticsearch. Failure reason from ES is [%s].".formatted(errorMessage);
        return new SendReportException(message, throwable);
    }
}
