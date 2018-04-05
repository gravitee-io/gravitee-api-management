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
package io.gravitee.gateway.report.impl.bus;

import io.gravitee.reporter.api.Reportable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReporterVerticle extends AbstractVerticle {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReporterVerticle.class);

    static final String EVENT_BUS_ADDRESS = "gw:metrics";
    private MessageProducer<Reportable> producer;

    @Override
    public void start() throws Exception {
        // Register specific codec
        vertx.eventBus().registerCodec(new ReportableMessageCodec());

        producer = vertx.eventBus()
                .<Reportable>publisher(
                    EVENT_BUS_ADDRESS,
                    new DeliveryOptions()
                            .setCodecName(ReportableMessageCodec.CODEC_NAME))
                .exceptionHandler(
                        throwable -> LOGGER.error("Unexpected error while sending a reportable element", throwable));
    }

    @Override
    public void stop() throws Exception {
        if (producer != null) {
            producer.close();
        }
    }

    public void report(Reportable reportable) {
        producer.write(reportable);
    }
}
