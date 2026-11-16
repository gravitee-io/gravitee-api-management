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
import io.gravitee.apim.reporter.common.bulk.exception.BulkItemsFailureException;
import io.gravitee.apim.reporter.common.bulk.exception.BulkItemsFailureException.ItemFailure;
import io.gravitee.apim.reporter.common.bulk.exception.SendReportException;
import io.gravitee.apim.reporter.common.bulk.sender.BulkSender;
import io.gravitee.common.service.AbstractService;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.bulk.BulkItemResponse;
import io.gravitee.elasticsearch.model.bulk.BulkResponse;
import io.gravitee.elasticsearch.model.bulk.Failure;
import io.gravitee.elasticsearch.model.bulk.Index;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ElasticBulkSender extends AbstractService<BulkSender> implements BulkSender {

    private static final int TOO_MANY_REQUESTS = 429;
    private static final int SERVER_ERROR = 500;

    private final Client elasticClient;

    private enum Outcome {
        WRITTEN,
        RETRYABLE,
        REJECTED,
    }

    @Override
    public Completable send(final CompressedBulk bulk) {
        return elasticClient
            .bulk(bulk.compressed())
            .onErrorResumeNext(throwable -> Single.error(sendFailure(throwable.getMessage(), throwable)))
            .flatMapCompletable(response ->
                Boolean.TRUE.equals(response.getErrors()) ? Completable.error(itemsFailure(response)) : Completable.complete()
            );
    }

    private static RuntimeException itemsFailure(final BulkResponse response) {
        final List<BulkItemResponse> items = response.getItems();
        if (items == null || items.isEmpty()) {
            return sendFailure("errors without any item in the bulk response", null);
        }

        final List<ItemFailure> failures = new ArrayList<>();
        for (int position = 0; position < items.size(); position++) {
            final Index item = items.get(position).getIndex();
            switch (outcome(item)) {
                case RETRYABLE -> failures.add(new ItemFailure(position, true, reason(item)));
                case REJECTED -> failures.add(new ItemFailure(position, false, reason(item)));
                case WRITTEN -> {}
            }
        }
        return new BulkItemsFailureException(failures);
    }

    private static Outcome outcome(final Index item) {
        final int status = item.getStatus();
        if (status < 300) {
            return Outcome.WRITTEN;
        }
        if (status == TOO_MANY_REQUESTS || status >= SERVER_ERROR) {
            return Outcome.RETRYABLE;
        }
        return Outcome.REJECTED;
    }

    private static String reason(final Index item) {
        final Failure error = item.getError();
        // The reason can quote the content of the document (e.g. a preview of a field value): only the type is logged.
        if (error == null) {
            return "status " + item.getStatus();
        }
        return error.getType();
    }

    private static SendReportException sendFailure(final String errorMessage, final Throwable throwable) {
        final String message = "Unable to send bulk data to elasticsearch. Failure reason from ES is [%s].".formatted(errorMessage);
        return new SendReportException(message, throwable);
    }
}
