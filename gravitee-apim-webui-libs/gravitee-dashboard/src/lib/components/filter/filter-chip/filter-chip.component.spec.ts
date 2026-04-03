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
import { By } from '@angular/platform-browser';

import { buildChipLabel, buildChipTooltip, FilterCondition } from '../filter.model';
import { FilterChipComponent } from './filter-chip.component';

const STATUS_CODE_EQ_200: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'EQ', values: ['200'] };
const STATUS_CODE_IN_200_204: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'IN', values: ['200', '204'] };
const STATUS_CODE_IN_200: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'IN', values: ['200'] };
const STATUS_CODE_GTE_500: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'GTE', values: ['500'] };
const STATUS_CODE_LTE_299: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'LTE', values: ['299'] };
const STATUS_CODE_EMPTY: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'EQ', values: [] };
const STATUS_CODE_UNKNOWN_OP: FilterCondition = {
  field: 'HTTP_STATUS',
  label: 'Status Code',
  operator: 'CONTAINS',
  values: ['value'],
};

describe('buildChipLabel', () => {
  describe('known operators', () => {
    it('should_display_count_when_multiple_values', () => {
      expect(buildChipLabel(STATUS_CODE_IN_200_204)).toBe('Status Code (2)');
    });

    it('should_display_eq_symbol_when_single_in_value', () => {
      expect(buildChipLabel(STATUS_CODE_IN_200)).toBe('Status Code = 200');
    });

    it('should_display_eq_symbol_for_eq_operator', () => {
      expect(buildChipLabel(STATUS_CODE_EQ_200)).toBe('Status Code = 200');
    });

    it('should_display_gte_symbol_for_gte_operator', () => {
      expect(buildChipLabel(STATUS_CODE_GTE_500)).toBe('Status Code ≥ 500');
    });

    it('should_display_lte_symbol_for_lte_operator', () => {
      expect(buildChipLabel(STATUS_CODE_LTE_299)).toBe('Status Code ≤ 299');
    });
  });

  describe('edge cases', () => {
    it('should_return_label_only_when_values_is_empty', () => {
      expect(buildChipLabel(STATUS_CODE_EMPTY)).toBe('Status Code');
    });

    it('should_display_raw_operator_name_when_operator_is_unknown', () => {
      expect(buildChipLabel(STATUS_CODE_UNKNOWN_OP)).toBe('Status Code CONTAINS value');
    });
  });
});

describe('buildChipTooltip', () => {
  it('should_display_full_expression_with_in_symbol_for_multiple_values', () => {
    expect(buildChipTooltip(STATUS_CODE_IN_200_204)).toBe('Status Code ∈ [200, 204]');
  });

  it('should_display_eq_expression_when_single_in_value', () => {
    expect(buildChipTooltip(STATUS_CODE_IN_200)).toBe('Status Code = 200');
  });

  it('should_return_label_only_when_values_is_empty', () => {
    expect(buildChipTooltip(STATUS_CODE_EMPTY)).toBe('Status Code');
  });

  it('should_display_raw_operator_name_when_operator_is_unknown', () => {
    expect(buildChipTooltip(STATUS_CODE_UNKNOWN_OP)).toBe('Status Code CONTAINS value');
  });
});

describe('FilterChipComponent', () => {
  let component: FilterChipComponent;
  let fixture: ComponentFixture<FilterChipComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilterChipComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterChipComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('filter', STATUS_CODE_EQ_200);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Label display', () => {
    it('should_display_correct_label_for_single_eq_value', () => {
      const chipEl = fixture.debugElement.query(By.css('mat-chip'));
      expect(chipEl.nativeElement.textContent).toContain('Status Code = 200');
    });

    it('should_display_count_as_label_and_full_expression_as_tooltip_for_multiple_values', () => {
      fixture.componentRef.setInput('filter', STATUS_CODE_IN_200_204);
      fixture.detectChanges();

      const chipEl = fixture.debugElement.query(By.css('mat-chip'));
      expect(chipEl.nativeElement.textContent).toContain('Status Code (2)');
      expect(component['tooltip']()).toBe('Status Code ∈ [200, 204]');
    });

    it('should_display_raw_operator_name_when_operator_is_unknown', () => {
      fixture.componentRef.setInput('filter', STATUS_CODE_UNKNOWN_OP);
      fixture.detectChanges();

      const chipEl = fixture.debugElement.query(By.css('mat-chip'));
      expect(chipEl.nativeElement.textContent).toContain('Status Code CONTAINS value');
    });
  });

  describe('Outputs', () => {
    it('should_emit_clicked_when_chip_body_is_clicked', () => {
      const spy = jest.spyOn(component.clicked, 'emit');
      const chipEl = fixture.debugElement.query(By.css('mat-chip'));
      chipEl.nativeElement.click();

      expect(spy).toHaveBeenCalled();
    });

    it('should_emit_removed_when_remove_icon_is_clicked', () => {
      const spy = jest.spyOn(component.removed, 'emit');
      const removeIcon = fixture.debugElement.query(By.css('mat-icon[matChipRemove]'));
      removeIcon.nativeElement.click();

      expect(spy).toHaveBeenCalled();
    });

    it('should_not_emit_clicked_when_remove_icon_is_clicked', () => {
      const clickedSpy = jest.spyOn(component.clicked, 'emit');
      const removeIcon = fixture.debugElement.query(By.css('mat-icon[matChipRemove]'));
      removeIcon.nativeElement.click();

      expect(clickedSpy).not.toHaveBeenCalled();
    });
  });
});
