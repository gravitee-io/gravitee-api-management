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
/// <reference types="jest" />

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { WebhookLogsQuickFiltersComponent } from './webhook-logs-quick-filters.component';

import { Constants } from '../../../../../../entities/Constants';
import { CONSTANTS_TESTING } from '../../../../../../shared/testing/gio-testing.module';

declare const jest: {
  spyOn: (...args: any[]) => any;
};
declare const describe: (...args: any[]) => void;
declare const beforeEach: (...args: any[]) => void;
declare const it: (...args: any[]) => void;
declare const expect: (...args: any[]) => any;

describe('WebhookLogsQuickFiltersComponent', () => {
  let fixture: ComponentFixture<WebhookLogsQuickFiltersComponent>;
  let component: WebhookLogsQuickFiltersComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsQuickFiltersComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsQuickFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit filters when form changes', () => {
    const filtersSpy = jest.spyOn(component.filtersChanged, 'emit');
    component.onApplicationCache([
      { value: 'app-1', label: 'Application One' },
      { value: 'app-2', label: 'Application Two' },
    ]);
    filtersSpy.mockClear();

    component.filtersForm.setValue({
      searchTerm: 'foo',
      statuses: [200, 500],
      applications: ['app-1'],
      period: component.defaultPeriod,
    });

    expect(filtersSpy).toHaveBeenCalledWith({
      searchTerm: 'foo',
      statuses: [200, 500],
      applications: [{ value: 'app-1', label: 'Application One' }],
    });
  });

  it('should clear the search value and emit filters', () => {
    const filtersSpy = jest.spyOn(component.filtersChanged, 'emit');
    filtersSpy.mockClear();
    component.filtersForm.setValue({ searchTerm: 'foo', statuses: [], applications: [], period: component.defaultPeriod });

    component.clearSearch();

    expect(component.filtersForm.get('searchTerm')!.value).toBe('');
    expect(filtersSpy).toHaveBeenCalledWith({ searchTerm: undefined, statuses: undefined, applications: undefined });
  });

  it('should emit active period when selection changes', () => {
    const filtersSpy = jest.spyOn(component.filtersChanged, 'emit');
    const nonDefaultPeriod = component.periods.find((period) => period.value !== component.defaultPeriod.value)!;
    component.filtersForm.setValue({
      searchTerm: '',
      statuses: [],
      applications: [],
      period: nonDefaultPeriod,
    });

    expect(filtersSpy).toHaveBeenCalledWith({
      searchTerm: undefined,
      statuses: undefined,
      applications: undefined,
      period: nonDefaultPeriod,
    });
  });

  it('should emit more filters event when clicking more button', () => {
    const moreSpy = jest.spyOn(component.moreFilters, 'emit');
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('[data-testid="more-button"]');
    button.click();

    expect(moreSpy).toHaveBeenCalled();
  });
});
