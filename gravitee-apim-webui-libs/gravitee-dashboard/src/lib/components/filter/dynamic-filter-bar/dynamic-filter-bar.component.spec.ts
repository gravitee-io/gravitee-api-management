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
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { FilterCondition } from '../filter.model';
import { DynamicFilterBarComponent } from './dynamic-filter-bar.component';

const STATUS_EQ_200: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'EQ', values: ['200'] };
const API_IN_TWO: FilterCondition = { field: 'API', label: 'API', operator: 'IN', values: ['uuid-1', 'uuid-2'] };
const UNKNOWN_FILTER: FilterCondition = { field: 'UNKNOWN_NEW_FILTER', label: 'Unknown', operator: 'EQ', values: ['val'] };

@Component({
  standalone: true,
  imports: [DynamicFilterBarComponent],
  template: `
    <gd-dynamic-filter-bar
      [conditions]="conditions()"
      [editable]="editable()"
      (addRequested)="addCount.set(addCount() + 1)"
      (editRequested)="lastEdit.set($event)"
      (removeRequested)="lastRemoveIndex.set($event)"
      (clearRequested)="clearCount.set(clearCount() + 1)"
    />
  `,
})
class TestHostComponent {
  conditions = signal<FilterCondition[]>([]);
  editable = signal(true);
  addCount = signal(0);
  lastEdit = signal<{ index: number; condition: FilterCondition } | null>(null);
  lastRemoveIndex = signal<number | null>(null);
  clearCount = signal(0);
}

describe('DynamicFilterBarComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('empty state', () => {
    it('should render no chips when conditions is empty', () => {
      const chips = fixture.debugElement.queryAll(By.css('gd-filter-chip'));
      expect(chips.length).toBe(0);
    });

    it('should render the add button', () => {
      const addBtn = fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__add-btn'));
      expect(addBtn).toBeTruthy();
    });

    it('should not render the clear button when no conditions', () => {
      const clearBtn = fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__clear-btn'));
      expect(clearBtn).toBeNull();
    });
  });

  describe('with conditions', () => {
    beforeEach(() => {
      host.conditions.set([STATUS_EQ_200, API_IN_TWO]);
      fixture.detectChanges();
    });

    it('should render one chip per condition', () => {
      const chips = fixture.debugElement.queryAll(By.css('gd-filter-chip'));
      expect(chips.length).toBe(2);
    });

    it('should render the clear button when conditions exist', () => {
      const clearBtn = fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__clear-btn'));
      expect(clearBtn).toBeTruthy();
    });

    it('should emit removeRequested with index when chip remove is triggered', () => {
      const chips = fixture.debugElement.queryAll(By.css('gd-filter-chip'));
      chips[1].componentInstance.removed.emit();
      fixture.detectChanges();
      expect(host.lastRemoveIndex()).toBe(1);
    });

    it('should emit editRequested with index and condition when chip is clicked', () => {
      const chips = fixture.debugElement.queryAll(By.css('gd-filter-chip'));
      chips[0].componentInstance.clicked.emit();
      fixture.detectChanges();
      expect(host.lastEdit()).toEqual({ index: 0, condition: STATUS_EQ_200 });
    });
  });

  describe('add button', () => {
    it('should emit addRequested when add button is clicked', () => {
      const addBtn = fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__add-btn'));
      addBtn.nativeElement.click();
      fixture.detectChanges();
      expect(host.addCount()).toBe(1);
    });
  });

  describe('clear button', () => {
    it('should emit clearRequested when clear button is clicked', () => {
      host.conditions.set([STATUS_EQ_200]);
      fixture.detectChanges();
      const clearBtn = fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__clear-btn'));
      clearBtn.nativeElement.click();
      fixture.detectChanges();
      expect(host.clearCount()).toBe(1);
    });
  });

  describe('editable=false', () => {
    beforeEach(() => {
      host.conditions.set([STATUS_EQ_200]);
      host.editable.set(false);
      fixture.detectChanges();
    });

    it('should hide add and clear buttons', () => {
      expect(fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__add-btn'))).toBeNull();
      expect(fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__clear-btn'))).toBeNull();
    });

    it('should pass editable=false to chips', () => {
      const chip = fixture.debugElement.query(By.css('gd-filter-chip'));
      expect(chip.componentInstance.editable()).toBe(false);
    });
  });

  describe('resilience to unknown filter names', () => {
    it('should render chips for unknown filter names without crashing', () => {
      host.conditions.set([UNKNOWN_FILTER]);
      fixture.detectChanges();
      const chips = fixture.debugElement.queryAll(By.css('gd-filter-chip'));
      expect(chips.length).toBe(1);
    });
  });
});
