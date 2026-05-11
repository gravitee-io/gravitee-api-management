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
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ApiRuntimeLogsNativeDetailsComponent } from './api-runtime-logs-native-details.component';
import { ApiRuntimeLogsNativeDetailsHarness } from './api-runtime-logs-native-details.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeNativeApiLog, NativeApiLog } from '../../../../entities/management-api-v2';

describe('ApiRuntimeLogsNativeDetailsComponent', () => {
  let fixture: ComponentFixture<ApiRuntimeLogsNativeDetailsComponent>;
  let httpTestingController: HttpTestingController;
  let harness: ApiRuntimeLogsNativeDetailsHarness;

  const API_ID = 'api-1';
  const REQUEST_ID = 'req-1';

  const setup = async (queryParams: Record<string, string> = { from: '1000', to: '2000' }) => {
    TestBed.configureTestingModule({
      imports: [ApiRuntimeLogsNativeDetailsComponent, MatIconTestingModule, GioTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID, requestId: REQUEST_ID }, queryParams } } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideAnimationsAsync('noop'),
      ],
    });
    fixture = TestBed.createComponent(ApiRuntimeLogsNativeDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsNativeDetailsHarness);
  };

  const flushLog = (log: NativeApiLog) =>
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/${REQUEST_ID}?from=1000&to=2000`)
      .flush(log);

  const errorLog = (status = 404, statusText = 'Not Found') =>
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/${REQUEST_ID}?from=1000&to=2000`)
      .error(new ProgressEvent(statusText), { status, statusText });

  afterEach(() => {
    httpTestingController?.verify();
  });

  it('renders title + connection + client + server cards on a successful CONNECTED log', async () => {
    await setup();
    flushLog(fakeNativeApiLog({ connectionStatus: 'CONNECTED', clientId: 'kafka-consumer-1', brokerId: '0' }));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.isTitleVisible()).toBe(true);
    expect(await harness.isConnectionCardVisible()).toBe(true);
    expect(await harness.isClientCardVisible()).toBe(true);
    expect(await harness.isServerCardVisible()).toBe(true);
    expect(await harness.isErrorCardVisible()).toBe(false);
  });

  it('renders error card when connectionStatus is not CONNECTED', async () => {
    await setup();
    flushLog(
      fakeNativeApiLog({
        connectionStatus: 'CONNECTION_ERROR',
        errorKey: 'AUTH_FAILED',
        errorMessage: 'SASL handshake failed: invalid credentials',
      }),
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.isErrorCardVisible()).toBe(true);
    const errorCard: HTMLElement = fixture.nativeElement.querySelector('[data-testid=native_log_error_card]');
    expect(errorCard.textContent).toContain('AUTH_FAILED');
    expect(errorCard.textContent).toContain('SASL handshake failed: invalid credentials');
  });

  it('hides the error card when status is CONNECTED', async () => {
    await setup();
    flushLog(fakeNativeApiLog({ connectionStatus: 'CONNECTED' }));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.isErrorCardVisible()).toBe(false);
  });

  it('renders the not-found banner and keeps the back link visible when the endpoint returns 404', async () => {
    await setup();
    errorLog(404, 'Not Found');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.isNotFoundBannerVisible()).toBe(true);
    expect(await harness.isLoadFailedBannerVisible()).toBe(false);
    expect(await harness.isTitleVisible()).toBe(false);
    expect(await harness.isBackLinkVisible()).toBe(true);
  });

  it('renders the load-failed banner (not the not-found banner) on non-404 errors', async () => {
    await setup();
    errorLog(500, 'Internal Server Error');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.isLoadFailedBannerVisible()).toBe(true);
    expect(await harness.isNotFoundBannerVisible()).toBe(false);
    expect(await harness.isBackLinkVisible()).toBe(true);
  });

  it('renders the load-failed banner without firing an HTTP call when from/to query params are missing', async () => {
    await setup({});
    httpTestingController.expectNone(req => req.url.includes(`/logs/native/${REQUEST_ID}`));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.isLoadFailedBannerVisible()).toBe(true);
    expect(await harness.isNotFoundBannerVisible()).toBe(false);
    expect(await harness.isBackLinkVisible()).toBe(true);
  });

  it('renders a dash for Duration in the connection card when connectionDurationMs is null', async () => {
    await setup();
    flushLog(fakeNativeApiLog({ connectionDurationMs: undefined }));
    await fixture.whenStable();
    fixture.detectChanges();

    const card: HTMLElement = fixture.nativeElement.querySelector('[data-testid=native_log_connection_card]');
    expect(card.textContent).toContain('Duration');
    expect(card.textContent).toContain('—');
  });
});
