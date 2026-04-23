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

const UNKNOWN_TYPE_DEF: FilterDefinition = {
  name: 'FUTURE_FILTER',
  label: 'Future Filter',
  type: 'BOOLEAN',
  operators: ['EQ', 'BETWEEN'],
};

const ALL_DEFINITIONS = [ENUM_DEF, NUMBER_DEF, KEYWORD_DEF, STRING_DEF, UNKNOWN_TYPE_DEF];

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

  async function setup(data: AddFilterDialogData = {}) {
    dialogRefSpy = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [AddFilterDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: FILTER_DEFINITION_PROVIDER, useClass: MockDefinitionProvider },
        { provide: FILTER_VALUES_PROVIDER, useClass: MockValuesProvider },
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

    it('should filter definitions by label search term', async () => {
      component['fieldControl'].setValue('Method');
      const filtered = await firstValueFrom(component['filteredDefinitions$']);
      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('HTTP_METHOD');
    });

    it('should filter definitions by apiType search term', async () => {
      component['fieldControl'].setValue('LLM');
      const filtered = await firstValueFrom(component['filteredDefinitions$']);
      expect(filtered.length).toBe(2);
      const names = filtered.map(d => d.name);
      expect(names).toContain('HTTP_METHOD');
      expect(names).toContain('API');
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
