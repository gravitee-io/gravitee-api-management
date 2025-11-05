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
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { WebhookLogsQuickFiltersComponent } from './webhook-logs-quick-filters.component';

describe('WebhookLogsQuickFiltersComponent', () => {
  let component: WebhookLogsQuickFiltersComponent;
  let fixture: ComponentFixture<WebhookLogsQuickFiltersComponent>;

  const mockInitialValues = {
    methods: undefined,
    plans: undefined,
    entrypoints: undefined,
    applications: [],
    statuses: new Set<number>(),
    from: undefined,
    to: undefined,
    searchTerm: '',
    timeframe: '0',
    callbackUrls: [],
  };

  const mockApplications = [
    { id: 'app-1', name: 'App 1' },
    { id: 'app-2', name: 'App 2' },
  ];

  const mockCallbackUrls = ['https://test1.com/webhook', 'https://test2.com/webhook'];

  const setupComponent = (overrides: Partial<typeof mockInitialValues> = {}) => {
    fixture.componentRef.setInput('loading', false);
    fixture.componentRef.setInput('initialValues', { ...mockInitialValues, ...overrides });
    fixture.componentRef.setInput('applications', mockApplications);
    fixture.componentRef.setInput('callbackUrls', mockCallbackUrls);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsQuickFiltersComponent, NoopAnimationsModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsQuickFiltersComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    setupComponent();
    expect(component).toBeTruthy();
  });

  describe('Form Initialization', () => {
    it('should initialize form with default values', () => {
      setupComponent();

      expect(component.quickFiltersForm.value).toEqual({
        searchTerm: '',
        status: [],
        application: [],
        timeframe: '0',
        callbackUrls: [],
      });
    });

    it('should initialize with provided initial values', () => {
      setupComponent({
        searchTerm: 'test',
        timeframe: '-1h',
        statuses: new Set([200]),
      });

      expect(component.quickFiltersForm.value.searchTerm).toBe('test');
      expect(component.quickFiltersForm.value.timeframe).toBe('-1h');
      expect(component.quickFiltersForm.value.status).toEqual(['200']);
    });
  });

  describe('Filter Changes', () => {
    it('should emit filtersChanged when form values change', fakeAsync(() => {
      setupComponent();

      let emittedFilters;
      component.filtersChanged.subscribe((filters) => {
        emittedFilters = filters;
      });

      component.quickFiltersForm.patchValue({ searchTerm: 'test' });
      tick(300);

      expect(emittedFilters.searchTerm).toBe('test');
    }));

    it('should update isFiltering when filters differ from defaults', fakeAsync(() => {
      setupComponent();

      expect(component.isFiltering).toBe(false);

      component.quickFiltersForm.patchValue({ timeframe: '-1h' });
      tick(300);

      expect(component.isFiltering).toBe(true);
    }));
  });

  describe('Filter Display Values', () => {
    beforeEach(() => {
      setupComponent();
    });

    it('should format status display value', () => {
      const result = component.getFilterDisplayValue('status', ['200', '404']);
      expect(result).toBe('200, 404');
    });

    it('should format application display value', () => {
      const result = component.getFilterDisplayValue('application', ['app-1', 'app-2']);
      expect(result).toBe('2 selected');
    });

    it('should format timeframe display value', () => {
      const result = component.getFilterDisplayValue('timeframe', '-1h');
      expect(result).toBe('Last 1 Hour');
    });

    it('should format callback URLs display value', () => {
      const result = component.getFilterDisplayValue('callbackUrls', ['url1', 'url2']);
      expect(result).toBe('2 selected');
    });

    it('should return empty string for empty arrays', () => {
      expect(component.getFilterDisplayValue('status', [])).toBe('');
      expect(component.getFilterDisplayValue('application', [])).toBe('');
      expect(component.getFilterDisplayValue('callbackUrls', [])).toBe('');
    });
  });

  describe('Remove Filters', () => {
    beforeEach(() => {
      setupComponent();
    });

    it('should remove individual filter and restore default value', () => {
      component.quickFiltersForm.patchValue({ status: ['200'] });

      component.removeFilter({ key: 'status', value: ['200'] });

      expect(component.quickFiltersForm.value.status).toEqual([]);
    });

    it('should reset all filters to default values', () => {
      component.quickFiltersForm.patchValue({
        searchTerm: 'test',
        status: ['200'],
        application: ['app-1'],
        timeframe: '-1h',
      });

      component.resetAllFilters();

      expect(component.quickFiltersForm.value).toEqual(component.defaultFilters);
    });
  });

  describe('Filter Entries', () => {
    beforeEach(() => {
      setupComponent();
    });

    it('should return empty array when no filters are applied', () => {
      const entries = component.filterEntries();
      expect(entries).toEqual([]);
    });

    it('should compute filter entries for non-default values', () => {
      component.currentFilters.set({
        searchTerm: 'test',
        status: ['200'],
        application: [],
        timeframe: '0',
        callbackUrls: [],
      });

      const entries = component.filterEntries();
      expect(entries.length).toBe(2);
      expect(entries).toContainEqual({ key: 'searchTerm', value: 'test' });
      expect(entries).toContainEqual({ key: 'status', value: ['200'] });
    });

    it('should exclude empty arrays from filter entries', () => {
      component.currentFilters.set({
        searchTerm: 'test',
        status: [],
        application: [],
        timeframe: '0',
        callbackUrls: [],
      });

      const entries = component.filterEntries();
      expect(entries.length).toBe(1);
      expect(entries[0]).toEqual({ key: 'searchTerm', value: 'test' });
    });
  });

  describe('More Filters Drawer', () => {
    beforeEach(() => {
      setupComponent();
    });

    it('should toggle more filters drawer visibility', () => {
      expect(component.showMoreFilters).toBe(false);

      component.showMoreFilters = true;
      expect(component.showMoreFilters).toBe(true);

      component.showMoreFilters = false;
      expect(component.showMoreFilters).toBe(false);
    });

    it('should update more filters values', () => {
      const newValues = { period: { label: 'Last 1 Hour', value: '-1h' } };
      component.onMoreFiltersValuesChange(newValues);

      expect(component.moreFiltersValues).toEqual(newValues);
    });

    it('should track more filters invalid state', () => {
      component.onMoreFiltersInvalidChange(true);
      expect(component.moreFiltersInvalid).toBe(true);

      component.onMoreFiltersInvalidChange(false);
      expect(component.moreFiltersInvalid).toBe(false);
    });

    it('should reset more filters to default values', () => {
      const filtersChangedSpy = jest.fn();
      component.filtersChanged.subscribe(filtersChangedSpy);

      component.resetMoreFilters();

      expect(component.moreFiltersValues.period?.value).toBe('0');
      expect(component.moreFiltersValues.from).toBeNull();
      expect(component.moreFiltersValues.to).toBeNull();
      expect(component.moreFiltersValues.callbackUrls).toEqual([]);
    });

    it('should apply more filters, emit changes, and close drawer', () => {
      const filtersChangedSpy = jest.fn();
      component.filtersChanged.subscribe(filtersChangedSpy);

      component.showMoreFilters = true;
      component.moreFiltersValues = { period: { label: 'Last 1 Hour', value: '-1h' } };

      component.applyMoreFilters();

      expect(component.showMoreFilters).toBe(false);
      expect(filtersChangedSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          period: { label: 'Last 1 Hour', value: '-1h' },
        }),
      );
    });
  });
});
