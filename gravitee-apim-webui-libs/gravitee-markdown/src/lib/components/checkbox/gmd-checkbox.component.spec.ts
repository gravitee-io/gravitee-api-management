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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdCheckboxComponent } from './gmd-checkbox.component';
import { GmdCheckboxComponentHarness } from './gmd-checkbox.component.harness';
import { GMD_FORM_STATE_STORE } from '../../services/gmd-form-state.store';

@Component({
  template: `
    <gmd-checkbox
      [fieldKey]="fieldKey()"
      [name]="name()"
      [label]="label()"
      [value]="value()"
      [required]="required()"
      [readonly]="readonly()"
      [disabled]="disabled()"
    />
  `,
  standalone: true,
  imports: [GmdCheckboxComponent],
})
class TestHostComponent {
  fieldKey = input<string | undefined>();
  name = input<string>('test-checkbox');
  label = input<string | undefined>();
  value = input<boolean>(false);
  required = input<boolean>(false);
  readonly = input<boolean>(false);
  disabled = input<boolean>(false);
}

describe('GmdCheckboxComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let checkboxComponent: GmdCheckboxComponent;
  let harness: GmdCheckboxComponentHarness;

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
    checkboxComponent = fixture.debugElement.query(p => p.name === 'gmd-checkbox')?.componentInstance;
    const loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GmdCheckboxComponentHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(checkboxComponent).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(checkboxComponent.value()).toBe(false);
    expect(checkboxComponent.required()).toBe(false);
    expect(checkboxComponent.readonly()).toBe(false);
    expect(checkboxComponent.disabled()).toBe(false);
  });

  it('should display label when provided', async () => {
    fixture.componentRef.setInput('name', 'test-checkbox');
    fixture.componentRef.setInput('label', 'Test Checkbox');
    fixture.detectChanges();

    const label = await harness.getLabel();
    expect(label).toBe('Test Checkbox');
  });

  it('should show required indicator when required is true', async () => {
    fixture.componentRef.setInput('label', 'Test Checkbox');
    fixture.componentRef.setInput('required', true);
    fixture.detectChanges();

    expect(await harness.hasRequiredIndicator()).toBe(true);
    expect(await harness.isRequired()).toBe(true);
  });

  it('should update value when checkbox is clicked', async () => {
    expect(await harness.isChecked()).toBe(false);

    await harness.click();
    fixture.detectChanges();

    expect(await harness.isChecked()).toBe(true);
  });

  it('should be disabled when disabled input is true', async () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    expect(await harness.isDisabled()).toBe(true);
  });

  it('should be readonly when readonly input is true', async () => {
    fixture.componentRef.setInput('readonly', true);
    fixture.detectChanges();

    expect(await harness.isReadonly()).toBe(true);
  });

  describe('Validation', () => {
    it('should be valid when not required and unchecked', () => {
      fixture.componentRef.setInput('required', false);
      fixture.detectChanges();

      expect(checkboxComponent.valid()).toBe(true);
      expect(checkboxComponent.validationErrors().length).toBe(0);
    });

    it('should be invalid when required and unchecked', () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      expect(checkboxComponent.valid()).toBe(false);
      expect(checkboxComponent.validationErrors()).toContain('required');
    });

    it('should be valid when required and checked', () => {
      fixture.componentRef.setInput('required', true);
      fixture.componentRef.setInput('value', true);
      fixture.detectChanges();

      expect(checkboxComponent.valid()).toBe(true);
      expect(checkboxComponent.validationErrors().length).toBe(0);
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

      await harness.click();
      fixture.detectChanges();

      expect(mockStore.updateField).toHaveBeenLastCalledWith(
        expect.objectContaining({
          fieldKey: 'test-key',
          value: 'true',
          valid: true,
          required: false,
        }),
      );
    });

    it('should update store with correct state when required and invalid', async () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      await harness.blur();
      fixture.detectChanges();

      expect(mockStore.updateField).toHaveBeenLastCalledWith(
        expect.objectContaining({
          fieldKey: 'test-key',
          value: 'false',
          valid: false,
          required: true,
          validationErrors: expect.arrayContaining(['required']),
        }),
      );
    });
  });

  describe('Initial value synchronization', () => {
    it('should sync and update value from input', async () => {
      fixture.componentRef.setInput('value', false);
      fixture.detectChanges();

      expect(await harness.isChecked()).toBe(false);

      fixture.componentRef.setInput('value', true);
      fixture.detectChanges();

      expect(await harness.isChecked()).toBe(true);
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
