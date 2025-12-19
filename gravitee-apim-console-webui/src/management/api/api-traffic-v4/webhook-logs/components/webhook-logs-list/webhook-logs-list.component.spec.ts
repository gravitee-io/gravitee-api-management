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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { By } from '@angular/platform-browser';

import { WebhookLogsListComponent } from './webhook-logs-list.component';
import { WebhookLogsListHarness } from './webhook-logs-list.harness';

import { LogsListBaseComponent } from '../../../components/logs-list-base/logs-list-base.component';
import { Pagination } from '../../../../../../entities/management-api-v2';
import { WebhookAdditionalMetrics, WebhookLog } from '../../models/webhook-logs.models';
import { GioTestingModule } from '../../../../../../shared/testing';

type WebhookLogWithId = WebhookLog & { id: string };

describe('WebhookLogsListComponent', () => {
  let fixture: ComponentFixture<WebhookLogsListComponent>;
  let harness: WebhookLogsListHarness;

  const defaultLogs: WebhookLogWithId[] = [
    createWebhookLog({
      id: 'log-1',
      timestamp: '2024-06-01T12:00:00.000Z',
      status: 200,
      callbackUrl: 'https://callback-success.test',
      application: { id: 'app-1', name: 'First application', apiKeyMode: 'UNSPECIFIED' },
      additionalMetrics: createAdditionalMetrics({ bool_webhook_dlq: true }),
    }),
    createWebhookLog({
      id: 'log-2',
      timestamp: '2024-06-01T13:00:00.000Z',
      status: 502,
      callbackUrl: 'https://callback-error.test',
      application: { id: 'app-2', apiKeyMode: 'UNSPECIFIED' },
      additionalMetrics: createAdditionalMetrics({ bool_webhook_dlq: false }),
    }),
    createWebhookLog({
      id: 'log-3',
      timestamp: '2024-06-01T14:00:00.000Z',
      status: 400,
      callbackUrl: 'https://callback-dlq.test',
      application: { id: 'app-3', name: 'DLQ Test App', apiKeyMode: 'UNSPECIFIED' },
      additionalMetrics: createAdditionalMetrics({ bool_webhook_dlq: true }),
    }),
  ];

  const defaultPagination: Pagination = {
    page: 1,
    perPage: 10,
    pageCount: 1,
    pageItemsCount: defaultLogs.length,
    totalCount: defaultLogs.length,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsListComponent, GioTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsListComponent);
    fixture.componentRef.setInput('logs', defaultLogs);
    fixture.componentRef.setInput('pagination', defaultPagination);
    fixture.componentRef.setInput('hasDlqConfigured', true);
    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture<WebhookLogsListHarness>(fixture, WebhookLogsListHarness);
  });

  it('should render the table with expected columns and values', async () => {
    const { headerCells, rowCells } = await harness.computeTableCells();

    expect(headerCells[0]).toEqual({
      timestamp: 'Timestamp',
      status: 'Status',
      callbackUrl: 'Callback URL',
      application: 'Application',
      sentToDlq: 'Sent to DLQ',
      actions: '',
    });

    expect(rowCells).toHaveLength(3);
    expect(rowCells[0][1]).toBe('200');
    expect(rowCells[0][2]).toBe('https://callback-success.test');
    expect(rowCells[0][3]).toBe('First application');
    expect(rowCells[0][4]).toBe('—');

    expect(rowCells[1][1]).toBe('502');
    expect(rowCells[1][2]).toBe('https://callback-error.test');
    expect(rowCells[1][3]).toBe('—');
    expect(rowCells[1][4]).toBe('No');

    expect(rowCells[2][1]).toBe('400');
    expect(rowCells[2][2]).toBe('https://callback-dlq.test');
    expect(rowCells[2][3]).toBe('DLQ Test App');
    expect(rowCells[2][4]).toBe('Yes');
  });

  it('should emit log details when action button is clicked', async () => {
    const logDetailsSpy = jest.fn();
    const subscription = fixture.componentInstance.logDetailsClicked.subscribe(logDetailsSpy);

    await harness.clickDetailsButtonAtRow(1);

    expect(logDetailsSpy).toHaveBeenCalledWith(defaultLogs[1]);
    subscription.unsubscribe();
  });

  it('should forward pagination events emitted by the base logs list', () => {
    const paginationSpy = jest.fn();
    const subscription = fixture.componentInstance.paginationUpdated.subscribe(paginationSpy);

    const baseComponentDebug = fixture.debugElement.query(By.directive(LogsListBaseComponent));
    const baseComponent = baseComponentDebug.componentInstance as LogsListBaseComponent<WebhookLog>;

    baseComponent.paginationUpdated.emit({ index: 2, size: 25 });

    expect(paginationSpy).toHaveBeenCalledWith({ index: 2, size: 25 });
    subscription.unsubscribe();
  });

  it('should display "Yes" when bool_webhook_dlq is true and status is >= 400', async () => {
    const logsWithDlq: WebhookLogWithId[] = [
      createWebhookLog({
        id: 'log-dlq-1',
        timestamp: '2024-06-01T15:00:00.000Z',
        status: 400,
        callbackUrl: 'https://callback-400.test',
        application: { id: 'app-dlq', name: 'DLQ App', apiKeyMode: 'UNSPECIFIED' },
        additionalMetrics: createAdditionalMetrics({ bool_webhook_dlq: true }),
      }),
      createWebhookLog({
        id: 'log-dlq-2',
        timestamp: '2024-06-01T16:00:00.000Z',
        status: 0,
        callbackUrl: 'https://callback-0.test',
        application: { id: 'app-dlq-2', name: 'DLQ App 2', apiKeyMode: 'UNSPECIFIED' },
        additionalMetrics: createAdditionalMetrics({ bool_webhook_dlq: true }),
      }),
    ];

    fixture.componentRef.setInput('logs', logsWithDlq);
    fixture.detectChanges();

    const { rowCells } = await harness.computeTableCells();

    expect(rowCells).toHaveLength(2);
    // Status 400 with bool_webhook_dlq: true should show "Yes"
    expect(rowCells[0][4]).toBe('Yes');
    // Status 0 with bool_webhook_dlq: true should also show "Yes"
    expect(rowCells[1][4]).toBe('Yes');
  });

  it('should display "—" when bool_webhook_dlq is undefined', async () => {
    const baseMetrics = createAdditionalMetrics();
    // Remove bool_webhook_dlq to simulate it being undefined (not present in backend response)
    const { bool_webhook_dlq: _bool_webhook_dlq, ...metricsWithoutDlq } = baseMetrics;

    const logsWithoutDlq: WebhookLogWithId[] = [
      createWebhookLog({
        id: 'log-no-dlq',
        timestamp: '2024-06-01T17:00:00.000Z',
        status: 500,
        callbackUrl: 'https://callback-500.test',
        application: { id: 'app-no-dlq', name: 'No DLQ App', apiKeyMode: 'UNSPECIFIED' },
        additionalMetrics: metricsWithoutDlq as Partial<WebhookAdditionalMetrics>,
      }),
    ];

    fixture.componentRef.setInput('logs', logsWithoutDlq);
    fixture.detectChanges();

    const { rowCells } = await harness.computeTableCells();

    expect(rowCells).toHaveLength(1);
    // Status 500 but bool_webhook_dlq is undefined should show "—"
    expect(rowCells[0][4]).toBe('—');
  });

  function createWebhookLog(overrides: Partial<WebhookLogWithId>): WebhookLogWithId {
    return {
      id: 'log-id',
      apiId: 'api-id',
      requestId: 'request-id',
      timestamp: '2024-06-01T00:00:00.000Z',
      method: 'GET',
      status: 200,
      application: { id: 'app-id', name: 'Application', apiKeyMode: 'UNSPECIFIED' },
      plan: { id: 'plan-id', name: 'Plan name' },
      requestEnded: true,
      gatewayResponseTime: 120,
      uri: '/callback',
      endpoint: 'endpoint-1',
      message: '',
      errorKey: undefined,
      errorComponentName: undefined,
      errorComponentType: undefined,
      warnings: [],
      callbackUrl: 'https://callback.test',
      duration: '12ms',
      additionalMetrics: createAdditionalMetrics(),
      ...overrides,
    };
  }

  function createAdditionalMetrics(overrides: Partial<WebhookAdditionalMetrics> = {}): WebhookAdditionalMetrics {
    return { ...baseAdditionalMetrics(), ...overrides };
  }

  function baseAdditionalMetrics(): WebhookAdditionalMetrics {
    return {
      'string_webhook_req-method': 'GET',
      string_webhook_url: 'https://callback.test',
      'keyword_webhook_app-id': 'app-id',
      'keyword_webhook_sub-id': 'sub-id',
      'int_webhook_retry-count': 0,
      'json_webhook_retry-timeline': '[]',
      'long_webhook_req-timestamp': Date.now(),
      'int_webhook_resp-status': 200,
      bool_webhook_dlq: false,
    };
  }
});
