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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiRuntimeLogsNativeListComponent } from './api-runtime-logs-native-list.component';
import { ApiRuntimeLogsNativeListHarness } from './api-runtime-logs-native-list.component.harness';

import { fakeNativeApiLog, fakePlanV4 } from '../../../../../../entities/management-api-v2';
import { fakeApplication } from '../../../../../../entities/application/Application.fixture';

describe('ApiRuntimeLogsNativeListComponent', () => {
  let fixture: ComponentFixture<ApiRuntimeLogsNativeListComponent>;
  let component: ApiRuntimeLogsNativeListComponent;
  let harness: ApiRuntimeLogsNativeListHarness;

  const apps = [fakeApplication({ id: 'app-1', name: 'Order Service' })];
  const plans = [fakePlanV4({ id: 'plan-1', name: 'Free' })];

  const setup = async (logs: ReturnType<typeof fakeNativeApiLog>[], applications = apps, knownPlans = plans) => {
    TestBed.configureTestingModule({
      imports: [ApiRuntimeLogsNativeListComponent, MatIconTestingModule],
      providers: [provideAnimationsAsync('noop')],
    });
    fixture = TestBed.createComponent(ApiRuntimeLogsNativeListComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('logs', logs);
    fixture.componentRef.setInput('pagination', { page: 1, perPage: 10, totalCount: logs.length });
    fixture.componentRef.setInput('applications', applications);
    fixture.componentRef.setInput('plans', knownPlans);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsNativeListHarness);
  };

  it('renders application name when id is in the resolved set', async () => {
    await setup([fakeNativeApiLog({ applicationId: 'app-1' })]);
    expect(component.applicationName('app-1')).toBe('Order Service');
  });

  it('renders empty when application is not in the resolved set', async () => {
    await setup([fakeNativeApiLog({ applicationId: 'app-unknown' })]);
    expect(component.applicationName('app-unknown')).toBe('');
  });

  it('renders plan name when id is in the resolved set', async () => {
    await setup([fakeNativeApiLog({ planId: 'plan-1' })]);
    expect(component.planName('plan-1')).toBe('Free');
  });

  it('renders empty when plan is not in the resolved set', async () => {
    await setup([fakeNativeApiLog({ planId: 'plan-removed' })]);
    expect(component.planName('plan-removed')).toBe('');
  });

  it('renders the friendly status label and badge class for known statuses', async () => {
    await setup([fakeNativeApiLog({ connectionStatus: 'CONNECTED' }), fakeNativeApiLog({ connectionStatus: 'CONNECTION_ERROR' })]);
    const badges = await harness.getStatusBadges();
    expect(badges).toHaveLength(2);
    expect(badges[0].text).toBe('Connected');
    expect(badges[0].classes).toContain('gio-badge-success');
    expect(badges[1].text).toBe('Failed');
    expect(badges[1].classes).toContain('gio-badge-error');
  });

  it('omits the badge cell when connectionStatus is missing', async () => {
    await setup([fakeNativeApiLog({ connectionStatus: undefined })]);
    expect(await harness.getStatusBadges()).toHaveLength(0);
  });

  it('omits the duration cell value when connectionDurationMs is null', async () => {
    await setup([fakeNativeApiLog({ connectionDurationMs: undefined })]);
    expect(await harness.getDurationTexts()).toEqual(['']);
  });

  it('does not re-emit pagination when the event matches current state', async () => {
    await setup([fakeNativeApiLog()]);
    const emitSpy = jest.spyOn(component.paginationUpdated, 'emit');
    component.onFiltersChanged({ pagination: { index: 1, size: 10 }, searchTerm: '' });
    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('re-emits pagination when the size changes', async () => {
    await setup([fakeNativeApiLog()]);
    const emitSpy = jest.spyOn(component.paginationUpdated, 'emit');
    component.onFiltersChanged({ pagination: { index: 1, size: 25 }, searchTerm: '' });
    expect(emitSpy).toHaveBeenCalledWith({ index: 1, size: 25 });
  });
});
