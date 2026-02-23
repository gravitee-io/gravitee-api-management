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
import { GmdFormStateStore } from './gmd-form-state.store';
import { GmdFieldState } from '../models/formField';

describe('GmdFormStateStore', () => {
  let store: GmdFormStateStore;

  const mockState1: GmdFieldState = {
    id: 'comp-1',
    fieldKey: 'field1',
    value: 'test',
    valid: true,
    required: true,
    touched: false,
    validationErrors: [],
  };

  const mockState2: GmdFieldState = {
    id: 'comp-2',
    fieldKey: 'field2',
    value: '',
    valid: false,
    required: true,
    touched: true,
    validationErrors: ['required'],
  };

  beforeEach(() => {
    store = new GmdFormStateStore();
  });

  it('should start with an empty map', () => {
    expect(store.fieldsMap().size).toBe(0);
  });

  it('should add a field', () => {
    store.updateField(mockState1);
    expect(store.fieldsMap().size).toBe(1);
    expect(store.fieldsMap().get('comp-1')).toEqual(mockState1);
  });

  it('should update an existing field', () => {
    store.updateField(mockState1);

    const updatedState = { ...mockState1, value: 'updated' };
    store.updateField(updatedState);

    expect(store.fieldsMap().size).toBe(1);
    expect(store.fieldsMap().get('comp-1')).toEqual(updatedState);
  });

  it('should not update if state is identical (optimization)', () => {
    store.updateField(mockState1);
    const mapBefore = store.fieldsMap();

    store.updateField({ ...mockState1 });
    const mapAfter = store.fieldsMap();

    // Reference equality check on the map itself
    expect(mapAfter).toBe(mapBefore);
  });

  it('should remove a field', () => {
    store.updateField(mockState1);
    store.updateField(mockState2);
    expect(store.fieldsMap().size).toBe(2);

    store.removeField('comp-1');
    expect(store.fieldsMap().size).toBe(1);
    expect(store.fieldsMap().has('comp-1')).toBe(false);
    expect(store.fieldsMap().get('comp-2')).toEqual(mockState2);
  });

  it('should reset the store', () => {
    store.updateField(mockState1);
    store.updateField(mockState2);

    store.reset();
    expect(store.fieldsMap().size).toBe(0);
  });

  describe('Computed Diagnostics', () => {
    describe('formValid', () => {
      it('should return true when there are no fields', () => {
        expect(store.formValid()).toBe(true);
      });

      it('should return true when all required fields are valid', () => {
        store.updateField(mockState1); // required and valid
        expect(store.formValid()).toBe(true);
      });

      it('should return false when a required field is invalid', () => {
        store.updateField(mockState2); // required and invalid
        expect(store.formValid()).toBe(false);
      });

      it('should ignore optional fields validity', () => {
        const optionalInvalid: GmdFieldState = {
          id: 'comp-3',
          fieldKey: 'optional',
          value: '',
          valid: false,
          required: false,
          touched: false,
          validationErrors: [],
        };
        store.updateField(optionalInvalid);
        expect(store.formValid()).toBe(true);
      });
    });

    describe('requiredFieldsCount', () => {
      it('should return 0 when there are no fields', () => {
        expect(store.requiredFieldsCount()).toBe(0);
      });

      it('should count only required fields', () => {
        store.updateField(mockState1); // required
        store.updateField({ ...mockState2, required: false }); // optional
        expect(store.requiredFieldsCount()).toBe(1);
      });

      it('should count all required fields', () => {
        store.updateField(mockState1);
        store.updateField(mockState2);
        expect(store.requiredFieldsCount()).toBe(2);
      });
    });

    describe('validRequiredFieldsCount', () => {
      it('should return 0 when there are no fields', () => {
        expect(store.validRequiredFieldsCount()).toBe(0);
      });

      it('should count only valid required fields', () => {
        store.updateField(mockState1); // required and valid
        store.updateField(mockState2); // required and invalid
        expect(store.validRequiredFieldsCount()).toBe(1);
      });

      it('should not count valid optional fields', () => {
        store.updateField({ ...mockState1, required: false }); // optional and valid
        expect(store.validRequiredFieldsCount()).toBe(0);
      });
    });

    describe('invalidFields', () => {
      it('should return empty array when all fields are valid', () => {
        store.updateField(mockState1);
        expect(store.invalidFields()).toEqual([]);
      });

      it('should return invalid fields with their errors', () => {
        store.updateField(mockState2);
        const invalid = store.invalidFields();

        expect(invalid.length).toBe(1);
        expect(invalid[0]).toEqual({
          id: 'comp-2',
          fieldKey: 'field2',
          errors: ['required'],
        });
      });

      it('should not include valid fields', () => {
        store.updateField(mockState1); // valid
        store.updateField(mockState2); // invalid

        const invalid = store.invalidFields();
        expect(invalid.length).toBe(1);
        expect(invalid[0].id).toBe('comp-2');
      });
    });

    describe('fieldValues', () => {
      it('should return empty array when there are no fields', () => {
        expect(store.fieldValues()).toEqual([]);
      });

      it('should return all field values', () => {
        store.updateField(mockState1);
        store.updateField(mockState2);

        const values = store.fieldValues();
        expect(values.length).toBe(2);
        expect(values).toEqual([
          { id: 'comp-1', fieldKey: 'field1', value: 'test' },
          { id: 'comp-2', fieldKey: 'field2', value: '' },
        ]);
      });
    });

    describe('duplicateFieldKeys', () => {
      it('should return empty array when there are no duplicates', () => {
        store.updateField(mockState1);
        store.updateField(mockState2);
        expect(store.duplicateFieldKeys()).toEqual([]);
      });

      it('should detect duplicate field keys', () => {
        store.updateField(mockState1);
        store.updateField({ ...mockState2, fieldKey: 'field1' }); // same key as mockState1

        const duplicates = store.duplicateFieldKeys();
        expect(duplicates.length).toBe(1);
        expect(duplicates[0]).toEqual({ key: 'field1', count: 2 });
      });

      it('should ignore empty field keys', () => {
        store.updateField({ ...mockState1, fieldKey: '' });
        store.updateField({ ...mockState2, fieldKey: '' });
        expect(store.duplicateFieldKeys()).toEqual([]);
      });

      it('should ignore whitespace-only field keys', () => {
        store.updateField({ ...mockState1, fieldKey: '   ' });
        store.updateField({ ...mockState2, fieldKey: '  ' });
        expect(store.duplicateFieldKeys()).toEqual([]);
      });
    });

    describe('allConfigErrors', () => {
      it('should return empty array when there are no errors', () => {
        store.updateField(mockState1);
        expect(store.allConfigErrors()).toEqual([]);
      });

      it('should include duplicate key errors', () => {
        store.updateField(mockState1);
        store.updateField({ ...mockState2, fieldKey: 'field1' });

        const errors = store.allConfigErrors();
        expect(errors.length).toBe(1);
        expect(errors[0]).toMatchObject({
          code: 'duplicateKey',
          severity: 'error',
          field: 'fieldKey',
          fieldKey: 'field1',
        });
      });

      it('should include field-specific config errors', () => {
        const stateWithError: GmdFieldState = {
          ...mockState1,
          configErrors: [
            {
              code: 'invalidRegex',
              message: 'Invalid pattern',
              severity: 'error',
              field: 'pattern',
            },
          ],
        };
        store.updateField(stateWithError);

        const errors = store.allConfigErrors();
        expect(errors.length).toBe(1);
        expect(errors[0]).toMatchObject({
          code: 'invalidRegex',
          severity: 'error',
          fieldKey: 'field1',
        });
      });

      it('should combine duplicate errors and field errors', () => {
        store.updateField({
          ...mockState1,
          configErrors: [
            {
              code: 'invalidRegex',
              message: 'Invalid pattern',
              severity: 'error',
              field: 'pattern',
            },
          ],
        });
        store.updateField({ ...mockState2, fieldKey: 'field1' }); // duplicate

        const errors = store.allConfigErrors();
        expect(errors.length).toBe(2);
      });
    });

    describe('criticalConfigErrors', () => {
      it('should return only errors with severity "error"', () => {
        const stateWithErrors: GmdFieldState = {
          ...mockState1,
          configErrors: [
            {
              code: 'invalidRegex',
              message: 'Error',
              severity: 'error',
              field: 'pattern',
            },
            {
              code: 'normalizedValue',
              message: 'Warning',
              severity: 'warning',
              field: 'minLength',
            },
          ],
        };
        store.updateField(stateWithErrors);

        const critical = store.criticalConfigErrors();
        expect(critical.length).toBe(1);
        expect(critical[0].severity).toBe('error');
      });
    });

    describe('configWarnings', () => {
      it('should return only errors with severity "warning"', () => {
        const stateWithWarnings: GmdFieldState = {
          ...mockState1,
          configErrors: [
            {
              code: 'invalidRegex',
              message: 'Error',
              severity: 'error',
              field: 'pattern',
            },
            {
              code: 'normalizedValue',
              message: 'Warning',
              severity: 'warning',
              field: 'minLength',
            },
          ],
        };
        store.updateField(stateWithWarnings);

        const warnings = store.configWarnings();
        expect(warnings.length).toBe(1);
        expect(warnings[0].severity).toBe('warning');
      });
    });
  });
});
