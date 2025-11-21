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
import { provideHttpClient } from '@angular/common/http';

import { WebhookLogsMoreFiltersComponent } from './webhook-logs-more-filters.component';
import { WebhookLogsMoreFiltersHarness } from './webhook-logs-more-filters.harness';

import { DEFAULT_PERIOD } from '../../../runtime-logs/models';

describe('WebhookLogsMoreFiltersComponent', () => {
  let fixture: ComponentFixture<WebhookLogsMoreFiltersComponent>;
  let harness: WebhookLogsMoreFiltersHarness;
  const callbackUrls = ['https://callback-1.test/webhook', 'https://callback-2.test/webhook'];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsMoreFiltersComponent, NoopAnimationsModule],
      providers: [provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsMoreFiltersComponent);
    fixture.componentRef.setInput('showMoreFilters', true);
    fixture.componentRef.setInput('callbackUrls', callbackUrls);
    fixture.componentRef.setInput('formValues', { period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] });
    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture<WebhookLogsMoreFiltersHarness>(fixture, WebhookLogsMoreFiltersHarness);
  });

  it('should emit selected values when clicking Show results', async () => {
    const applySpy = jest.fn();
    const closeSpy = jest.fn();
    fixture.componentInstance.applyMoreFiltersEvent.subscribe(applySpy);
    fixture.componentInstance.closeMoreFiltersEvent.subscribe(closeSpy);

    await harness.selectCallbackUrls([callbackUrls[0]]);
    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(
      expect.objectContaining({ callbackUrls: [callbackUrls[0]], period: DEFAULT_PERIOD, from: null, to: null }),
    );
    expect(closeSpy).toHaveBeenCalled();
  });

  it('should reset values when clicking Clear all', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFiltersEvent.subscribe(applySpy);

    await harness.selectCallbackUrls(callbackUrls);
    await harness.clickClearAll();

    expect(applySpy).toHaveBeenCalledWith({
      period: DEFAULT_PERIOD,
      from: null,
      to: null,
      callbackUrls: [],
    });
  });

  it('should emit close event when clicking the close icon', async () => {
    const closeSpy = jest.fn();
    fixture.componentInstance.closeMoreFiltersEvent.subscribe(closeSpy);

    await harness.clickClose();

    expect(closeSpy).toHaveBeenCalled();
  });
});
