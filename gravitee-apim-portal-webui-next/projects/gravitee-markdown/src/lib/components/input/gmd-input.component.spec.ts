/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdInputComponent } from './gmd-input.component';
import { GmdInputComponentHarness } from './gmd-input.component.harness';
import { GMD_FORM_STATE_STORE } from '../../services/gmd-form-state.store';

@Component({
  template: `
    <gmd-input
      [fieldKey]="fieldKey()"
      [name]="name()"
      [label]="label()"
      [placeholder]="placeholder()"
      [value]="value()"
      [required]="required()"
      [minLength]="minLength()"
      [maxLength]="maxLength()"
      [pattern]="pattern()" />
  `,
  standalone: true,
  imports: [GmdInputComponent],
})
class TestHostComponent {
  fieldKey = input<string | undefined>();
  name = input<string>('test-input');
  label = input<string | undefined>();
  placeholder = input<string | undefined>();
  value = input<string | undefined>();
  required = input<boolean>(false);
  minLength = input<number | string | undefined>();
  maxLength = input<number | string | undefined>();
  pattern = input<string | undefined>();
}

describe('GmdInputComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let inputComponent: GmdInputComponent;
  let loader: HarnessLoader;
  let harness: GmdInputComponentHarness;

  let mockStore: { updateField: jest.Mock; removeField: jest.Mock };

  beforeEach(async () => {
    mockStore = {
      updateField: jest.fn(),
      removeField: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [{ provide: GMD_FORM_STATE_STORE, useValue: mockStore }],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    inputComponent = fixture.debugElement.query(p => p.name === 'gmd-input')?.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GmdInputComponentHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(inputComponent).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(inputComponent.value()).toBeUndefined();
    expect(inputComponent.required()).toBe(false);
  });

  it('should display label when provided', async () => {
    fixture.componentRef.setInput('label', 'Test Input');
    fixture.detectChanges();

    const label = await harness.getLabel();
    expect(label).toContain('Test Input');
  });

  it('should show placeholder when provided', async () => {
    fixture.componentRef.setInput('placeholder', 'Enter text here');
    fixture.detectChanges();

    const placeholder = await harness.getPlaceholder();
    expect(placeholder).toBe('Enter text here');
  });

  it('should show required indicator when required is true', async () => {
    fixture.componentRef.setInput('label', 'Test Input');
    fixture.componentRef.setInput('required', true);
    fixture.detectChanges();

    const hasRequired = await harness.hasRequiredIndicator();
    expect(hasRequired).toBe(true);
  });

  it('should update value when input changes', async () => {
    await harness.setValue('test value');
    fixture.detectChanges();

    const value = await harness.getValue();
    expect(value).toBe('test value');
  });

  describe('Validation', () => {
    it('should be valid when not required and empty', () => {
      fixture.componentRef.setInput('required', false);
      fixture.detectChanges();

      expect(inputComponent.valid()).toBe(true);
      expect(inputComponent.validationErrors().length).toBe(0);
    });

    it('should be invalid when required and empty', () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      expect(inputComponent.valid()).toBe(false);
      expect(inputComponent.validationErrors()).toContain('required');
    });

    it('should be valid when required and has value', async () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      await harness.setValue('test');
      fixture.detectChanges();

      expect(inputComponent.valid()).toBe(true);
      expect(inputComponent.validationErrors().length).toBe(0);
    });

    it('should validate minLength', async () => {
      fixture.componentRef.setInput('minLength', 5);
      fixture.detectChanges();

      await harness.setValue('test');
      fixture.detectChanges();

      expect(inputComponent.valid()).toBe(false);
      expect(inputComponent.validationErrors()).toContain('minLength');
    });

    it('should validate maxLength', async () => {
      fixture.componentRef.setInput('maxLength', 5);
      fixture.detectChanges();

      await harness.setValue('too long value');
      fixture.detectChanges();

      expect(inputComponent.valid()).toBe(false);
      expect(inputComponent.validationErrors()).toContain('maxLength');
    });

    it('should validate pattern', async () => {
      fixture.componentRef.setInput('pattern', '^[0-9]+$');
      fixture.detectChanges();

      await harness.setValue('abc123');
      fixture.detectChanges();

      expect(inputComponent.valid()).toBe(false);
      expect(inputComponent.validationErrors()).toContain('pattern');
    });

    it('should show error messages when touched and invalid', async () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      await harness.blur();
      fixture.detectChanges();

      const errors = await harness.getErrorMessages();
      expect(errors.length).toBeGreaterThan(0);
      expect(errors[0]).toBe('This field is required.');
    });
  });

  describe('Store updates', () => {
    it('should update store when fieldKey is set and value changes', async () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.detectChanges();

      await harness.setValue('test value');
      fixture.detectChanges();

      expect(mockStore.updateField).toHaveBeenLastCalledWith(
        expect.objectContaining({
          fieldKey: 'test-key',
          value: 'test value',
          valid: true,
        }),
      );
    });

    it('should update store with validation errors when invalid', async () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      await harness.blur();
      fixture.detectChanges();

      expect(mockStore.updateField).toHaveBeenLastCalledWith(
        expect.objectContaining({
          fieldKey: 'test-key',
          valid: false,
          validationErrors: expect.arrayContaining(['required']),
        }),
      );
    });
  });

  describe('Initial value synchronization', () => {
    it('should sync and update value from input', async () => {
      fixture.componentRef.setInput('value', 'first value');
      fixture.detectChanges();

      let value = await harness.getValue();
      expect(value).toBe('first value');

      fixture.componentRef.setInput('value', 'second value');
      fixture.detectChanges();

      value = await harness.getValue();
      expect(value).toBe('second value');
    });

    it('should handle undefined value', async () => {
      fixture.componentRef.setInput('value', undefined);
      fixture.detectChanges();

      const value = await harness.getValue();
      expect(value).toBe('');
    });
  });

  describe('Number-like validation inputs', () => {
    it.each([
      { input: 5, expected: '5', type: 'numeric' },
      { input: '10', expected: '10', type: 'string' },
    ])('should accept $type minLength', async ({ input, expected }) => {
      fixture.componentRef.setInput('minLength', input);
      fixture.detectChanges();

      const minLength = await harness.getMinLength();
      expect(minLength).toBe(expected);
    });

    it.each([
      { input: 20, expected: '20', type: 'numeric' },
      { input: '30', expected: '30', type: 'string' },
    ])('should accept $type maxLength', async ({ input, expected }) => {
      fixture.componentRef.setInput('maxLength', input);
      fixture.detectChanges();

      const maxLength = await harness.getMaxLength();
      expect(maxLength).toBe(expected);
    });
  });

  describe('ARIA attributes', () => {
    it('should set aria-required when required', async () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      const ariaRequired = await harness.getAriaRequired();
      expect(ariaRequired).toBe('true');
    });

    it('should set aria-invalid and aria-describedby when has errors', async () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      await harness.blur();
      fixture.detectChanges();

      const ariaInvalid = await harness.getAriaInvalid();
      const ariaDescribedby = await harness.getAriaDescribedby();
      expect(ariaInvalid).toBe('true');
      expect(ariaDescribedby).toBeTruthy();
    });

    it('should not set aria-invalid when valid', async () => {
      fixture.componentRef.setInput('required', false);
      fixture.detectChanges();

      await harness.blur();
      fixture.detectChanges();

      const ariaInvalid = await harness.getAriaInvalid();
      expect(ariaInvalid).toBeNull();
    });
  });
});
