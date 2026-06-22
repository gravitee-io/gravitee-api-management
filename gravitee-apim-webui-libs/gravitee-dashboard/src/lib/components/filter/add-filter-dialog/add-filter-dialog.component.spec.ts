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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { firstValueFrom, of } from 'rxjs';

import { FILTER_DEFINITION_PROVIDER, FILTER_VALUES_PROVIDER, FilterDefinitionProvider, FilterValuesProvider } from '../filter-providers';
import { FilterDefinition } from '../filter.model';
import { AddFilterDialogComponent, AddFilterDialogData } from './add-filter-dialog.component';

const ENUM_DEF: FilterDefinition = {
  name: 'HTTP_METHOD',
  label: 'HTTP Method',
  type: 'ENUM',
  operators: ['EQ', 'IN'],
  values: ['GET', 'POST', 'PUT', 'DELETE'],
  apiTypes: ['HTTP_PROXY', 'LLM'],
};

const NUMBER_DEF: FilterDefinition = {
  name: 'HTTP_STATUS',
  label: 'Status Code',
  type: 'NUMBER',
  operators: ['EQ', 'LTE', 'GTE'],
  range: { min: 100, max: 599 },
  apiTypes: ['HTTP_PROXY'],
};

const KEYWORD_DEF: FilterDefinition = {
  name: 'API',
  label: 'API',
  type: 'KEYWORD',
  operators: ['EQ', 'IN'],
  apiTypes: ['HTTP_PROXY', 'LLM', 'MESSAGE', 'MCP'],
};

const STRING_DEF: FilterDefinition = {
  name: 'HTTP_PATH',
  label: 'HTTP Path',
  type: 'STRING',
  operators: ['EQ'],
};

const CONTAINS_STRING_DEF: FilterDefinition = {
  name: 'PAYLOAD',
  label: 'Payload content',
  type: 'STRING',
  operators: ['CONTAINS'],
  apiTypes: ['HTTP_PROXY', 'LLM', 'MCP'],
  signals: ['LOGS'],
};

const UNKNOWN_TYPE_DEF: FilterDefinition = {
  name: 'FUTURE_FILTER',
  label: 'Future Filter',
  type: 'BOOLEAN',
  operators: ['EQ', 'BETWEEN'],
};

const ALL_DEFINITIONS = [ENUM_DEF, NUMBER_DEF, KEYWORD_DEF, STRING_DEF, CONTAINS_STRING_DEF, UNKNOWN_TYPE_DEF];

class MockDefinitionProvider implements FilterDefinitionProvider {
  getDefinitions() {
    return of(ALL_DEFINITIONS);
  }
}

class MockValuesProvider implements FilterValuesProvider {
  getValues() {
    return of({ data: [], hasNextPage: false });
  }
}

