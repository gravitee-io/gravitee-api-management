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
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import moment from 'moment';

import { WebhookLogsQuickFiltersComponent } from './webhook-logs-quick-filters.component';

import { Constants } from '../../../../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { DEFAULT_PERIOD } from '../../../runtime-logs/models';

describe('WebhookLogsQuickFiltersComponent', () => {
  let fixture: ComponentFixture<WebhookLogsQuickFiltersComponent>;
  let component: WebhookLogsQuickFiltersComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsQuickFiltersComponent, NoopAnimationsModule, GioTestingModule],
      providers: [
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsQuickFiltersComponent);
    component = fixture.componentInstance;
    component.initialValues = {};
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with statuses as strings when initialValues contains number statuses', () => {
    const newFixture = TestBed.createComponent(WebhookLogsQuickFiltersComponent);
    const newComponent = newFixture.componentInstance;
    newComponent.initialValues = {
      statuses: [200, 404, 500],
    };
    newComponent.ngOnInit();
    newFixture.detectChanges();

    const statusesValue = newComponent.quickFiltersForm?.get('statuses')?.value;
    expect(statusesValue).toEqual(['200', '404', '500']);
  });

  it('should emit filters when form changes', () => {
    const filtersSpy = jest.spyOn(component.filtersChanged, 'emit');
    component.onApplicationCache([
      { value: 'app-1', label: 'Application One' },
      { value: 'app-2', label: 'Application Two' },
    ]);
    filtersSpy.mockClear();

    component.quickFiltersForm.setValue({
      statuses: ['200', '500'],
      applications: ['app-1'],
      period: DEFAULT_PERIOD,
    });

    expect(filtersSpy).toHaveBeenCalledWith({
      statuses: [200, 500],
      applications: [{ value: 'app-1', label: 'Application One' }],
    });
  });

  it('should emit active period when selection changes', () => {
    const filtersSpy = jest.spyOn(component.filtersChanged, 'emit');
    const nonDefaultPeriod = component.periods.find(period => period.value !== DEFAULT_PERIOD.value)!;
    component.quickFiltersForm.setValue({
      statuses: [],
      applications: [],
      period: nonDefaultPeriod,
    });

    expect(filtersSpy).toHaveBeenCalledWith({
      statuses: undefined,
      applications: undefined,
      period: nonDefaultPeriod,
    });
  });

  it('should open more filters panel when clicking more button', () => {
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('[data-testid="more-button"]');
    button.click();

    expect(component.showMoreFilters).toBe(true);
  });

  it('should apply more filters and emit values with custom dates', () => {
    const filtersSpy = jest.spyOn(component.filtersChanged, 'emit');
    filtersSpy.mockClear();

    const from = moment('2025-01-01T00:00:00Z');
    const to = moment('2025-01-02T00:00:00Z');
    component.applyMoreFilters({ period: DEFAULT_PERIOD, from, to, callbackUrls: [] });

    expect(filtersSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        from: from.valueOf(),
        to: to.valueOf(),
      }),
    );
  });

  it('should reset filters when clicking reset button', () => {
    const filtersSpy = jest.spyOn(component.filtersChanged, 'emit');
    component.quickFiltersForm.setValue({
      statuses: ['200'],
      applications: ['app-1'],
      period: DEFAULT_PERIOD,
    });
    component.onApplicationCache([{ value: 'app-1', label: 'Acme App' }]);
    filtersSpy.mockClear();

    fixture.detectChanges();

    const resetButton: HTMLButtonElement = fixture.nativeElement.querySelector('[data-testId="reset-filters-button"]');
    resetButton.click();

    expect(component.quickFiltersForm.value).toEqual({
      statuses: [],
      applications: [],
      period: DEFAULT_PERIOD,
    });
    expect(filtersSpy).toHaveBeenCalledWith({
      statuses: undefined,
      applications: undefined,
    });
  });
});
