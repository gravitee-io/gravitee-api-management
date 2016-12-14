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
package io.gravitee.gateway.report.impl.lmax;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.gravitee.gateway.report.impl.ReporterServiceImpl;
import io.gravitee.reporter.api.Reportable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class LmaxReporterService extends ReporterServiceImpl implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(LmaxReporterService.class);

    @Autowired
    private Environment environment;

    private Disruptor<ReportableEvent> disruptor;

    @Override
    public void afterPropertiesSet() throws Exception {
        ReportableEventFactory factory = new ReportableEventFactory();
        int bufferSize = environment.getProperty("reporters.system.buffersize", int.class, 4096);

        disruptor = new Disruptor<>(factory, bufferSize, new ThreadFactory() {
            private int counter = 0;
            private static final String prefix = "reporter-disruptor";

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, prefix + '-' + counter++);
            }
        }, ProducerType.MULTI, new BlockingWaitStrategy());
    }

    @Override
    protected void doStop() throws Exception {
        try {
            logger.info("Shutdown LMAX reporter");
            disruptor.shutdown();
            super.doStop();
        } catch (Exception ex) {
            logger.error("Unexpected error", ex);
            throw new IllegalStateException("LMAX reporter should never go here !");
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        disruptor.handleEventsWith(
                (ReporterEventHandler []) getReporters().stream().map(ReporterEventHandler::new).collect(Collectors.toList()).toArray(new ReporterEventHandler[getReporters().size()]));

        logger.info("Start reportable event disruptor");
        disruptor.start();
    }

    @Override
    protected void doReport(Reportable reportable) {
        boolean eventWasPublished = disruptor.getRingBuffer().tryPublishEvent((reportableEvent, l) -> reportableEvent.setReportable(reportable));
        if(!eventWasPublished) {
        	logger.warn("A reportable event was dropped ! Check for slow reporter consumer or a too small {reporters.system.buffersize}, actual value = {}",
        			disruptor.getRingBuffer().getBufferSize());
        }
    }
}
