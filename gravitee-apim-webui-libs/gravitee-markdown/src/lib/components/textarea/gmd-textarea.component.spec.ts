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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdTextareaComponent } from './gmd-textarea.component';
import { GmdTextareaComponentHarness } from './gmd-textarea.component.harness';
import { GMD_FORM_STATE_STORE } from '../../services/gmd-form-state.store';

@Component({
  template: `
    <gmd-textarea
      [fieldKey]="fieldKey()"
      [name]="name()"
      [label]="label()"
      [placeholder]="placeholder()"
      [value]="value()"
      [required]="required()"
      [minLength]="minLength()"
      [maxLength]="maxLength()"
      [rows]="rows()"
      [readonly]="readonly()"
      [disabled]="disabled()"
    />
  `,
  standalone: true,
  imports: [GmdTextareaComponent],
})
class TestHostComponent {
  fieldKey = input<string | undefined>();
  name = input<string>('test-textarea');
  label = input<string | undefined>();
  placeholder = input<string | undefined>();
  value = input<string | undefined>();
  required = input<boolean>(false);
  minLength = input<number | string | undefined>();
  maxLength = input<number | string | undefined>();
  rows = input<number>(4);
  readonly = input<boolean>(false);
  disabled = input<boolean>(false);
}

describe('GmdTextareaComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let textareaComponent: GmdTextareaComponent;
  let loader: HarnessLoader;
  let harness: GmdTextareaComponentHarness;

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
    textareaComponent = fixture.debugElement.query(p => p.name === 'gmd-textarea')?.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GmdTextareaComponentHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(textareaComponent).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(textareaComponent.value()).toBeUndefined();
    expect(textareaComponent.required()).toBe(false);
    expect(textareaComponent.disabled()).toBe(false);
    expect(textareaComponent.readonly()).toBe(false);
    expect(textareaComponent.rows()).toBe(4);
  });

  it('should display label when provided', async () => {
    fixture.componentRef.setInput('label', 'Test Textarea');
    fixture.detectChanges();

    const label = await harness.getLabel();
    expect(label).toContain('Test Textarea');
  });

  it('should show placeholder when provided', async () => {
    fixture.componentRef.setInput('placeholder', 'Enter text here');
    fixture.detectChanges();

    const placeholder = await harness.getPlaceholder();
    expect(placeholder).toBe('Enter text here');
  });

  it('should show required indicator when required is true', async () => {
    fixture.componentRef.setInput('label', 'Test Textarea');
    fixture.componentRef.setInput('required', true);
    fixture.detectChanges();

    const hasIndicator = await harness.hasRequiredIndicator();
    expect(hasIndicator).toBe(true);
  });

  it('should update value when textarea input changes', async () => {
    await harness.setValue('test value');
    fixture.detectChanges();

    const value = await harness.getValue();
    expect(value).toBe('test value');
  });

  it('should set rows attribute', async () => {
    fixture.componentRef.setInput('rows', 10);
    fixture.detectChanges();

    const rows = await harness.getRows();
    expect(rows).toBe(10);
  });

  it('should be readonly when readonly input is true', async () => {
    fixture.componentRef.setInput('readonly', true);
    fixture.detectChanges();

    const isReadonly = await harness.isReadonly();
    expect(isReadonly).toBe(true);
  });

  it('should be disabled when disabled input is true', async () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const isDisabled = await harness.isDisabled();
    expect(isDisabled).toBe(true);
  });

  describe('Validation', () => {
    it('should be valid when not required and empty', () => {
      fixture.componentRef.setInput('required', false);
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(true);
      expect(textareaComponent.validationErrors().length).toBe(0);
    });

    it('should be invalid when required and empty', () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(false);
      expect(textareaComponent.validationErrors()).toContain('required');
    });

    it('should be valid when required and has value', () => {
      fixture.componentRef.setInput('required', true);
      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.value = 'test';
      textarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(true);
      expect(textareaComponent.validationErrors().length).toBe(0);
    });

    it('should validate minLength', () => {
      fixture.componentRef.setInput('minLength', 5);
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.value = 'test';
      textarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(false);
      expect(textareaComponent.validationErrors()).toContain('minLength');
    });

    it('should validate maxLength', () => {
      fixture.componentRef.setInput('maxLength', 5);
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.value = 'too long value';
      textarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(false);
      expect(textareaComponent.validationErrors()).toContain('maxLength');
    });

    it('should show error messages when touched and invalid', async () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      await harness.blur();
      fixture.detectChanges();

      const errorMessages = await harness.getErrorMessages();
      expect(errorMessages.length).toBeGreaterThan(0);
      expect(errorMessages[0]).toBe('This field is required.');
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

      expect(await harness.getValue()).toBe('first value');

      fixture.componentRef.setInput('value', 'second value');
      fixture.detectChanges();

      expect(await harness.getValue()).toBe('second value');
    });

    it('should handle undefined value', async () => {
      fixture.componentRef.setInput('value', undefined);
      fixture.detectChanges();

      expect(await harness.getValue()).toBe('');
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
