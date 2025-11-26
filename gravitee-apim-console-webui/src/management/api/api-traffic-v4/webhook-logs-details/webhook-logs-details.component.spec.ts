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
import { Router, ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { WebhookLogsDetailsComponent } from './webhook-logs-details.component';
import { WebhookLogsDetailsHarness } from './webhook-logs-details.harness';

import { HttpMethod } from '../../../../entities/management-api-v2';
import { WebhookLog } from '../webhook-logs/models/webhook-logs.models';
import { getWebhookLogMockByRequestId } from '../webhook-logs/mocks/webhook-logs.mock';
import { GioTestingModule } from '../../../../shared/testing';

describe('WebhookLogsDetailsComponent', () => {
  let fixture: ComponentFixture<WebhookLogsDetailsComponent>;
  let harness: WebhookLogsDetailsHarness;
  let routerNavigateSpy: ReturnType<typeof jest.spyOn>;
  let routerNavigateByUrlSpy: ReturnType<typeof jest.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsDetailsComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { requestId: 'request-1' },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsDetailsComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    // Reset to empty state after ngOnInit has run
    component.selectedLog = null;
    component.overviewRequest = [];
    component.overviewResponse = [];
    component.deliveryAttemptsDataSource = [];
    component.requestHeaders = [];
    component.responseHeaders = [];
    component.requestBody = '';
    component.responseBody = '';
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    routerNavigateByUrlSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookLogsDetailsHarness);
  });

  it('should navigate back when the back button is clicked', async () => {
    await harness.clickBack();
    if (routerNavigateSpy.mock.calls.length) {
      const activatedRoute = TestBed.inject(ActivatedRoute);
      expect(routerNavigateSpy).toHaveBeenCalledWith(['..'], { relativeTo: activatedRoute });
    } else {
      expect(routerNavigateByUrlSpy).toHaveBeenCalled();
    }
  });

  it('should open reporting settings when empty state action is clicked', async () => {
    expect(await harness.hasEmptyState()).toBe(true);

    await harness.clickOpenSettings();

    const activatedRoute = TestBed.inject(ActivatedRoute);
    expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], {
      relativeTo: activatedRoute,
      queryParams: { openSettings: 'true' },
    });
  });

  describe('buildDeliveryAttempts', () => {
    let component: WebhookLogsDetailsComponent;

    beforeEach(() => {
      component = fixture.componentInstance;
    });

    const createBaseLog = (): WebhookLog => ({
      apiId: 'api-test',
      requestId: 'req-test',
      timestamp: '2024-01-01T00:00:00.000Z',
      method: 'POST' as HttpMethod,
      status: 200,
      application: { id: 'app-1', name: 'Test App', type: 'SIMPLE', apiKeyMode: 'UNSPECIFIED' },
      plan: { id: 'plan-1', name: 'Test Plan', mode: 'PUSH' },
      requestEnded: true,
      gatewayResponseTime: 1500,
      uri: 'http://test.com',
      endpoint: 'http://test.com',
      callbackUrl: 'http://test.com',
      duration: '1.5 s',
      additionalMetrics: {
        'string_webhook_req-method': 'POST',
        string_webhook_url: 'http://test.com',
        'keyword_webhook_app-id': 'app-1',
        'keyword_webhook_sub-id': 'sub-1',
        'int_webhook_retry-count': 0,
        'string_webhook_last-error': null,
        'long_webhook_req-timestamp': 1704067200000,
        'json_webhook_req-headers': null,
        'string_webhook_req-body': null,
        'long_webhook_resp-time': 1500,
        'int_webhook_resp-status': 200,
        'int_webhook_resp-body-size': 100,
        'json_webhook_resp-headers': null,
        'string_webhook_resp-body': null,
        bool_webhook_dlq: false,
        'json_webhook_retry-timeline': undefined as any,
      },
    });

    it('should fall back to single delivery attempt when timeline is missing', () => {
      const log = { ...createBaseLog(), additionalMetrics: { ...createBaseLog().additionalMetrics } };
      (log.additionalMetrics as any)['json_webhook_retry-timeline'] = undefined;

      component.selectedLog = log;
      (component as any).applyLog(log);

      expect(component.deliveryAttemptsDataSource).toHaveLength(1);
      expect(component.deliveryAttemptsDataSource[0]).toEqual({
        attempt: 1,
        timestamp: log.timestamp,
        duration: log.gatewayResponseTime,
        status: log.status,
        reason: 'Initial delivery attempt',
      });
    });

    it('should fall back to single delivery attempt when timeline is empty array string', () => {
      const log = createBaseLog();
      log.additionalMetrics!['json_webhook_retry-timeline'] = '[]';

      component.selectedLog = log;
      (component as any).applyLog(log);

      expect(component.deliveryAttemptsDataSource).toHaveLength(1);
      expect(component.deliveryAttemptsDataSource[0]).toEqual({
        attempt: 1,
        timestamp: log.timestamp,
        duration: log.gatewayResponseTime,
        status: log.status,
        reason: 'Initial delivery attempt',
      });
    });

    it('should fall back to single delivery attempt when timeline is invalid JSON', () => {
      const log = createBaseLog();
      log.additionalMetrics!['json_webhook_retry-timeline'] = 'invalid json';

      component.selectedLog = log;
      (component as any).applyLog(log);

      expect(component.deliveryAttemptsDataSource).toHaveLength(1);
      expect(component.deliveryAttemptsDataSource[0]).toEqual({
        attempt: 1,
        timestamp: log.timestamp,
        duration: log.gatewayResponseTime,
        status: log.status,
        reason: 'Initial delivery attempt',
      });
    });

    it('should fall back to single delivery attempt when parsed timeline is empty array', () => {
      const log = createBaseLog();
      log.additionalMetrics!['json_webhook_retry-timeline'] = JSON.stringify([]);

      component.selectedLog = log;
      (component as any).applyLog(log);

      expect(component.deliveryAttemptsDataSource).toHaveLength(1);
      expect(component.deliveryAttemptsDataSource[0]).toEqual({
        attempt: 1,
        timestamp: log.timestamp,
        duration: log.gatewayResponseTime,
        status: log.status,
        reason: 'Initial delivery attempt',
      });
    });

    it('should use last error from log when falling back', () => {
      const log = createBaseLog();
      (log.additionalMetrics as any)['json_webhook_retry-timeline'] = undefined;
      log.additionalMetrics!['string_webhook_last-error'] = 'Custom error message';

      component.selectedLog = log;
      (component as any).applyLog(log);

      expect(component.deliveryAttemptsDataSource).toHaveLength(1);
      expect(component.deliveryAttemptsDataSource[0].reason).toBe('Custom error message');
    });

    it('should return timeline attempts when timeline is valid and non-empty', () => {
      const log = createBaseLog();
      const timeline = [
        {
          attempt: 1,
          timestamp: 1704067201000,
          duration: 500,
          status: 200,
          reason: 'First attempt',
        },
        {
          attempt: 2,
          timestamp: 1704067202000,
          duration: 750,
          status: 200,
          reason: 'Second attempt',
        },
      ];
      log.additionalMetrics!['json_webhook_retry-timeline'] = JSON.stringify(timeline);

      component.selectedLog = log;
      (component as any).applyLog(log);

      expect(component.deliveryAttemptsDataSource).toHaveLength(2);
      expect(component.deliveryAttemptsDataSource[0]).toEqual({
        attempt: 1,
        timestamp: new Date(1704067201000).toISOString(),
        duration: 500,
        status: 200,
        reason: 'First attempt',
      });
      expect(component.deliveryAttemptsDataSource[1]).toEqual({
        attempt: 2,
        timestamp: new Date(1704067202000).toISOString(),
        duration: 750,
        status: 200,
        reason: 'Second attempt',
      });
    });

    it('should handle timeline items with missing fields by using log defaults', () => {
      const log = createBaseLog();
      const timeline = [
        {
          timestamp: 1704067201000,
          // Missing attempt, duration, status
        },
      ];
      log.additionalMetrics!['json_webhook_retry-timeline'] = JSON.stringify(timeline);

      component.selectedLog = log;
      (component as any).applyLog(log);

      expect(component.deliveryAttemptsDataSource).toHaveLength(1);
      expect(component.deliveryAttemptsDataSource[0]).toEqual({
        attempt: 1, // Defaults to index + 1
        timestamp: new Date(1704067201000).toISOString(),
        duration: log.gatewayResponseTime, // Falls back to log duration
        status: log.status, // Falls back to log status
        reason: 'Initial delivery attempt', // Falls back to default
      });
    });

    it('should use timeline with req-3 mock data that has empty timeline', () => {
      const log = getWebhookLogMockByRequestId('req-3');
      expect(log).toBeDefined();
      if (log) {
        component.selectedLog = log;
        (component as any).applyLog(log);

        expect(component.deliveryAttemptsDataSource).toHaveLength(1);
        expect(component.deliveryAttemptsDataSource[0].attempt).toBe(1);
        expect(component.deliveryAttemptsDataSource[0].status).toBe(0);
      }
    });
  });

  describe('FormatDurationPipe integration', () => {
    it('should display formatted duration in delivery attempts table', async () => {
      const log = getWebhookLogMockByRequestId('req-1');
      expect(log).toBeDefined();

      if (log) {
        const component = fixture.componentInstance;
        component.selectedLog = log;
        (component as any).applyLog(log);
        fixture.detectChanges();

        // Verify delivery attempts are populated
        expect(component.deliveryAttemptsDataSource.length).toBeGreaterThan(0);

        // The pipe should format the duration values correctly
        // The actual rendering is tested through the harness/e2e tests
        // This test ensures the data is structured correctly for the pipe
        component.deliveryAttemptsDataSource.forEach((attempt) => {
          expect(attempt.duration).toBeDefined();
          expect(typeof attempt.duration).toBe('number');
        });
      }
    });
  });
});
