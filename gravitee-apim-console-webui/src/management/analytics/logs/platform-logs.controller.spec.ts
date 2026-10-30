/*
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
import { ActivatedRoute, Router } from '@angular/router';

import PlatformLogsController from './platform-logs.controller';

import AnalyticsService from '../../../services/analytics.service';
import { ApiService } from '../../../services/api.service';
import ApplicationService from '../../../services/application.service';

describe('PlatformLogsController', () => {
  const activatedRoute = { snapshot: { queryParams: {} } } as unknown as ActivatedRoute;
  const ngRouter = { navigate: jest.fn() } as unknown as Router;

  const buildController = (analyticsService: Partial<AnalyticsService>): PlatformLogsController => {
    const controller = new PlatformLogsController(
      {} as ApiService,
      analyticsService as AnalyticsService,
      {},
      {} as ApplicationService,
      ngRouter,
      { $watch: jest.fn() } as any,
    );
    (controller as any).activatedRoute = activatedRoute;
    return controller;
  };

  // Regression for APIM-15676: a failed log search must not leave the previous search's results on screen.
  it('should clear the results table when the log search fails', async () => {
    const controller = buildController({ findLogs: jest.fn().mockRejectedValue(new Error('timeout')) });
    (controller as any).logs = { total: '1000', logs: [{ id: 'stale' }], metadata: {} };

    controller.refresh();
    await new Promise(process.nextTick);

    expect((controller as any).logs).toEqual({ total: '0', logs: [], metadata: {} });
  });

  it('should display the results when the log search succeeds', async () => {
    const data = { total: '2', logs: [{ id: 'a' }, { id: 'b' }], metadata: {} };
    const controller = buildController({ findLogs: jest.fn().mockResolvedValue({ data }) });

    controller.refresh();
    await new Promise(process.nextTick);

    expect((controller as any).logs).toEqual(data);
  });
});
