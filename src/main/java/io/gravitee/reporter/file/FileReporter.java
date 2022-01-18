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
package io.gravitee.reporter.file;

import io.gravitee.common.service.AbstractService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.reporter.file.config.FileReporterConfiguration;
import io.gravitee.reporter.file.formatter.Formatter;
import io.gravitee.reporter.file.formatter.FormatterFactory;
import io.gravitee.reporter.file.vertx.VertxFileWriter;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileReporter extends AbstractService implements Reporter {

    private final Logger logger = LoggerFactory.getLogger(FileReporter.class);

    @Autowired
    private Vertx vertx;

    @Autowired
    private FileReporterConfiguration configuration;

    @Value("${reporters.file.enabled:false}")
    private boolean enabled;

    private Map<Class<? extends Reportable>, VertxFileWriter> writers = new HashMap<>(4);

    @Override
    public void report(Reportable reportable) {
        writers.get(reportable.getClass()).write(reportable);
    }

    @Override
    public boolean canHandle(Reportable reportable) {
        return enabled && writers.containsKey(reportable.getClass());
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            // Initialize writers
            for (MetricsType type : MetricsType.values()) {
                Formatter formatter = FormatterFactory.getFormatter(configuration.getOutputType(), configuration.getRules(type));
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

            CompositeFuture
                .join(writers.values().stream().map(VertxFileWriter::initialize).collect(Collectors.toList()))
                .onComplete(
                    event -> {
                        if (event.succeeded()) {
                            logger.info("File reporter successfully started");
                        } else {
                            logger.info("An error occurs while starting file reporter", event.cause());
                        }
                    }
                );
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            CompositeFuture
                .join(writers.values().stream().map(VertxFileWriter::stop).collect(Collectors.toList()))
                .onComplete(
                    event -> {
                        if (event.succeeded()) {
                            logger.info("File reporter successfully stopped");
                        } else {
                            logger.info("An error occurs while stopping file reporter", event.cause());
                        }
                    }
                );
        }
    }
}
