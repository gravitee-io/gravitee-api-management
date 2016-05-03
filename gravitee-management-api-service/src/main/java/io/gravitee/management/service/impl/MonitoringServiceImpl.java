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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.monitoring.*;
import io.gravitee.management.service.MonitoringService;
import io.gravitee.repository.monitoring.MonitoringRepository;
import io.gravitee.repository.monitoring.model.MonitoringResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author GraviteeSource Team
 */
@Service
public class MonitoringServiceImpl implements MonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringServiceImpl.class);

    @Inject
    private MonitoringRepository monitoringRepository;

    @Override
    public MonitoringData findMonitoring(final String gatewayId) {
        LOGGER.debug("Running monitoring query for Gateway instance '{}'", gatewayId);
        final MonitoringResponse monitoringResponse = monitoringRepository.query(gatewayId);
        return convert(monitoringResponse);
    }

    private MonitoringData convert(final MonitoringResponse monitoringResponse) {
        final MonitoringData monitoringData = new MonitoringData();
        if (monitoringResponse == null) {
            return monitoringData;
        }
        monitoringData.setCpu(convertCPU(monitoringResponse));
        monitoringData.setGc(convertGC(monitoringResponse));
        monitoringData.setJvm(convertJVM(monitoringResponse));
        monitoringData.setProcess(convertProcess(monitoringResponse));
        monitoringData.setThread(convertThread(monitoringResponse));
        return monitoringData;
    }

    private MonitoringCPU convertCPU(MonitoringResponse monitoringResponse) {
        final MonitoringCPU monitoringCPU = new MonitoringCPU();
        monitoringCPU.setLoadAverage(monitoringResponse.getOsCPULoadAverage());
        monitoringCPU.setPercentUse(monitoringResponse.getOsCPUPercent());
        return monitoringCPU;
    }

    private MonitoringGC convertGC(MonitoringResponse monitoringResponse) {
        final MonitoringGC monitoringGC = new MonitoringGC();
        monitoringGC.setOldCollectionCount(monitoringResponse.getJvmGCCollectorsOldCollectionCount());
        monitoringGC.setOldCollectionTimeInMillis(monitoringResponse.getJvmGCCollectorsOldCollectionTimeInMillis());
        monitoringGC.setYoungCollectionCount(monitoringResponse.getJvmGCCollectorsYoungCollectionCount());
        monitoringGC.setYoungCollectionTimeInMillis(monitoringResponse.getJvmGCCollectorsYoungCollectionTimeInMillis());
        return monitoringGC;
    }

    private MonitoringJVM convertJVM(MonitoringResponse monitoringResponse) {
        final MonitoringJVM monitoringJVM = new MonitoringJVM();
        monitoringJVM.setTimestamp(monitoringResponse.getJvmTimestamp());
        monitoringJVM.setHeapCommittedInBytes(monitoringResponse.getJvmHeapCommittedInBytes());
        monitoringJVM.setHeapMaxInBytes(monitoringResponse.getJvmHeapMaxInBytes());
        monitoringJVM.setHeapUsedInBytes(monitoringResponse.getJvmHeapUsedInBytes());
        monitoringJVM.setHeapUsedPercent(monitoringResponse.getJvmHeapUsedPercent());
        monitoringJVM.setNonHeapCommittedInBytes(monitoringResponse.getJvmNonHeapCommittedInBytes());
        monitoringJVM.setNonHeapUsedInBytes(monitoringResponse.getJvmNonHeapUsedInBytes());
        monitoringJVM.setUptimeInMillis(monitoringResponse.getJvmUptimeInMillis());

        monitoringJVM.setYoungPoolMaxInBytes(monitoringResponse.getJvmMemPoolYoungMaxInBytes());
        monitoringJVM.setYoungPoolPeakMaxInBytes(monitoringResponse.getJvmMemPoolYoungPeakMaxInBytes());
        monitoringJVM.setYoungPoolPeakUsedInBytes(monitoringResponse.getJvmMemPoolYoungPeakUsedInBytes());
        monitoringJVM.setYoungPoolUsedInBytes(monitoringResponse.getJvmMemPoolYoungUsedInBytes());

        monitoringJVM.setSurvivorPoolMaxInBytes(monitoringResponse.getJvmMemPoolSurvivorMaxInBytes());
        monitoringJVM.setSurvivorPoolPeakMaxInBytes(monitoringResponse.getJvmMemPoolSurvivorPeakMaxInBytes());
        monitoringJVM.setSurvivorPoolPeakUsedInBytes(monitoringResponse.getJvmMemPoolSurvivorPeakUsedInBytes());
        monitoringJVM.setSurvivorPoolUsedInBytes(monitoringResponse.getJvmMemPoolSurvivorUsedInBytes());

        monitoringJVM.setOldPoolMaxInBytes(monitoringResponse.getJvmMemPoolOldMaxInBytes());
        monitoringJVM.setOldPoolPeakMaxInBytes(monitoringResponse.getJvmMemPoolOldPeakMaxInBytes());
        monitoringJVM.setOldPoolPeakUsedInBytes(monitoringResponse.getJvmMemPoolOldPeakUsedInBytes());
        monitoringJVM.setOldPoolUsedInBytes(monitoringResponse.getJvmMemPoolOldUsedInBytes());

        return monitoringJVM;
    }

    private MonitoringProcess convertProcess(MonitoringResponse monitoringResponse) {
        final MonitoringProcess monitoringProcess = new MonitoringProcess();
        monitoringProcess.setOpenFileDescriptors(monitoringResponse.getJvmProcessOpenFileDescriptors());
        monitoringProcess.setMaxFileDescriptors(monitoringResponse.getJvmProcessMaxFileDescriptors());
        return monitoringProcess;
    }

    private MonitoringThread convertThread(MonitoringResponse monitoringResponse) {
        final MonitoringThread monitoringThread = new MonitoringThread();
        monitoringThread.setCount(monitoringResponse.getJvmThreadCount());
        monitoringThread.setPeakCount(monitoringResponse.getJvmThreadPeakCount());
        return monitoringThread;
    }
}