describe('AddFilterDialogComponent', () => {
  let fixture: ComponentFixture<AddFilterDialogComponent>;
  let component: AddFilterDialogComponent;
  let dialogRefSpy: { close: jest.Mock };

  async function setup(data: AddFilterDialogData = {}, filterValuesProvider: FilterValuesProvider = new MockValuesProvider()) {
    dialogRefSpy = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [AddFilterDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: FILTER_DEFINITION_PROVIDER, useClass: MockDefinitionProvider },
        { provide: FILTER_VALUES_PROVIDER, useValue: filterValuesProvider },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddFilterDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  describe('field selection (Filter by autocomplete)', () => {
    beforeEach(() => setup());

    it('should expose all filter definitions on init', async () => {
      const filtered = await firstValueFrom(component['filteredDefinitions$']);
      expect(filtered.length).toBe(ALL_DEFINITIONS.length);
    });

    it('should sort filter definitions alphabetically by label', async () => {
      const filtered = await firstValueFrom(component['filteredDefinitions$']);
      const labels = filtered.map(d => d.label);
      const sorted = [...labels].sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }));
      expect(labels).toEqual(sorted);
    });

    it('should map known apiTypes for display and keep unknown tokens raw', () => {
      expect(component['formatApiTypeForDisplay']('HTTP_PROXY')).toBe('HTTP Proxy');
      expect(component['formatApiTypeForDisplay']('HTTP PROXY')).toBe('HTTP Proxy');
      expect(component['formatApiTypeForDisplay']('http_proxy')).toBe('HTTP Proxy');
      expect(component['formatApiTypeForDisplay']('MESSAGE')).toBe('Message');
      expect(component['formatApiTypeForDisplay']('LLM')).toBe('LLM');
      expect(component['formatApiTypeForDisplay']('MCP')).toBe('MCP');
    });

    it('should filter definitions by label search term', async () => {
      component['fieldControl'].setValue('Method');
      const filtered = await firstValueFrom(component['filteredDefinitions$']);
      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('HTTP_METHOD');
    });

    it('should filter definitions by apiType search term', async () => {
      component['fieldControl'].setValue('LLM');
      const filtered = await firstValueFrom(component['filteredDefinitions$']);
      expect(filtered.length).toBe(3);
      const names = filtered.map(d => d.name);
      expect(names).toContain('HTTP_METHOD');
      expect(names).toContain('API');
      expect(names).toContain('PAYLOAD');
    });

    it('should filter definitions by name (case insensitive)', async () => {
      component['fieldControl'].setValue('http_status');
      const filtered = await firstValueFrom(component['filteredDefinitions$']);
      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('HTTP_STATUS');
    });

    it('should clear field value on click when a definition is already selected', () => {
      component['selectDefinition'](ENUM_DEF);
      component['fieldControl'].setValue(ENUM_DEF, { emitEvent: false });

      component['onFieldInputClick']();

      expect(component['fieldControl'].value).toBe('');
    });
  });

  describe('operator selection', () => {
    beforeEach(async () => {
      await setup();
      component['selectDefinition'](ENUM_DEF);
      fixture.detectChanges();
    });

    it('should expose available operators', () => {
      expect(component['availableOperators']()).toEqual(['EQ', 'IN']);
    });

    it('should auto-select operator when only one is available', () => {
      component['selectDefinition'](STRING_DEF);
      expect(component['selectedOperator']()).toBe('EQ');
      expect(component['operatorControl'].value).toBe('EQ');
    });

    it('should not auto-select when multiple operators are available', () => {
      component['selectDefinition'](ENUM_DEF);
      expect(component['selectedOperator']()).toBeNull();
    });

    it('should reset operator when a new definition is selected', () => {
      component['selectedOperator'].set('EQ');
      component['selectDefinition'](NUMBER_DEF);
      expect(component['selectedOperator']()).toBeNull();
    });
  });

  describe('KEYWORD timeframe limit for value suggestions', () => {
    it('should pass dashboard bounds to keywordValuesTimeFrom/To when KEYWORD and limit is on', async () => {
      await setup({ timeFrom: 1000, timeTo: 2000 });
      component['selectDefinition'](KEYWORD_DEF);
      fixture.detectChanges();
      expect(component['hasKeywordTimeframeBounds']()).toBe(true);
      expect(component['limitKeywordValuesToTimeframe']()).toBe(true);
      expect(component['keywordValuesTimeFrom']()).toBe(1000);
      expect(component['keywordValuesTimeTo']()).toBe(2000);
    });

    it('should omit bounds from keywordValuesTimeFrom/To when limit is turned off', async () => {
      await setup({ timeFrom: 1000, timeTo: 2000 });
      component['selectDefinition'](KEYWORD_DEF);
      component['onLimitKeywordValuesToTimeframeChange']({ checked: false } as MatCheckboxChange);
      fixture.detectChanges();
      expect(component['keywordValuesTimeFrom']()).toBeUndefined();
      expect(component['keywordValuesTimeTo']()).toBeUndefined();
    });

    it('should show timeframe limit checkbox when KEYWORD is selected and dialog has bounds', async () => {
      await setup({ timeFrom: 1, timeTo: 2 });
      component['selectDefinition'](KEYWORD_DEF);
      component['selectedOperator'].set('IN');
      component['operatorControl'].setValue('IN', { emitEvent: false });
      fixture.detectChanges();
      const root = fixture.nativeElement as HTMLElement;
      expect(root.querySelector('mat-checkbox')).toBeTruthy();
      expect(root.textContent).toContain('Limit values');
    });

    it('should not show timeframe limit checkbox when dialog has no bounds', async () => {
      await setup({});
      component['selectDefinition'](KEYWORD_DEF);
      fixture.detectChanges();
      expect((fixture.nativeElement as HTMLElement).querySelector('mat-checkbox')).toBeFalsy();
    });

    it('should pass from/to to getValues after debounce when limit is on', fakeAsync(() => {
      const getValuesSpy = jest.fn().mockReturnValue(of({ data: [], hasNextPage: false }));
      dialogRefSpy = { close: jest.fn() };
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [AddFilterDialogComponent],
        providers: [
          provideNoopAnimations(),
          { provide: MAT_DIALOG_DATA, useValue: { timeFrom: 111, timeTo: 222 } satisfies AddFilterDialogData },
          { provide: MatDialogRef, useValue: dialogRefSpy },
          { provide: FILTER_DEFINITION_PROVIDER, useClass: MockDefinitionProvider },
          { provide: FILTER_VALUES_PROVIDER, useValue: { getValues: getValuesSpy } as FilterValuesProvider },
        ],
      });
      const f = TestBed.createComponent(AddFilterDialogComponent);
      const c = f.componentInstance;
      f.detectChanges();
      tick(0);
      c['selectDefinition'](KEYWORD_DEF);
      c['selectedOperator'].set('IN');
      f.detectChanges();
      tick(200);
      f.detectChanges();
      expect(getValuesSpy).toHaveBeenCalled();
      expect(getValuesSpy).toHaveBeenCalledWith(expect.objectContaining({ from: 111, to: 222, filterName: 'API', page: 1 }));
    }));
  });

  describe('KEYWORD filter value (chip-grid + CDK overlay for IN)', () => {
    it('should render chip-grid without mat-autocomplete for KEYWORD when operator is IN', async () => {
      await setup();
      component['selectDefinition'](KEYWORD_DEF);
      component['selectedOperator'].set('IN');
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const root = fixture.nativeElement as HTMLElement;
      expect(root.querySelectorAll('mat-select').length).toBe(1);
      const keyword = root.querySelector('gd-keyword-value-input');
      expect(keyword?.querySelector('mat-chip-grid')).toBeTruthy();
      expect(keyword?.querySelector('mat-autocomplete')).toBeFalsy();
      expect(keyword?.querySelector('input[matAutocomplete]')).toBeFalsy();
    });
  });

  describe('unknown type fallback', () => {
    beforeEach(async () => {
      await setup();
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
      component['selectDefinition'](UNKNOWN_TYPE_DEF);
      fixture.detectChanges();
      consoleSpy.mockRestore();
    });

    it('should fall back to STRING type for unknown filter types', () => {
      expect(component['resolvedFilterType']()).toBe('STRING');
    });

    it('should filter out unknown operators', () => {
      const operators = component['availableOperators']();
      expect(operators).toEqual(['EQ']);
      expect(operators).not.toContain('BETWEEN');
    });
  });

  describe('edit mode', () => {
    it('should restore existing condition in edit mode', async () => {
      await setup({
        existingCondition: {
          field: 'HTTP_METHOD',
          label: 'HTTP Method',
          operator: 'IN',
          values: ['GET', 'POST'],
        },
      });

      expect(component['selectedDefinition']()?.name).toBe('HTTP_METHOD');
      expect(component['selectedOperator']()).toBe('IN');
      expect(component['selectedValues']()).toEqual(['GET', 'POST']);
      expect(component['selectedValueLabels']()).toEqual(['GET', 'POST']);
      expect(component['fieldControl'].value).toEqual(expect.objectContaining({ name: 'HTTP_METHOD' }));
      expect(component['operatorControl'].value).toBe('IN');
    });

    it('should normalize IN to EQ when restoring a single value', async () => {
      await setup({
        existingCondition: {
          field: 'HTTP_METHOD',
          label: 'HTTP Method',
          operator: 'IN',
          values: ['GET'],
        },
      });

      expect(component['selectedOperator']()).toBe('EQ');
      expect(component['operatorControl'].value).toBe('EQ');
    });

    it('should switch the title to "Edit filter" in edit mode', async () => {
      await setup({
        existingCondition: { field: 'HTTP_METHOD', label: 'HTTP Method', operator: 'EQ', values: ['GET'] },
      });
      expect(component['isEditMode']()).toBe(true);
    });
  });

  describe('CONTAINS operator (PAYLOAD filter)', () => {
    beforeEach(async () => {
      await setup();
      component['selectDefinition'](CONTAINS_STRING_DEF);
      fixture.detectChanges();
    });

    it('should auto-select CONTAINS when it is the only operator', () => {
      expect(component['selectedOperator']()).toBe('CONTAINS');
      expect(component['operatorControl'].value).toBe('CONTAINS');
    });

    it('should expose CONTAINS as only available operator', () => {
      expect(component['availableOperators']()).toEqual(['CONTAINS']);
    });

    it('should resolve to STRING type for CONTAINS filter', () => {
      expect(component['resolvedFilterType']()).toBe('STRING');
    });

    it('should display operator label as "Contains"', () => {
      expect(component['operatorLabel']('CONTAINS')).toBe('Contains');
    });

    it('should confirm with CONTAINS operator and single value', () => {
      component['selectedValues'].set(['quantum']);
      fixture.detectChanges();

      component['confirm']();

      expect(dialogRefSpy.close).toHaveBeenCalledWith({
        field: 'PAYLOAD',
        label: 'Payload content',
        operator: 'CONTAINS',
        values: ['quantum'],
        valueLabels: ['quantum'],
      });
    });
  });

  describe('signal-exclusive badge', () => {
    beforeEach(() => setup());

    it('should return "Logs" for a filter with only LOGS signal', () => {
      expect(component['isSignalExclusive'](CONTAINS_STRING_DEF)).toBe('Logs');
    });

    it('should return null for a filter spanning multiple signals', () => {
      expect(component['isSignalExclusive'](ENUM_DEF)).toBeNull();
    });

    it('should return null for a filter without signals', () => {
      expect(component['isSignalExclusive'](STRING_DEF)).toBeNull();
    });
  });

  describe('confirm', () => {
    it('should close dialog with FilterCondition on confirm', async () => {
      await setup();
      component['selectDefinition'](ENUM_DEF);
      component['selectedOperator'].set('EQ');
      component['selectedValues'].set(['GET']);
      fixture.detectChanges();

      component['confirm']();

      expect(dialogRefSpy.close).toHaveBeenCalledWith({
        field: 'HTTP_METHOD',
        label: 'HTTP Method',
        operator: 'EQ',
        values: ['GET'],
        valueLabels: ['GET'],
      });
    });

    it('should emit IN when confirming with EQ and multiple values', async () => {
      await setup();
      component['selectDefinition'](ENUM_DEF);
      component['selectedOperator'].set('EQ');
      component['selectedValues'].set(['GET', 'POST']);
      fixture.detectChanges();

      component['confirm']();

      expect(dialogRefSpy.close).toHaveBeenCalledWith({
        field: 'HTTP_METHOD',
        label: 'HTTP Method',
        operator: 'IN',
        values: ['GET', 'POST'],
        valueLabels: ['GET', 'POST'],
      });
    });

    it('should not close the dialog when selection is incomplete', async () => {
      await setup();
      component['selectDefinition'](ENUM_DEF);
      // operator + values missing
      component['confirm']();

      expect(dialogRefSpy.close).not.toHaveBeenCalled();
    });

    it('canConfirm should be false until a definition, operator, and values are all provided', async () => {
      await setup();
      expect(component['canConfirm']()).toBe(false);

      component['selectDefinition'](ENUM_DEF);
      expect(component['canConfirm']()).toBe(false);

      component['selectedOperator'].set('EQ');
      expect(component['canConfirm']()).toBe(false);

      component['selectedValues'].set(['GET']);
      expect(component['canConfirm']()).toBe(true);
    });
  });
});
