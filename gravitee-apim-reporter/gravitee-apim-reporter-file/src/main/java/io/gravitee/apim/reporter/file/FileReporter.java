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
package io.gravitee.apim.reporter.file;

import io.gravitee.apim.reporter.common.MetricsType;
import io.gravitee.apim.reporter.common.formatter.Formatter;
import io.gravitee.apim.reporter.common.formatter.FormatterFactory;
import io.gravitee.apim.reporter.file.config.FileReporterConfiguration;
import io.gravitee.apim.reporter.file.vertx.VertxFileWriter;
import io.gravitee.common.service.AbstractService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class FileReporter extends AbstractService<Reporter> implements Reporter {

    private final FileReporterConfiguration configuration;

    private final FormatterFactory formatterFactory;

    private final Vertx vertx;

    private final Map<Class<? extends Reportable>, VertxFileWriter<Reportable>> writers = new HashMap<>(4);

    public FileReporter(FileReporterConfiguration configuration, Vertx vertx, FormatterFactory formatterFactory) {
        this.formatterFactory = formatterFactory;
        this.configuration = configuration;
        this.vertx = vertx;
    }

    @Override
    public void report(Reportable reportable) {
        writers.get(reportable.getClass()).write(reportable);
    }

    @Override
    public boolean canHandle(Reportable reportable) {
        return configuration.isEnabled() && writers.containsKey(reportable.getClass());
    }

    @Override
    protected void doStart() {
        if (configuration.isEnabled()) {
            // Initialize reporters
            for (MetricsType type : MetricsType.values()) {
                Formatter<Reportable> formatter = formatterFactory.getFormatter(configuration.getOutputType(), type);
                applicationContext.getAutowireCapableBeanFactory().autowireBean(formatter);
                writers.put(
                    type.getClazz(),
                    new VertxFileWriter<>(
                        vertx,
                        type,
                        formatter,
                        configuration.getFilename() + '.' + configuration.getOutputType().getExtension(),
                        configuration
                    )
                );
            }

            Future.join(writers.values().stream().map(VertxFileWriter::initialize).toList()).onComplete(event -> {
                if (event.succeeded()) {
                    log.info("File reporter successfully started");
                } else {
                    log.error("An error occurs while starting file reporter", event.cause());
                }
            });
        }
    }

    @Override
    protected void doStop() {
        if (configuration.isEnabled()) {
            Future.join(writers.values().stream().map(VertxFileWriter::stop).toList()).onComplete(event -> {
                if (event.succeeded()) {
                    log.info("File reporter successfully stopped");
                } else {
                    log.error("An error occurs while stopping file reporter", event.cause());
                }
            });
        }
    }
}
