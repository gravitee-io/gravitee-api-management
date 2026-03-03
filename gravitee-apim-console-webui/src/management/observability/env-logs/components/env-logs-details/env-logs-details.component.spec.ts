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
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { EnvLogsDetailsComponent } from './env-logs-details.component';
import { EnvLogsDetailsHarness } from './env-logs-details.harness';

import { fakeEnvLogs } from '../../models/env-log.fixture';

describe('EnvLogsDetailsComponent', () => {
  const logId = 'log-1';
  const log = fakeEnvLogs().find(l => l.id === logId)!;

  async function createComponent(params: Record<string, string> = { logId }) {
    await TestBed.configureTestingModule({
      imports: [EnvLogsDetailsComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params,
            },
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(EnvLogsDetailsComponent);
    fixture.detectChanges();

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvLogsDetailsHarness);

    return { fixture, harness };
  }

  it('should display log details', async () => {
    const { fixture, harness } = await createComponent();

    expect(await harness.getTitleText()).toContain('Log');
    expect(fixture.nativeElement.textContent).toContain(log.method);
    expect(await harness.getOverviewUri()).toEqual(log.api);
    expect(fixture.nativeElement.textContent).toContain(String(log.status));
  });

  it('should navigate back when the back button is clicked', async () => {
    const { harness } = await createComponent();
    await harness.clickBack();
  });

  it('should show "Log not found" banner when logId does not match any log', async () => {
    const { fixture } = await createComponent({ logId: 'non-existent-id' });

    const banner = fixture.nativeElement.querySelector('gio-banner-info');
    expect(banner).toBeTruthy();
    expect(banner.textContent).toContain('Log not found');
  });

  it('should show "Log not found" banner when logId is missing', async () => {
    const { fixture } = await createComponent({});

    const banner = fixture.nativeElement.querySelector('gio-banner-info');
    expect(banner).toBeTruthy();
    expect(banner.textContent).toContain('Log not found');
  });

  it('should display formatted headers sorted alphabetically', async () => {
    const { fixture } = await createComponent();
    const component = fixture.componentInstance;

    const headers = component.requestHeaders();

    const headerKeys = headers.map(h => h.key);
    const sortedKeys = [...headerKeys].sort((a, b) => a.localeCompare(b));
    expect(headerKeys).toEqual(sortedKeys);

    expect(headers.length).toBeGreaterThan(0);
  });

  it('should display expansion panel with Application, Plan, and Endpoint', async () => {
    const { fixture } = await createComponent();

    const expansionPanel = fixture.nativeElement.querySelector('.more-details');
    expect(expansionPanel).toBeTruthy();
    expect(expansionPanel.textContent).toContain('More details');

    const header = expansionPanel.querySelector('mat-expansion-panel-header');
    header.click();
    fixture.detectChanges();

    expect(expansionPanel.textContent).toContain(log.application);
    expect(expansionPanel.textContent).toContain(log.plan.name);
    expect(expansionPanel.textContent).toContain(log.endpoint);
  });

  it('should render the method badge with the correct class', async () => {
    const { fixture } = await createComponent();

    const methodBadge = fixture.nativeElement.querySelector(`.gio-method-badge-${log.method.toLowerCase()}`);
    expect(methodBadge).toBeTruthy();
    expect(methodBadge.textContent.trim()).toBe(log.method);
  });

  it('should render the status badge with the success class for status 200', async () => {
    const { fixture } = await createComponent();

    const statusBadge = fixture.nativeElement.querySelector('.gio-badge-success');
    expect(statusBadge).toBeTruthy();
    expect(statusBadge.textContent.trim()).toBe(String(log.status));
  });
});
