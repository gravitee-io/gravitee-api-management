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
import moment from 'moment';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { GioTimeframeComponent } from './gio-timeframe.component';

describe('GioTimeframeComponent', () => {
  let component: GioTimeframeComponent;
  let fixture: ComponentFixture<GioTimeframeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        GioTimeframeComponent,
        FormsModule,
        MatFormFieldModule,
        MatOptionModule,
        MatSelectModule,
        MatInputModule,
        MatButtonModule,
        OwlDateTimeModule,
        OwlMomentDateTimeModule,
        NoopAnimationsModule,
        MatIconTestingModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GioTimeframeComponent);
    component = fixture.componentInstance;
  });

  it('should initialize with default value for period, from, and to', () => {
    component.writeValue(null);
    expect(component.value).toEqual({ period: '', from: null, to: null });
  });

  it('should register onChange callback', () => {
    const onChangeMock = jest.fn();
    component.registerOnChange(onChangeMock);

    const date = moment('2025-08-14T11:53:00.812Z');
    component.onPeriodChange('daily');
    component.onFromChange(date);
    component.onToChange(date);

    expect(onChangeMock).toHaveBeenCalledWith(component.value);
  });

  it('should register onTouched callback', () => {
    const onTouchedMock = jest.fn();
    component.registerOnTouched(onTouchedMock);

    component.onPeriodChange('weekly');

    expect(onTouchedMock).toHaveBeenCalled();
  });

  it('should update minDate when from value changes', () => {
    const fromDate = moment('2025-08-14', 'YYYY-MM-DD');
    component.onFromChange(fromDate);

    expect(component.minDate).toEqual(fromDate);
  });

  it('should emit apply event on apply button click', () => {
    jest.spyOn(component.apply, 'emit');

    component.onApplyClicked();

    expect(component.apply.emit).toHaveBeenCalled();
  });

  it('should update period and call onChange on period change', () => {
    const onChangeMock = jest.fn();
    component.registerOnChange(onChangeMock);

    const newPeriod = 'custom';
    component.onPeriodChange(newPeriod);

    expect(component.value.period).toEqual(newPeriod);
    expect(onChangeMock).toHaveBeenCalledWith(component.value);
  });

  it('should update value and call onChange on to date change', () => {
    const onChangeMock = jest.fn();
    component.registerOnChange(onChangeMock);

    const toDate = moment('2025-08-20', 'YYYY-MM-DD');
    component.onToChange(toDate);

    expect(component.value.to).toEqual(toDate);
    expect(onChangeMock).toHaveBeenCalledWith(component.value);
  });

  it('should disable the component when setDisabledState is called', () => {
    component.setDisabledState(true);

    expect(component.disabled).toBe(true);
  });

  it('should hide error and enable Apply when date range becomes valid', () => {
    // Arrange: provide timeFrames and valid custom period
    fixture.componentRef.setInput('timeFrames', [
      { id: '1d', label: '1 day' },
      { id: 'custom', label: 'Custom' },
    ]);

    const from = moment().hour(10);
    const to = moment(from).add(2, 'hours'); // Valid: to after from

    component.writeValue({ period: 'custom', from, to });

    // Act
    fixture.detectChanges();

    // Assert: no mat-error
    const errorEl: HTMLElement | null = fixture.nativeElement.querySelector('mat-error');
    expect(errorEl).toBeNull();

    // Assert: Apply button is enabled
    const applyBtn: HTMLButtonElement | null = fixture.nativeElement.querySelector('[data-testid="apply-button"]');
    expect(applyBtn).toBeTruthy();
    expect(applyBtn?.disabled).toBe(false);
  });
});
