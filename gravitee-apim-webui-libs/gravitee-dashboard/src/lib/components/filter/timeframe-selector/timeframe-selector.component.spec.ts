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
import moment from 'moment';

import { TimeframeSelectorComponent, TimeframeValue } from './timeframe-selector.component';

describe('TimeframeSelectorComponent', () => {
  let component: TimeframeSelectorComponent;
  let fixture: ComponentFixture<TimeframeSelectorComponent>;

  const mockTimeFrames = [
    { id: '1h', label: '1 Hour' },
    { id: '1d', label: '1 Day' },
    { id: '7d', label: '7 Days' },
    { id: 'custom', label: 'Custom' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimeframeSelectorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TimeframeSelectorComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('timeFrames', mockTimeFrames);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize form with default period', () => {
      expect(component.form.get('period')?.value).toBe('1h');
      expect(component.form.get('from')?.value).toBeNull();
      expect(component.form.get('to')?.value).toBeNull();
    });

    it('should accept custom defaultPeriod input', () => {
      fixture.componentRef.setInput('defaultPeriod', '7d');
      fixture.detectChanges();

      // The input value should be set correctly
      expect(component.defaultPeriod()).toBe('7d');

      // writeValue should use the custom defaultPeriod when null is passed
      component.writeValue(null);
      expect(component.form.get('period')?.value).toBe('7d');
    });

    it('should initialize minDate as null', () => {
      expect(component.minDate).toBeNull();
    });

    it('should initialize nowDate as tomorrow', () => {
      const expectedDate = moment().add(1, 'd');
      expect(component.nowDate.isSame(expectedDate, 'day')).toBe(true);
    });

    it('should initialize disabled as false', () => {
      expect(component.disabled).toBe(false);
    });
  });

  describe('Form controls', () => {
    it('should have period, from, and to controls', () => {
      expect(component.form.contains('period')).toBe(true);
      expect(component.form.contains('from')).toBe(true);
      expect(component.form.contains('to')).toBe(true);
    });

    it('should have dateRangeGroupValidator applied', () => {
      const fromDate = moment('2024-01-15');
      const toDate = moment('2024-01-10');

      component.form.patchValue({ from: fromDate, to: toDate });
      expect(component.form.hasError('dateRange')).toBe(true);
    });

    it('should be valid when from date is before to date', () => {
      const fromDate = moment('2024-01-10');
      const toDate = moment('2024-01-15');

      component.form.patchValue({ from: fromDate, to: toDate });
      expect(component.form.hasError('dateRange')).toBe(false);
    });

    it('should be valid when dates are equal', () => {
      const date = moment('2024-01-10');

      component.form.patchValue({ from: date, to: date });
      expect(component.form.hasError('dateRange')).toBe(false);
    });
  });

  describe('value getter', () => {
    it('should return current form value', () => {
      const fromDate = moment('2024-01-10');
      const toDate = moment('2024-01-15');

      component.form.patchValue({ period: 'custom', from: fromDate, to: toDate });

      const value = component.value;
      expect(value.period).toBe('custom');
      expect(value.from?.isSame(fromDate)).toBe(true);
      expect(value.to?.isSame(toDate)).toBe(true);
    });

    it('should return default values when form is empty', () => {
      const value = component.value;
      expect(value.period).toBe('1h');
      expect(value.from).toBeNull();
      expect(value.to).toBeNull();
    });
  });

  describe('ControlValueAccessor - writeValue', () => {
    it('should set form values when writeValue is called', () => {
      const fromDate = moment('2024-01-10');
      const toDate = moment('2024-01-15');
      const value: TimeframeValue = { period: 'custom', from: fromDate, to: toDate };

      component.writeValue(value);

      expect(component.form.get('period')?.value).toBe('custom');
      expect(component.form.get('from')?.value?.isSame(fromDate)).toBe(true);
      expect(component.form.get('to')?.value?.isSame(toDate)).toBe(true);
    });

    it('should set minDate when writeValue is called with from date', () => {
      const fromDate = moment('2024-01-10');
      const value: TimeframeValue = { period: 'custom', from: fromDate, to: null };

      component.writeValue(value);

      expect(component.minDate?.isSame(fromDate)).toBe(true);
    });

    it('should use default period when writeValue is called with null', () => {
      component.writeValue(null);

      expect(component.form.get('period')?.value).toBe('1h');
      expect(component.form.get('from')?.value).toBeNull();
      expect(component.form.get('to')?.value).toBeNull();
    });

    it('should not emit event when writeValue is called', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);
      onChangeSpy.mockClear();

      const value: TimeframeValue = { period: '7d', from: null, to: null };
      component.writeValue(value);

      expect(onChangeSpy).not.toHaveBeenCalled();
    });

    it('should handle partial values in writeValue', () => {
      const value: TimeframeValue = { period: '1d', from: null, to: null };
      component.writeValue(value);

      expect(component.form.get('period')?.value).toBe('1d');
      expect(component.form.get('from')?.value).toBeNull();
      expect(component.form.get('to')?.value).toBeNull();
    });
  });

  describe('ControlValueAccessor - registerOnChange', () => {
    it('should register onChange callback', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.form.patchValue({ period: '7d' });

      expect(onChangeSpy).toHaveBeenCalledWith({
        period: '7d',
        from: null,
        to: null,
      });
    });

    it('should call onChange when form value changes', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      const fromDate = moment('2024-01-10');
      const toDate = moment('2024-01-15');
      component.form.patchValue({ period: 'custom', from: fromDate, to: toDate });

      expect(onChangeSpy).toHaveBeenCalledWith({
        period: 'custom',
        from: fromDate,
        to: toDate,
      });
    });
  });

  describe('ControlValueAccessor - registerOnTouched', () => {
    it('should register onTouched callback', () => {
      const onTouchedSpy = jest.fn();
      component.registerOnTouched(onTouchedSpy);

      component.onPeriodChange('7d');

      expect(onTouchedSpy).toHaveBeenCalled();
    });
  });

  describe('ControlValueAccessor - setDisabledState', () => {
    it('should disable form when setDisabledState is called with true', () => {
      component.setDisabledState!(true);

      expect(component.disabled).toBe(true);
      expect(component.form.disabled).toBe(true);
    });

    it('should enable form when setDisabledState is called with false', () => {
      component.setDisabledState!(true);
      component.setDisabledState!(false);

      expect(component.disabled).toBe(false);
      expect(component.form.enabled).toBe(true);
    });

    it('should not emit event when disabling form', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);
      onChangeSpy.mockClear();

      component.setDisabledState!(true);

      expect(onChangeSpy).not.toHaveBeenCalled();
    });
  });

  describe('onPeriodChange', () => {
    it('should update period control value', () => {
      component.onPeriodChange('7d');

      expect(component.form.get('period')?.value).toBe('7d');
    });

    it('should call onTouched when period changes', () => {
      const onTouchedSpy = jest.fn();
      component.registerOnTouched(onTouchedSpy);

      component.onPeriodChange('1d');

      expect(onTouchedSpy).toHaveBeenCalled();
    });
  });

  describe('onFromChange', () => {
    it('should update from control value', () => {
      const fromDate = moment('2024-01-10');

      component.onFromChange(fromDate);

      expect(component.form.get('from')?.value?.isSame(fromDate)).toBe(true);
    });

    it('should update minDate when from date changes', () => {
      const fromDate = moment('2024-01-10');

      component.onFromChange(fromDate);

      expect(component.minDate?.isSame(fromDate)).toBe(true);
    });

    it('should set minDate to null when from date is null', () => {
      component.onFromChange(moment('2024-01-10'));
      component.onFromChange(null);

      expect(component.minDate).toBeNull();
    });

    it('should update form validity when from date changes', () => {
      const fromDate = moment('2024-01-15');
      const toDate = moment('2024-01-10');
      component.form.patchValue({ to: toDate });

      component.onFromChange(fromDate);

      expect(component.form.hasError('dateRange')).toBe(true);
    });
  });

  describe('onToChange', () => {
    it('should update to control value', () => {
      const toDate = moment('2024-01-15');

      component.onToChange(toDate);

      expect(component.form.get('to')?.value?.isSame(toDate)).toBe(true);
    });

    it('should handle null to date', () => {
      component.onToChange(null);

      expect(component.form.get('to')?.value).toBeNull();
    });
  });

  describe('onApplyClicked', () => {
    it('should emit apply event', () => {
      const applySpy = jest.spyOn(component.apply, 'emit');

      component.onApplyClicked();

      expect(applySpy).toHaveBeenCalled();
    });
  });

  describe('onRefreshClicked', () => {
    it('should emit refresh event', () => {
      const refreshSpy = jest.spyOn(component.refresh, 'emit');

      component.onRefreshClicked();

      expect(refreshSpy).toHaveBeenCalled();
    });
  });

  describe('Form value changes subscription', () => {
    it('should call onChange when form value changes', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.form.patchValue({ period: '7d' });

      expect(onChangeSpy).toHaveBeenCalledWith({
        period: '7d',
        from: null,
        to: null,
      });
    });

    it('should handle null values in form changes', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.form.patchValue({ period: 'custom', from: null, to: null });

      expect(onChangeSpy).toHaveBeenCalledWith({
        period: 'custom',
        from: null,
        to: null,
      });
    });
  });

  describe('from valueChanges subscription', () => {
    it('should update minDate when from control value changes', () => {
      const fromDate = moment('2024-01-10');

      component.form.get('from')?.setValue(fromDate);

      expect(component.minDate?.isSame(fromDate)).toBe(true);
    });

    it('should update form validity when from control value changes', () => {
      const fromDate = moment('2024-01-15');
      const toDate = moment('2024-01-10');
      component.form.patchValue({ to: toDate });

      component.form.get('from')?.setValue(fromDate);

      expect(component.form.hasError('dateRange')).toBe(true);
    });
  });

  describe('Inputs', () => {
    it('should accept timeFrames input', () => {
      const customTimeFrames = [
        { id: '1h', label: 'One Hour' },
        { id: '1d', label: 'One Day' },
      ];

      fixture.componentRef.setInput('timeFrames', customTimeFrames);
      fixture.detectChanges();

      expect(component.timeFrames()).toEqual(customTimeFrames);
    });

    it('should use default customPeriod when not provided', () => {
      expect(component.customPeriod()).toBe('custom');
    });

    it('should accept custom customPeriod input', () => {
      fixture.componentRef.setInput('customPeriod', 'custom-range');
      fixture.detectChanges();

      expect(component.customPeriod()).toBe('custom-range');
    });

    it('should use default defaultPeriod when not provided', () => {
      expect(component.defaultPeriod()).toBe('1h');
    });

    it('should accept custom defaultPeriod input', () => {
      fixture.componentRef.setInput('defaultPeriod', '7d');
      fixture.detectChanges();

      expect(component.defaultPeriod()).toBe('7d');
    });
  });
});
