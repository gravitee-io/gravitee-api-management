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

import { InstanceDetailsMonitoringComponent } from './instance-details-monitoring.component';
import { InstanceDetailsMonitoringModule } from './instance-details-monitoring.module';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioHttpTestingModule } from '../../../../shared/testing';

describe('InstanceMonitoringComponent', () => {
  let fixture: ComponentFixture<InstanceDetailsMonitoringComponent>;
  const instanceId = '5bc17c57-b350-460d-817c-57b350060db3';

  const fakeAjsState = {
    go: jest.fn(),
    includes: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, InstanceDetailsMonitoringModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { instanceId } },
        {
          provide: UIRouterState,
          useValue: fakeAjsState,
        },
      ],
    });
    fixture = TestBed.createComponent(InstanceDetailsMonitoringComponent);
  });

  describe('compute methods', () => {
    it.each([
      [Infinity, 1, '-'],
      [42, 1, '42.0 bytes'],
      [1050, 1, '1.0 kB'],
      [100056, 1, '97.7 kB'],
      [4096000000, 2, '3.81 GB'],
      [0, 1, '0.0 bytes'],
      [42, undefined, '42.0 bytes'],
      [42, 0, '42 bytes'],
    ])('humanizeSize | should convert %p bytes with precision %p into %p', (bytes, precision, result) => {
      expect(fixture.componentInstance.humanizeSize(bytes, precision)).toBe(result);
    });

    it.each([
      [42, 1, 4200],
      [42, 0, undefined],
      [0, 42, 0],
      [115312310, 240, 48046796],
      [240, 115312310, 0],
    ])('ratio | should compute ratio of %p on %p into %p', (val1, val2, result) => {
      expect(fixture.componentInstance.ratio(val1, val2)).toBe(result);
    });

    it.each([
      [42, 1, '4200%'],
      [42, 0, '-%'],
      [0, 42, '0%'],
      [115312310, 240, '48046796%'],
      [240, 115312310, '0%'],
    ])('ratioLabel | should compute ratio label of %p on %p into %p', (val1, val2, result) => {
      expect(fixture.componentInstance.ratioLabel(val1, val2)).toBe(result);
    });
  });
});
