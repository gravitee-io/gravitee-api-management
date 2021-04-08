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
package io.gravitee.repository.bridge.client.management;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.gravitee.repository.bridge.client.http.HttpResponse;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.MaybeSubject;
import io.reactivex.subjects.SingleSubject;
import io.vertx.core.Future;
import io.vertx.ext.web.codec.BodyCodec;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpNodeMonitoringRepository extends AbstractRepository implements NodeMonitoringRepository {

    @Override
    public Maybe<Monitoring> findByNodeIdAndType(String nodeId, String type) {
        final Future<HttpResponse<Monitoring>> future = get("/node/monitoring?nodeId=" + nodeId + "&type=" + type, BodyCodec.json(Monitoring.class))
                .send();

        MaybeSubject<Monitoring> maybe = MaybeSubject.create();
        future.onComplete(promise -> {
            if (promise.succeeded()) {
                if (promise.result().statusCode() == HttpStatusCode.OK_200) {
                    maybe.onSuccess(promise.result().payload());
                } else {
                    maybe.onComplete();
                }
            } else {
                maybe.onError(promise.cause());
            }
        });

        return maybe;
    }

    @Override
    public Single<Monitoring> create(Monitoring monitoring) {
        final Future<HttpResponse<Monitoring>> future = post("/node/monitoring", BodyCodec.json(Monitoring.class))
                .send(monitoring);

        SingleSubject<Monitoring> single = SingleSubject.create();
        future.onComplete(promise -> {
            if (promise.succeeded()) {
                single.onSuccess(promise.result().payload());
            } else {
                single.onError(promise.cause());
            }
        });

        return single;
    }

    @Override
    public Single<Monitoring> update(Monitoring monitoring) {
        final Future<HttpResponse<Monitoring>> future = put("/node/monitoring", BodyCodec.json(Monitoring.class))
                .send(monitoring);

        SingleSubject<Monitoring> single = SingleSubject.create();
        future.onComplete(promise -> {
            if (promise.succeeded()) {
                single.onSuccess(promise.result().payload());
            } else {
                single.onError(promise.cause());
            }
        });

        return single;
    }

    @Override
    public Flowable<Monitoring> findByTypeAndTimeFrame(String type, long from, long to) {
        return Flowable.error(new IllegalStateException());
    }
}
