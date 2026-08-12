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

import { render, screen } from '@testing-library/react';

import { GatewayInstanceMonitoringView } from './GatewayInstanceMonitoringView';
import type { MonitoringData } from '../types/instance';

const MONITORING_DATA = {
    cpu: { percent_use: 12, load_average: { '1m': 0.5, '5m': 0.4 } },
    process: { cpu_percent: 10, open_file_descriptors: 100, max_file_descriptors: 1024 },
    jvm: {
        timestamp: 1_700_000_000_000,
        uptime_in_millis: 60_000,
        heap_used_in_bytes: 100,
        heap_used_percent: 50,
        heap_committed_in_bytes: 200,
        heap_max_in_bytes: 400,
        non_heap_used_in_bytes: 50,
        non_heap_committed_in_bytes: 75,
        young_pool_used_in_bytes: 10,
        young_pool_max_in_bytes: 20,
        young_pool_peak_used_in_bytes: 15,
        young_pool_peak_max_in_bytes: 20,
        survivor_pool_used_in_bytes: 5,
        survivor_pool_max_in_bytes: 10,
        survivor_pool_peak_used_in_bytes: 6,
        survivor_pool_peak_max_in_bytes: 10,
        old_pool_used_in_bytes: 30,
        old_pool_max_in_bytes: 60,
        old_pool_peak_used_in_bytes: 40,
        old_pool_peak_max_in_bytes: 60,
    },
    thread: { count: 42, peak_count: 55 },
    gc: {
        young_collection_count: 1,
        young_collection_time_in_millis: 2,
        old_collection_count: 3,
        old_collection_time_in_millis: 4,
    },
} satisfies MonitoringData;

describe('GatewayInstanceMonitoringView', () => {
    it('renders hero indicators and classic monitoring sections', () => {
        render(<GatewayInstanceMonitoringView data={MONITORING_DATA} />);

        expect(screen.getByTestId('gateway-instance-monitoring')).not.toBeNull();
        expect(screen.getByLabelText('10%')).not.toBeNull();
        expect(screen.getByLabelText('50%')).not.toBeNull();
        expect(screen.getByText('GC collections')).not.toBeNull();
        expect(screen.getByText('File Descriptors')).not.toBeNull();

        expect(screen.getByTestId('instance-monitoring_jvm-box')).not.toBeNull();
        expect(screen.getByText('JVM')).not.toBeNull();
        expect(screen.getByText('Young pool used')).not.toBeNull();
        expect(screen.getByText('Survivor pool used')).not.toBeNull();
        expect(screen.getByText('Old pool used')).not.toBeNull();

        expect(screen.getByTestId('instance-monitoring_cpu-box')).not.toBeNull();
        expect(screen.getByTestId('instance-monitoring_process-box')).not.toBeNull();
        expect(screen.getByTestId('instance-monitoring_thread-box')).not.toBeNull();
        expect(screen.getByTestId('instance-monitoring_gc-box')).not.toBeNull();
        expect(screen.getByText('[1m]')).not.toBeNull();
        expect(screen.getByText('Young collection time')).not.toBeNull();
        expect(screen.getByText('2 ms')).not.toBeNull();
        expect(screen.getByText('Old collection time')).not.toBeNull();
        expect(screen.getByText('4 ms')).not.toBeNull();
        expect(screen.getAllByTestId('monitoring-progress-bar')).toHaveLength(6);
    });
});
