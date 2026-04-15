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

import { AnalyticsTimeframeComponent } from './analytics-timeframe.component';

describe('AnalyticsTimeframeComponent', () => {
  let fixture: ComponentFixture<AnalyticsTimeframeComponent>;
  let component: AnalyticsTimeframeComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsTimeframeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsTimeframeComponent);
    component = fixture.componentInstance;
  });

  it('should default to last 1 hour on first load', () => {
    const emitSpy = jest.spyOn(component.timeframeChange, 'emit');

    fixture.detectChanges();

    const selectedButton = fixture.nativeElement.querySelector('[data-testid="timeframe-1h"]');
    expect(selectedButton.classList.contains('selected')).toBeTruthy();
    expect(emitSpy).toHaveBeenCalledTimes(1);
  });

  it('should emit timeframeChange with computed epoch range when selecting a timeframe', () => {
    const now = 1_730_000_000_000;
    const dateNowSpy = jest.spyOn(Date, 'now').mockReturnValue(now);
    const emitSpy = jest.spyOn(component.timeframeChange, 'emit');

    fixture.detectChanges();

    const sevenDaysButton = fixture.nativeElement.querySelector('[data-testid="timeframe-7d"]');
    sevenDaysButton.click();
    fixture.detectChanges();

    expect(emitSpy).toHaveBeenNthCalledWith(2, {
      from: now - 7 * 24 * 60 * 60 * 1000,
      to: now,
    });

    dateNowSpy.mockRestore();
  });

  it('should keep exactly one selected timeframe button at a time', () => {
    fixture.detectChanges();

    const twentyFourHoursButton = fixture.nativeElement.querySelector('[data-testid="timeframe-24h"]');
    twentyFourHoursButton.click();
    fixture.detectChanges();

    const selectedButtons = fixture.nativeElement.querySelectorAll('button.selected');
    expect(selectedButtons.length).toBe(1);
    expect(selectedButtons[0].getAttribute('data-testid')).toBe('timeframe-24h');
  });
});
