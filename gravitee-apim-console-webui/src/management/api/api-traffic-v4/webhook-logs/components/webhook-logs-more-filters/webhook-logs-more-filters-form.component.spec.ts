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
import { OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import moment from 'moment';

import { WebhookLogsMoreFiltersFormComponent } from './webhook-logs-more-filters-form.component';
import { WebhookLogsMoreFiltersModule } from './webhook-logs-more-filters.module';
import { DEFAULT_PERIOD } from '../../../models';

describe('WebhookLogsMoreFiltersFormComponent', () => {
  let component: WebhookLogsMoreFiltersFormComponent;
  let fixture: ComponentFixture<WebhookLogsMoreFiltersFormComponent>;

  const mockFormValues = {
    period: DEFAULT_PERIOD,
    from: null,
    to: null,
    callbackUrls: [],
  };

  const mockCallbackUrls = ['https://test1.com/webhook', 'https://test2.com/webhook'];

  const setupComponent = (formValuesOverrides = {}) => {
    component.formValues = { ...mockFormValues, ...formValuesOverrides };
    component.callbackUrls = mockCallbackUrls;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsMoreFiltersModule, NoopAnimationsModule, MatIconTestingModule, OwlDateTimeModule, OwlMomentDateTimeModule],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsMoreFiltersFormComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    setupComponent();
    expect(component).toBeTruthy();
  });

  describe('Form Initialization', () => {
    it('should initialize datesForm with default values', () => {
      setupComponent();

      expect(component.datesForm).toBeDefined();
      expect(component.datesForm.value.period).toEqual(DEFAULT_PERIOD);
      expect(component.datesForm.value.from).toBeNull();
      expect(component.datesForm.value.to).toBeNull();
    });

    it('should initialize moreFiltersForm with default values', () => {
      setupComponent();

      expect(component.moreFiltersForm).toBeDefined();
      expect(component.moreFiltersForm.value.callbackUrls).toEqual([]);
    });

    it('should initialize with provided form values', () => {
      const customFormValues = {
        period: { label: 'Last 1 Hour', value: '-1h' },
        from: moment('2025-06-15'),
        to: moment('2025-06-16'),
        callbackUrls: ['https://test1.com/webhook'],
      };
      setupComponent(customFormValues);

      expect(component.datesForm.value.period).toEqual(customFormValues.period);
      expect(component.datesForm.value.from).toEqual(customFormValues.from);
      expect(component.datesForm.value.to).toEqual(customFormValues.to);
      expect(component.moreFiltersForm.value.callbackUrls).toEqual(customFormValues.callbackUrls);
    });

    it('should emit initial values on init', fakeAsync(() => {
      let emittedValues;
      component.valuesChangeEvent.subscribe((values) => {
        emittedValues = values;
      });

      setupComponent();
      tick();

      expect(emittedValues).toBeDefined();
      expect(emittedValues.period).toEqual(DEFAULT_PERIOD);
    }));
  });

  describe('Period Selection', () => {
    beforeEach(() => {
      setupComponent();
    });

    it('should clear from and to dates when period is changed', fakeAsync(() => {
      const fromDate = moment('2025-06-15');
      const toDate = moment('2025-06-16');

      component.datesForm.patchValue({ from: fromDate, to: toDate });
      tick();

      let emittedValues;
      component.valuesChangeEvent.subscribe((values) => {
        emittedValues = values;
      });

      component.datesForm.patchValue({ period: { label: 'Last 1 Hour', value: '-1h' } });
      tick();

      expect(emittedValues.from).toBeNull();
      expect(emittedValues.to).toBeNull();
    }));

    it('should reset period to None when from date is set', fakeAsync(() => {
      const fromDate = moment('2025-06-15');

      let emittedValues;
      component.valuesChangeEvent.subscribe((values) => {
        emittedValues = values;
      });

      component.datesForm.patchValue({ from: fromDate });
      tick();

      expect(emittedValues.period).toEqual(DEFAULT_PERIOD);
      expect(component.minDate).toEqual(fromDate);
    }));

    it('should reset period to None when to date is set', fakeAsync(() => {
      const toDate = moment('2025-06-16');

      let emittedValues;
      component.valuesChangeEvent.subscribe((values) => {
        emittedValues = values;
      });

      component.datesForm.patchValue({ to: toDate });
      tick();

      expect(emittedValues.period).toEqual(DEFAULT_PERIOD);
    }));
  });

  describe('Callback URL Selection', () => {
    beforeEach(() => {
      setupComponent();
    });

    it('should update callback URLs', fakeAsync(() => {
      let emittedValues;
      component.valuesChangeEvent.subscribe((values) => {
        emittedValues = values;
      });

      component.moreFiltersForm.patchValue({ callbackUrls: ['https://test1.com/webhook'] });
      tick();

      expect(emittedValues.callbackUrls).toEqual(['https://test1.com/webhook']);
    }));

    it('should handle multiple callback URLs', fakeAsync(() => {
      let emittedValues;
      component.valuesChangeEvent.subscribe((values) => {
        emittedValues = values;
      });

      component.moreFiltersForm.patchValue({ callbackUrls: mockCallbackUrls });
      tick();

      expect(emittedValues.callbackUrls).toEqual(mockCallbackUrls);
    }));
  });

  describe('Form Validation', () => {
    beforeEach(() => {
      setupComponent();
    });

    it('should emit isInvalid state on changes', fakeAsync(() => {
      let emittedIsInvalid;
      component.isInvalidEvent.subscribe((isInvalid) => {
        emittedIsInvalid = isInvalid;
      });

      component.datesForm.patchValue({ period: { label: 'Last 1 Hour', value: '-1h' } });
      tick();

      expect(emittedIsInvalid).toBe(false);
    }));
  });
});
