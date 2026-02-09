/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { FormControl } from '@angular/forms';
import moment from 'moment';
import { of } from 'rxjs';

import { GenericFilterBarComponent, Filter, SelectedFilter } from './generic-filter-bar.component';

describe('GenericFilterBarComponent', () => {
  let component: GenericFilterBarComponent;
  let fixture: ComponentFixture<GenericFilterBarComponent>;

  const DEFAULT_PERIOD = '1d';
  const MOCK_FILTERS: Filter[] = [
    {
      key: 'API',
      label: 'API',
      data: [
        { value: 'api-1', label: 'API 1' },
        { value: 'api-2', label: 'API 2' },
      ],
    },
    {
      key: 'APPLICATION',
      label: 'Application',
      data: [
        { value: 'app-1', label: 'App 1' },
        { value: 'app-2', label: 'App 2' },
      ],
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GenericFilterBarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(GenericFilterBarComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('filters', []);
    fixture.componentRef.setInput('currentSelectedFilters', []);
    fixture.componentRef.setInput('defaultPeriod', DEFAULT_PERIOD);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize form with default period', () => {
      const periodControl = component.form.controls.period;
      expect(periodControl).toBeTruthy();
      expect(periodControl.value).toEqual({ period: DEFAULT_PERIOD, from: undefined, to: undefined });
    });

    it('should create dynamic form controls when filters input is provided', () => {
      fixture.componentRef.setInput('filters', MOCK_FILTERS);
      fixture.detectChanges();

      expect(component.form.contains('API')).toBe(true);
      expect(component.form.contains('APPLICATION')).toBe(true);
      expect(component.form.get('API')?.value).toEqual([]);
    });

    it('should emit default period on init if currentSelectedFilters is empty', () => {
      const spy = jest.spyOn(component.selectedFilters, 'emit');
      component.ngOnInit();
      expect(spy).toHaveBeenCalledWith([{ parentKey: 'period', value: DEFAULT_PERIOD }]);
    });

    it('should NOT emit default period on init if currentSelectedFilters is provided', () => {
      fixture = TestBed.createComponent(GenericFilterBarComponent);
      component = fixture.componentInstance;
      fixture.componentRef.setInput('filters', MOCK_FILTERS);
      fixture.componentRef.setInput('defaultPeriod', DEFAULT_PERIOD);
      fixture.componentRef.setInput('currentSelectedFilters', [{ parentKey: 'API', value: 'api-1' }]);

      const spy = jest.spyOn(component.selectedFilters, 'emit');
      fixture.detectChanges();

      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('Sync: Input -> Form', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('filters', MOCK_FILTERS);
      fixture.detectChanges();
    });

    it('should update regular form controls when currentSelectedFilters changes', () => {
      const selectedInput: SelectedFilter[] = [
        { parentKey: 'API', value: 'api-1' },
        { parentKey: 'API', value: 'api-2' },
        { parentKey: 'APPLICATION', value: 'app-1' },
      ];

      fixture.componentRef.setInput('currentSelectedFilters', selectedInput);
      fixture.detectChanges();

      expect(component.form.get('API')?.value).toEqual(['api-1', 'api-2']);
      expect(component.form.get('APPLICATION')?.value).toEqual(['app-1']);
    });

    it('should update period control when currentSelectedFilters contains a period', () => {
      const selectedInput: SelectedFilter[] = [{ parentKey: 'period', value: '1h' }];

      fixture.componentRef.setInput('currentSelectedFilters', selectedInput);
      fixture.detectChanges();

      const periodValue = component.form.controls.period.value;
      expect(periodValue?.period).toBe('1h');
    });

    it('should update period control with custom dates', () => {
      const from = moment('2024-01-01');
      const to = moment('2024-01-31');
      const selectedInput: SelectedFilter[] = [
        { parentKey: 'period', value: 'custom' },
        { parentKey: 'from', value: from.valueOf().toString() },
        { parentKey: 'to', value: to.valueOf().toString() },
      ];

      fixture.componentRef.setInput('currentSelectedFilters', selectedInput);
      fixture.detectChanges();

      const periodValue = component.form.controls.period.value;
      expect(periodValue?.period).toBe('custom');
      expect(periodValue?.from?.valueOf()).toBe(from.valueOf());
      expect(periodValue?.to?.valueOf()).toBe(to.valueOf());
    });

    it('should reset period to default when currentSelectedFilters becomes empty', () => {
      fixture.componentRef.setInput('currentSelectedFilters', [{ parentKey: 'API', value: 'api-1' }]);
      fixture.detectChanges();

      fixture.componentRef.setInput('currentSelectedFilters', []);
      fixture.detectChanges();

      expect(component.form.controls.period.value?.period).toBe(DEFAULT_PERIOD);
    });
  });

  describe('Interaction: Form -> Output', () => {
    let emitSpy: jest.SpyInstance;

    beforeEach(() => {
      fixture.componentRef.setInput('filters', MOCK_FILTERS);
      fixture.detectChanges();
      emitSpy = jest.spyOn(component.selectedFilters, 'emit');
    });

    it('should emit when a regular filter value changes', () => {
      const apiControl = component.form.get('API') as FormControl;
      apiControl.setValue(['api-1']);

      expect(emitSpy).toHaveBeenCalled();
      const emitted = emitSpy.mock.calls[0][0] as SelectedFilter[];
      expect(emitted).toContainEqual({ parentKey: 'API', value: 'api-1' });
    });

    it('should emit when period changes', () => {
      component.form.controls.period.setValue({ period: '1h', from: undefined, to: undefined });

      expect(emitSpy).toHaveBeenCalled();
      const emitted = emitSpy.mock.calls[0][0] as SelectedFilter[];
      expect(emitted).toContainEqual({ parentKey: 'period', value: '1h' });
    });

    it('should emit custom period with flattened from/to keys', () => {
      const from = moment('2024-01-01');
      const to = moment('2024-01-31');

      component.form.controls.period.setValue({
        period: 'custom',
        from: from,
        to: to,
      });

      expect(emitSpy).toHaveBeenCalled();
      const emitted = emitSpy.mock.calls[0][0] as SelectedFilter[];

      expect(emitted).toContainEqual({ parentKey: 'period', value: 'custom' });
      expect(emitted).toContainEqual({ parentKey: 'from', value: from.valueOf().toString() });
      expect(emitted).toContainEqual({ parentKey: 'to', value: to.valueOf().toString() });
    });

    it('should not emit if value is identical', () => {
      const apiControl = component.form.get('API') as FormControl;

      apiControl.setValue(['api-1']);
      emitSpy.mockClear();

      apiControl.setValue(['api-1']);

      expect(emitSpy).not.toHaveBeenCalled();
    });
  });

  describe('Helper Methods', () => {
    it('should set period to custom via applyCustomTimeframe', () => {
      component.form.controls.period.setValue({ period: '1d', from: undefined, to: undefined });

      component.applyCustomTimeframe();

      const val = component.form.controls.period.value;
      expect(val?.period).toBe('custom');
    });

    it('should preserve dates when switching to custom via applyCustomTimeframe', () => {
      const from = moment();
      const to = moment().add(1, 'day');

      component.form.controls.period.setValue({ period: '1d', from, to });

      component.applyCustomTimeframe();

      const val = component.form.controls.period.value;
      expect(val?.period).toBe('custom');
      expect(val?.from).toBe(from);
      expect(val?.to).toBe(to);
    });

    it('should emit refresh event when calling refreshFilters', () => {
      const spy = jest.spyOn(component.refresh, 'emit');
      component.refreshFilters();
      expect(spy).toHaveBeenCalled();
    });
  });

  describe('Loaders support', () => {
    it('should support filters with Observable data source', () => {
      const obsFilter: Filter = {
        key: 'API',
        label: 'Async API',
        data$: of([{ value: '1', label: 'One' }]),
      };

      fixture.componentRef.setInput('filters', [obsFilter]);
      fixture.detectChanges();

      expect(component.form.contains('API')).toBe(true);
    });

    it('should support filters with Data Loader function', () => {
      const loaderFilter: Filter = {
        key: 'APPLICATION',
        label: 'Loader App',
        dataLoader: () => of({ data: [], hasNextPage: false }),
      };

      fixture.componentRef.setInput('filters', [loaderFilter]);
      fixture.detectChanges();

      expect(component.form.contains('APPLICATION')).toBe(true);
    });
  });
});
