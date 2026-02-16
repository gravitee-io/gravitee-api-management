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

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, firstValueFrom } from 'rxjs';
import { HarnessLoader } from '@angular/cdk/testing';

import { WebhookLogsComponent } from './webhook-logs.component';
import { WebhookLogsHarness } from './webhook-logs.harness';
import { WebhookSettingsDialogComponent } from './components/webhook-settings-dialog/webhook-settings-dialog.component';
import { WebhookSettingsDialogHarness } from './components/webhook-settings-dialog/webhook-settings-dialog.harness';
import { fakeWebhookLogsResponse, fakeWebhookLog } from './webhook-logs.fixture';
import { WebhookLogsResponse } from './models/webhook-logs.models';

import { ApiV4, ConnectionLog, ApiSubscriptionsResponse, Subscription } from '../../../../entities/management-api-v2';
import { Constants } from '../../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeConnectionLog } from '../../../../entities/management-api-v2/log/connectionLog.fixture';

const API_ID = 'api-test-id';
const defaultApi = {
  id: API_ID,
  analytics: { enabled: true, logging: { mode: { endpoint: true } } },
  definitionVersion: 'V4',
} as ApiV4;

describe('WebhookLogsComponent', () => {
  let fixture: ComponentFixture<WebhookLogsComponent>;
  let harness: WebhookLogsHarness;
  let routerNavigateSpy: jest.SpyInstance;
  let httpTestingController: HttpTestingController;
  let activatedRoute: ActivatedRoute;
  let rootLoader: HarnessLoader;

  const setupComponent = async (options?: { queryParams?: Record<string, string | undefined>; api?: ApiV4 }) => {
    const { queryParams = {}, api = defaultApi } = options ?? {};

    TestBed.configureTestingModule({
      imports: [WebhookLogsComponent, WebhookSettingsDialogComponent, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: queryParams,
              queryParamMap: convertToParamMap(queryParams),
            },
            params: of({ apiId: API_ID }),
            queryParams: of(queryParams),
          },
        },
        { provide: Constants, useValue: CONSTANTS_TESTING },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    await TestBed.compileComponents();

    fixture = TestBed.createComponent(WebhookLogsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    activatedRoute = TestBed.inject(ActivatedRoute);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true as any);

    fixture.detectChanges();

    expectApi(api || defaultApi);

    expectSubscriptions();
    expectWebhookLogs();

    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookLogsHarness);
  };

  function expectApi(api: ApiV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env?.v2BaseURL}/apis/${API_ID}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectSubscriptions() {
    const mockSubscriptions: ApiSubscriptionsResponse = {
      data: [
        {
          id: 'sub-1',
          consumerConfiguration: {
            entrypointId: 'webhook',
            entrypointConfiguration: {
              callbackUrl: 'https://webhook.site/test-1',
            },
          },
        } as Subscription,
        {
          id: 'sub-2',
          consumerConfiguration: {
            entrypointId: 'webhook',
            entrypointConfiguration: {
              callbackUrl: 'https://webhook.site/test-2',
            },
          },
        } as Subscription,
      ],
      pagination: {
        page: 1,
        perPage: 1000,
        pageCount: 1,
        pageItemsCount: 2,
        totalCount: 2,
      },
    };

    httpTestingController
      .expectOne(req => {
        const baseUrl = `${CONSTANTS_TESTING.env?.v2BaseURL}/apis/${API_ID}/subscriptions`;
        return (
          req.method === 'GET' &&
          req.url === baseUrl &&
          req.params.get('page') === '1' &&
          req.params.get('perPage') === '1000' &&
          req.params.get('statuses') === 'ACCEPTED,PENDING,PAUSED'
        );
      })
      .flush(mockSubscriptions);
  }

  function expectWebhookLogs(
    total: number = 3,
    param: {
      page?: number;
      perPage?: number;
      statuses?: string;
      applicationIds?: string;
      callbackUrls?: string;
      from?: number;
      to?: number;
    } = {},
  ) {
    const page = param.page ?? 1;
    const perPage = param.perPage ?? 10;
    const itemsInPage = total < perPage ? total : perPage;

    const webhookLogsResponse = fakeWebhookLogsResponse();
    const mockConnectionLogs: ConnectionLog[] = [];
    for (let i = 0; i < itemsInPage; i++) {
      const webhookLog = webhookLogsResponse.data[i] || webhookLogsResponse.data[0];
      const additionalMetrics: { [key: string]: string } = {};
      if (webhookLog.additionalMetrics) {
        Object.entries(webhookLog.additionalMetrics).forEach(([key, value]) => {
          if (value !== null && value !== undefined) {
            additionalMetrics[key] = String(value);
          }
        });
      }

      mockConnectionLogs.push(
        fakeConnectionLog({
          apiId: webhookLog.apiId,
          requestId: webhookLog.requestId,
          timestamp: webhookLog.timestamp,
          method: webhookLog.method,
          status: webhookLog.status,
          application: webhookLog.application,
          plan: webhookLog.plan,
          requestEnded: webhookLog.requestEnded,
          gatewayResponseTime: webhookLog.gatewayResponseTime,
          uri: webhookLog.uri,
          endpoint: webhookLog.endpoint,
          message: webhookLog.message,
          errorKey: webhookLog.errorKey,
          errorComponentName: webhookLog.errorComponentName,
          errorComponentType: webhookLog.errorComponentType,
          warnings: webhookLog.warnings,
          additionalMetrics: Object.keys(additionalMetrics).length > 0 ? additionalMetrics : undefined,
        }),
      );
    }

    const baseUrl = `${CONSTANTS_TESTING.env?.v2BaseURL}/apis/${API_ID}/logs/messages`;

    // Use match instead of expectOne to handle multiple matching requests
    const matchingRequests = httpTestingController.match(req => {
      // Check base URL and method
      if (req.method !== 'GET' || !req.url.startsWith(baseUrl)) {
        return false;
      }

      if (req.params.get('page') !== String(page)) return false;
      if (req.params.get('perPage') !== String(perPage)) return false;
      if (req.params.get('connectorId') !== 'webhook') return false;
      if (req.params.get('connectorType') !== 'entrypoint') return false;
      if (req.params.get('operation') !== 'subscribe') return false;

      const hasFrom = req.params.get('from') !== null;
      const hasTo = req.params.get('to') !== null;
      if (param.from !== undefined && !hasFrom) return false;
      if (param.from === undefined && hasFrom) return false;
      if (param.from !== undefined && req.params.get('from') !== String(param.from)) return false;
      if (param.to !== undefined && !hasTo) return false;
      if (param.to === undefined && hasTo) return false;
      if (param.to !== undefined && req.params.get('to') !== String(param.to)) return false;

      // Parse additional parameters in format: additional=fieldName;value1,value2
      const additionalParams = req.params.getAll('additional') || [];
      const additionalMap = new Map<string, string>();
      additionalParams.forEach(param => {
        const [fieldName, values] = param.split(';', 2);
        if (fieldName && values) {
          additionalMap.set(fieldName.trim(), values.trim());
        }
      });

      if (param.statuses) {
        const expectedValue = param.statuses;
        const actualValue = additionalMap.get('int_webhook_resp-status');
        if (!actualValue || actualValue !== expectedValue) return false;
      } else {
        if (additionalMap.has('int_webhook_resp-status')) return false;
      }

      if (param.applicationIds) {
        const expectedValue = param.applicationIds;
        const actualValue = additionalMap.get('keyword_webhook_app-id');
        if (!actualValue || actualValue !== expectedValue) return false;
      } else {
        if (additionalMap.has('keyword_webhook_app-id')) return false;
      }

      if (param.callbackUrls) {
        const expectedValue = param.callbackUrls;
        const actualValue = additionalMap.get('string_webhook_url');
        if (!actualValue || actualValue !== expectedValue) return false;
      } else {
        if (additionalMap.has('string_webhook_url')) return false;
      }

      if (!param.statuses && !param.applicationIds && !param.callbackUrls) {
        if (additionalMap.size > 0) return false;
      }

      return true;
    });

    matchingRequests.forEach(request => {
      if (!request.cancelled) {
        request.flush({
          data: mockConnectionLogs,
          pagination: {
            totalCount: total,
            page: page,
            perPage: perPage,
            pageCount: Math.ceil(total / perPage),
            pageItemsCount: itemsInPage,
          },
        });
      }
    });
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController?.verify();
    routerNavigateSpy?.mockRestore();
  });

  it('should render demo logs and navigate to the details page when clicking the details action', async () => {
    await setupComponent();

    await fixture.whenStable();
    fixture.detectChanges();

    const logsListHarness = await harness.getLogsList();
    expect(logsListHarness).not.toBeNull();
    expect(await logsListHarness!.countRows()).toBe(3);

    await logsListHarness!.clickDetailsButtonAtRow(0);
    await fixture.whenStable();

    expect(routerNavigateSpy).toHaveBeenCalled();
    const navigateCall = routerNavigateSpy.mock.calls[0];
    expect(navigateCall[0]).toEqual(['./', 'req-1']);
    expect(navigateCall[1]).toEqual(
      expect.objectContaining({
        relativeTo: activatedRoute,
        state: expect.objectContaining({
          webhookLog: expect.objectContaining({
            requestId: 'req-1',
            apiId: 'api-mock',
            status: 500,
          }),
        }),
      }),
    );
  });

  it('should filter logs when quick filters change', async () => {
    await setupComponent();

    await fixture.whenStable();
    fixture.detectChanges();

    const logsListHarness = await harness.getLogsList();
    expect(await logsListHarness!.countRows()).toBe(3);

    httpTestingController.verify();

    fixture.componentInstance.onFiltersChanged({ statuses: [500] });
    fixture.detectChanges();

    expectWebhookLogs(3, { page: 1, perPage: 10, statuses: '500' });

    await fixture.whenStable();
    fixture.detectChanges();

    expect(await logsListHarness!.countRows()).toBe(3);
  });

  it('should open the settings dialog when clicking the Configure Webhook Reporting button', async () => {
    await setupComponent();

    await harness.clickConfigureReporting();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const dialog = await rootLoader.getHarness(WebhookSettingsDialogHarness);
    expect(dialog).not.toBeNull();
  });

  it('should show the reporting disabled banner when entrypoint logging is disabled', async () => {
    const disabledApi = {
      ...defaultApi,
      listeners: [
        {
          type: 'SUBSCRIPTION',
          entrypoints: [
            {
              type: 'webhook',
              configuration: {
                logging: {
                  enabled: false,
                },
              },
            },
          ],
        },
      ],
    } as ApiV4;

    await setupComponent({ api: disabledApi });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('reporting-disabled-banner')).not.toBeNull();
  });

  describe('addApplicationNameToLogs', () => {
    it('should enrich logs with application names when application ID exists but name is missing', async () => {
      await setupComponent();

      const logsWithoutNames = fakeWebhookLogsResponse({
        data: [
          fakeWebhookLog({
            application: {
              id: 'app-1',
              apiKeyMode: 'UNSPECIFIED',
            },
          }),
          fakeWebhookLog({
            application: {
              id: 'app-2',
              apiKeyMode: 'UNSPECIFIED',
            },
          }),
        ],
      });

      const mockApplications = {
        data: [
          { id: 'app-1', name: 'Application One' },
          { id: 'app-2', name: 'Application Two' },
        ],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 2, totalCount: 2 },
      };

      const component = fixture.componentInstance;
      const resultObservable = (component as any).addApplicationNameToLogs(logsWithoutNames);

      const resultPromise = firstValueFrom(resultObservable);

      await new Promise(resolve => setTimeout(resolve, 0));

      const matchingRequests = httpTestingController.match(req => {
        const ids = req.params.getAll('ids');
        return (
          req.method === 'GET' &&
          req.url.includes(`${CONSTANTS_TESTING.env?.baseURL}/applications/_paged`) &&
          ids.includes('app-1') &&
          ids.includes('app-2')
        );
      });

      expect(matchingRequests.length).toBeGreaterThan(0);
      matchingRequests[0].flush(mockApplications);

      const result = (await resultPromise) as WebhookLogsResponse;

      expect(result.data[0].application?.name).toBe('Application One');
      expect(result.data[1].application?.name).toBe('Application Two');
    });

    it('should not enrich logs when application names already exist', async () => {
      await setupComponent();

      const logsWithNames = fakeWebhookLogsResponse({
        data: [
          fakeWebhookLog({
            application: {
              id: 'app-1',
              name: 'Existing Name',
              apiKeyMode: 'UNSPECIFIED',
            },
          }),
        ],
      });

      const component = fixture.componentInstance;
      const result = (await firstValueFrom((component as any).addApplicationNameToLogs(logsWithNames))) as WebhookLogsResponse;

      httpTestingController.expectNone(req => req.url.includes('/applications/_paged'));

      expect(result.data[0].application?.name).toBe('Existing Name');
    });

    it('should not enrich logs when no applications are present', async () => {
      await setupComponent();

      httpTestingController.verify();

      const baseLog = fakeWebhookLog();
      const { 'keyword_webhook_app-id': _, ...metricsWithoutAppId } = baseLog.additionalMetrics || {};

      const logsWithoutApps = fakeWebhookLogsResponse({
        data: [
          fakeWebhookLog({
            application: undefined,
            additionalMetrics: metricsWithoutAppId as any,
          }),
        ],
      });

      const component = fixture.componentInstance;
      const result = (await firstValueFrom((component as any).addApplicationNameToLogs(logsWithoutApps))) as WebhookLogsResponse;

      httpTestingController.expectNone(req => req.url.includes('/applications/_paged'));

      expect(result.data[0].application).toBeUndefined();
    });

    it('should enrich logs using additionalMetrics when application object is missing', async () => {
      await setupComponent();

      const logsWithAppIdInMetrics = fakeWebhookLogsResponse({
        data: [
          fakeWebhookLog({
            application: undefined,
            additionalMetrics: {
              'keyword_webhook_app-id': 'app-3',
            } as any,
          }),
        ],
      });

      const mockApplications = {
        data: [{ id: 'app-3', name: 'Application From Metrics' }],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      };

      const component = fixture.componentInstance;
      const resultObservable = (component as any).addApplicationNameToLogs(logsWithAppIdInMetrics);

      const resultPromise = firstValueFrom(resultObservable);

      await new Promise(resolve => setTimeout(resolve, 0));

      const matchingRequests = httpTestingController.match(req => {
        const ids = req.params.getAll('ids');
        return req.method === 'GET' && req.url.includes(`${CONSTANTS_TESTING.env?.baseURL}/applications/_paged`) && ids.includes('app-3');
      });

      expect(matchingRequests.length).toBeGreaterThan(0);
      matchingRequests[0].flush(mockApplications);

      const result = (await resultPromise) as WebhookLogsResponse;

      expect(result.data[0].application?.id).toBe('app-3');
      expect(result.data[0].application?.name).toBe('Application From Metrics');
      expect(result.data[0].application?.apiKeyMode).toBe('UNSPECIFIED');
    });

    it('should return logs unchanged when application service fails', async () => {
      await setupComponent();

      const logsWithoutNames = fakeWebhookLogsResponse({
        data: [
          fakeWebhookLog({
            application: {
              id: 'app-1',
              apiKeyMode: 'UNSPECIFIED',
            },
          }),
        ],
      });

      const component = fixture.componentInstance;
      const resultObservable = (component as any).addApplicationNameToLogs(logsWithoutNames);

      const resultPromise = firstValueFrom(resultObservable);

      await new Promise(resolve => setTimeout(resolve, 0));

      const matchingRequests = httpTestingController.match(req => {
        return req.method === 'GET' && req.url.includes(`${CONSTANTS_TESTING.env?.baseURL}/applications/_paged`);
      });

      expect(matchingRequests.length).toBeGreaterThan(0);
      matchingRequests[0].error(new ErrorEvent('Network error'), { status: 500 });

      const result = (await resultPromise) as WebhookLogsResponse;

      expect(result.data[0].application?.id).toBe('app-1');
      expect(result.data[0].application?.name).toBeUndefined();
    });
  });
});
