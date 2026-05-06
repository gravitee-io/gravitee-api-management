/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Dashboard } from '@gravitee/gravitee-dashboard';

import { AnalyticsDashboardsResponse } from './analytics-dashboard';

export function fakeDashboard(modifier?: Partial<Dashboard>): Dashboard {
  return {
    id: 'dashboard-1',
    name: 'HTTP Overview',
    createdBy: 'admin',
    createdAt: '2026-01-01T00:00:00Z',
    lastModified: '2026-01-15T00:00:00Z',
    labels: {},
    widgets: [],
    ...modifier,
  };
}

export function fakeAnalyticsDashboardsResponse(modifier?: Partial<AnalyticsDashboardsResponse>): AnalyticsDashboardsResponse {
  return {
    data: [fakeDashboard(), fakeDashboard({ id: 'dashboard-2', name: 'API Performance' })],
    metadata: {
      pagination: {
        current_page: 1,
        total_pages: 1,
        total: 2,
        size: 10,
      },
    },
    ...modifier,
  };
}
