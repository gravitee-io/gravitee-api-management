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
import { useCallback, useState } from 'react';

import { resolveHealthTimeRange, type HealthTimeRange, type Timeframe } from '../../../../utils/healthTimeframe';

/**
 * One absolute window for every health-check widget and page.
 * Recomputed when the timeframe changes and when the operator refreshes.
 */
export function useHealthCheckWindow(timeframe: Timeframe): { range: HealthTimeRange; refresh: () => void } {
    const [range, setRange] = useState(() => resolveHealthTimeRange(timeframe));
    const [appliedTimeframe, setAppliedTimeframe] = useState(timeframe);

    if (appliedTimeframe !== timeframe) {
        setAppliedTimeframe(timeframe);
        setRange(resolveHealthTimeRange(timeframe));
    }

    const refresh = useCallback(() => {
        setRange(resolveHealthTimeRange(timeframe));
    }, [timeframe]);

    return { range, refresh };
}
