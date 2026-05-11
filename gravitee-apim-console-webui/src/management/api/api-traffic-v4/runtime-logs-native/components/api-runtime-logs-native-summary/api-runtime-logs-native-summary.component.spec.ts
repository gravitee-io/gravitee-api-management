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

import { ApiRuntimeLogsNativeSummaryComponent } from './api-runtime-logs-native-summary.component';
import { ApiRuntimeLogsNativeSummaryHarness } from './api-runtime-logs-native-summary.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { fakeNativeApiLogsSummary } from '../../../../../../entities/management-api-v2';

describe('ApiRuntimeLogsNativeSummaryComponent', () => {
  let fixture: ComponentFixture<ApiRuntimeLogsNativeSummaryComponent>;
  let httpTestingController: HttpTestingController;

  const API_ID = 'an-api-id';

  const setup = ({ from, to }: { from: number | null; to: number | null }) => {
    TestBed.configureTestingModule({
      imports: [ApiRuntimeLogsNativeSummaryComponent, MatIconTestingModule, GioTestingModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting(), provideAnimationsAsync('noop')],
    });
    fixture = TestBed.createComponent(ApiRuntimeLogsNativeSummaryComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('apiId', API_ID);
    fixture.componentRef.setInput('from', from);
    fixture.componentRef.setInput('to', to);
    fixture.detectChanges();
  };

  const getHarness = () => TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsNativeSummaryHarness);

  afterEach(() => {
    httpTestingController?.verify();
  });

  it('does not call the summary endpoint when from or to are null', async () => {
    setup({ from: null, to: null });
    httpTestingController.expectNone(req => req.url.endsWith('/logs/native/summary'));
    const harness = await getHarness();
    expect(await harness.getCounts()).toEqual({ CONNECTED: '—', SESSION_ERROR: '—', CONNECTION_ERROR: '—', INTERNAL_ERROR: '—' });
  });

  it('renders counts from the summary endpoint when from/to are set', async () => {
    setup({ from: 1000, to: 2000 });
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/summary?from=1000&to=2000`)
      .flush(fakeNativeApiLogsSummary());
    fixture.detectChanges();
    const harness = await getHarness();
    expect(await harness.getCounts()).toEqual({ CONNECTED: '184', SESSION_ERROR: '32', CONNECTION_ERROR: '28', INTERNAL_ERROR: '4' });
  });

  it('falls back to 0 for statuses missing in the response', async () => {
    setup({ from: 1000, to: 2000 });
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/summary?from=1000&to=2000`)
      .flush({ countByConnectionStatus: { CONNECTED: 5 } });
    fixture.detectChanges();
    const harness = await getHarness();
    expect(await harness.getCounts()).toEqual({ CONNECTED: '5', SESSION_ERROR: '0', CONNECTION_ERROR: '0', INTERNAL_ERROR: '0' });
  });

  it('refetches when from/to change', async () => {
    setup({ from: 1000, to: 2000 });
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/summary?from=1000&to=2000`)
      .flush(fakeNativeApiLogsSummary());
    fixture.detectChanges();

    fixture.componentRef.setInput('from', 3000);
    fixture.componentRef.setInput('to', 4000);
    fixture.detectChanges();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/summary?from=3000&to=4000`)
      .flush(
        fakeNativeApiLogsSummary({ countByConnectionStatus: { CONNECTED: 1, SESSION_ERROR: 2, CONNECTION_ERROR: 3, INTERNAL_ERROR: 4 } }),
      );
    fixture.detectChanges();
    const harness = await getHarness();
    expect(await harness.getCounts()).toEqual({ CONNECTED: '1', SESSION_ERROR: '2', CONNECTION_ERROR: '3', INTERNAL_ERROR: '4' });
  });

  it('shows a retry button when the summary endpoint errors, and re-fetches on click', async () => {
    setup({ from: 1000, to: 2000 });
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/summary?from=1000&to=2000`)
      .error(new ProgressEvent('Network error'), { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    const retry: HTMLButtonElement | null = fixture.nativeElement.querySelector('[data-testid="native_logs_summary_retry"]');
    expect(retry).not.toBeNull();
    retry!.click();
    fixture.detectChanges();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/native/summary?from=1000&to=2000`)
      .flush(fakeNativeApiLogsSummary());
    fixture.detectChanges();
    const harness = await getHarness();
    expect(await harness.getCounts()).toEqual({ CONNECTED: '184', SESSION_ERROR: '32', CONNECTION_ERROR: '28', INTERNAL_ERROR: '4' });
  });
});
