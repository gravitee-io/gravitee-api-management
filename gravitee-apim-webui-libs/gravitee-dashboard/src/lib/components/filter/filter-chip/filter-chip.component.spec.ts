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

import { buildChipLabel, buildChipLabelParts, buildChipTooltip, FilterCondition } from '../filter.model';
import { FilterChipComponent } from './filter-chip.component';

const STATUS_CODE_EQ_200: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'EQ', values: ['200'] };
const STATUS_CODE_IN_200_204: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'IN', values: ['200', '204'] };
const STATUS_CODE_NOT_IN_300_404: FilterCondition = {
  field: 'HTTP_STATUS',
  label: 'Status Code',
  operator: 'NOT_IN',
  values: ['300', '404'],
};
const STATUS_CODE_IN_200: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'IN', values: ['200'] };
const STATUS_CODE_GTE_500: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'GTE', values: ['500'] };
const STATUS_CODE_LTE_299: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'LTE', values: ['299'] };
const STATUS_CODE_EMPTY: FilterCondition = { field: 'HTTP_STATUS', label: 'Status Code', operator: 'EQ', values: [] };
const STATUS_CODE_UNKNOWN_OP: FilterCondition = {
  field: 'HTTP_STATUS',
  label: 'Status Code',
  operator: 'UNKNOWN_OP',
  values: ['value'],
};

describe('buildChipLabel', () => {
  describe('known operators', () => {
    it('should_display_operator_word_and_count_when_multiple_in_values', () => {
      expect(buildChipLabel(STATUS_CODE_IN_200_204)).toBe('Status Code in (2)');
    });

    it('should_display_operator_word_and_count_when_multiple_not_in_values', () => {
      expect(buildChipLabel(STATUS_CODE_NOT_IN_300_404)).toBe('Status Code not in (2)');
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
      expect(buildChipLabel(STATUS_CODE_UNKNOWN_OP)).toBe('Status Code UNKNOWN_OP value');
    });
  });
});

describe('buildChipLabelParts', () => {
  it('should_return_name_only_when_values_is_empty', () => {
    expect(buildChipLabelParts(STATUS_CODE_EMPTY)).toEqual({ name: 'Status Code', operator: '', value: '', isCount: false });
  });

  it('should_return_all_parts_for_single_eq_value', () => {
    expect(buildChipLabelParts(STATUS_CODE_EQ_200)).toEqual({ name: 'Status Code', operator: '=', value: '200', isCount: false });
  });

  it('should_normalize_single_in_value_to_eq_operator', () => {
    expect(buildChipLabelParts(STATUS_CODE_IN_200)).toEqual({ name: 'Status Code', operator: '=', value: '200', isCount: false });
  });

  it('should_return_bare_count_and_isCount_true_for_multiple_in_values', () => {
    expect(buildChipLabelParts(STATUS_CODE_IN_200_204)).toEqual({ name: 'Status Code', operator: 'in', value: '2', isCount: true });
  });

  it('should_return_bare_count_and_isCount_true_for_multiple_not_in_values', () => {
    expect(buildChipLabelParts(STATUS_CODE_NOT_IN_300_404)).toEqual({ name: 'Status Code', operator: 'not in', value: '2', isCount: true });
  });
});

describe('buildChipTooltip', () => {
  it('should_display_full_expression_with_in_word_for_multiple_values', () => {
    expect(buildChipTooltip(STATUS_CODE_IN_200_204)).toBe('Status Code in [200, 204]');
  });

  it('should_display_eq_expression_when_single_in_value', () => {
    expect(buildChipTooltip(STATUS_CODE_IN_200)).toBe('Status Code = 200');
  });

  it('should_return_label_only_when_values_is_empty', () => {
    expect(buildChipTooltip(STATUS_CODE_EMPTY)).toBe('Status Code');
  });

  it('should_display_raw_operator_name_when_operator_is_unknown', () => {
    expect(buildChipTooltip(STATUS_CODE_UNKNOWN_OP)).toBe('Status Code UNKNOWN_OP value');
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
    it('should_render_name_operator_and_value_in_separate_spans', () => {
      const nameEl = fixture.debugElement.query(By.css('.gd-filter-chip__name'));
      const operatorEl = fixture.debugElement.query(By.css('.gd-filter-chip__operator'));
      const valueEl = fixture.debugElement.query(By.css('.gd-filter-chip__value'));

      expect(nameEl.nativeElement.textContent.trim()).toBe('Status Code');
      expect(operatorEl.nativeElement.textContent.trim()).toBe('=');
      expect(valueEl.nativeElement.textContent.trim()).toBe('200');
    });

    it('should_render_operator_word_and_badge_count_for_multiple_values', () => {
      fixture.componentRef.setInput('filter', STATUS_CODE_IN_200_204);
      fixture.detectChanges();

      const valueEl = fixture.debugElement.query(By.css('.gd-filter-chip__value'));
      expect(fixture.debugElement.query(By.css('.gd-filter-chip__name')).nativeElement.textContent.trim()).toBe('Status Code');
      expect(fixture.debugElement.query(By.css('.gd-filter-chip__operator')).nativeElement.textContent.trim()).toBe('in');
      expect(valueEl.nativeElement.textContent.trim()).toBe('2');
      expect(valueEl.nativeElement.classList).toContain('gd-filter-chip__value--badge');
      expect(component['tooltip']()).toBe('Status Code in [200, 204]');
    });

    it('should_distinguish_in_and_not_in_operators_in_multi_value_label', () => {
      fixture.componentRef.setInput('filter', STATUS_CODE_NOT_IN_300_404);
      fixture.detectChanges();

      const valueEl = fixture.debugElement.query(By.css('.gd-filter-chip__value'));
      expect(fixture.debugElement.query(By.css('.gd-filter-chip__operator')).nativeElement.textContent.trim()).toBe('not in');
      expect(valueEl.nativeElement.textContent.trim()).toBe('2');
      expect(valueEl.nativeElement.classList).toContain('gd-filter-chip__value--badge');
      expect(component['tooltip']()).toBe('Status Code not in [300, 404]');
    });

    it('should_render_name_only_span_when_values_is_empty', () => {
      fixture.componentRef.setInput('filter', { ...STATUS_CODE_EQ_200, values: [] });
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('.gd-filter-chip__name')).nativeElement.textContent.trim()).toBe('Status Code');
      expect(fixture.debugElement.query(By.css('.gd-filter-chip__operator'))).toBeNull();
      expect(fixture.debugElement.query(By.css('.gd-filter-chip__value'))).toBeNull();
    });

    it('should_render_raw_operator_name_when_operator_is_unknown', () => {
      fixture.componentRef.setInput('filter', STATUS_CODE_UNKNOWN_OP);
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('.gd-filter-chip__operator')).nativeElement.textContent.trim()).toBe('UNKNOWN_OP');
      expect(fixture.debugElement.query(By.css('.gd-filter-chip__value')).nativeElement.textContent.trim()).toBe('value');
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

  describe('editable input', () => {
    it('should_not_emit_clicked_when_editable_is_false', () => {
      fixture.componentRef.setInput('editable', false);
      fixture.detectChanges();

      const spy = jest.spyOn(component.clicked, 'emit');
      const chipEl = fixture.debugElement.query(By.css('mat-chip'));
      chipEl.nativeElement.click();

      expect(spy).not.toHaveBeenCalled();
    });

    it('should_hide_remove_icon_when_editable_is_false', () => {
      fixture.componentRef.setInput('editable', false);
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('mat-icon[matChipRemove]'))).toBeNull();
    });

    it('should_show_remove_icon_when_editable_is_true', () => {
      fixture.componentRef.setInput('editable', true);
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('mat-icon[matChipRemove]'))).not.toBeNull();
    });

    it('should_set_disabled_on_mat_chip_when_editable_is_false', () => {
      fixture.componentRef.setInput('editable', false);
      fixture.detectChanges();

      const chipEl = fixture.debugElement.query(By.css('mat-chip'));
      expect(chipEl.componentInstance.disabled).toBe(true);
    });
  });
});
