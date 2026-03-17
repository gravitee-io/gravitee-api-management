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

import { GmdCheckboxGroupComponent } from './gmd-checkbox-group.component';
import { GmdCheckboxGroupComponentHarness } from './gmd-checkbox-group.component.harness';
import { GMD_FORM_STATE_STORE } from '../../services/gmd-form-state.store';

@Component({
  template: `
    <gmd-checkbox-group
      [fieldKey]="fieldKey()"
      [name]="name()"
      [label]="label()"
      [value]="value()"
      [required]="required()"
      [options]="options()"
      [disabled]="disabled()"
    />
  `,
  standalone: true,
  imports: [GmdCheckboxGroupComponent],
})
class TestHostComponent {
  fieldKey = input<string | undefined>();
  name = input<string>('test-checkbox-group');
  label = input<string | undefined>();
  value = input<string | undefined>();
  required = input<boolean>(false);
  options = input<string>('');
  disabled = input<boolean>(false);
}

describe('GmdCheckboxGroupComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let checkboxGroupComponent: GmdCheckboxGroupComponent;
  let loader: HarnessLoader;
  let harness: GmdCheckboxGroupComponentHarness;

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
    checkboxGroupComponent = fixture.debugElement.query(p => p.name === 'gmd-checkbox-group')?.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GmdCheckboxGroupComponentHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(checkboxGroupComponent).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(checkboxGroupComponent.value()).toBeUndefined();
    expect(checkboxGroupComponent.required()).toBe(false);
    expect(checkboxGroupComponent.disabled()).toBe(false);
  });

  it('should display label when provided', async () => {
    fixture.componentRef.setInput('label', 'Test Checkbox Group');
    fixture.detectChanges();

    const label = await harness.getLabel();
    expect(label).toContain('Test Checkbox Group');
  });

  it('should show required indicator when required is true', async () => {
    fixture.componentRef.setInput('label', 'Test Checkbox Group');
    fixture.componentRef.setInput('required', true);
    fixture.detectChanges();

    const hasIndicator = await harness.hasRequiredIndicator();
    expect(hasIndicator).toBe(true);
  });

  describe('Options parsing', () => {
    it('should parse comma-separated options', () => {
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.detectChanges();

      const options = checkboxGroupComponent.optionsVM();
      expect(options).toEqual(['option1', 'option2', 'option3']);
    });

    it('should handle empty options', () => {
      fixture.componentRef.setInput('options', '');
      fixture.detectChanges();

      const options = checkboxGroupComponent.optionsVM();
      expect(options).toEqual([]);
    });

    it('should trim whitespace from comma-separated options', () => {
      fixture.componentRef.setInput('options', 'option1 , option2 , option3');
      fixture.detectChanges();

      const options = checkboxGroupComponent.optionsVM();
      expect(options).toEqual(['option1', 'option2', 'option3']);
    });

    it('should render options in template', async () => {
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.detectChanges();

      const options = await harness.getOptions();
      expect(options).toHaveLength(3);
    });
  });

  describe('Toggle behavior', () => {
    it('should toggle a checkbox on click', async () => {
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.detectChanges();

      expect(await harness.isChecked('option1')).toBe(false);

      await harness.toggle('option1');
      fixture.detectChanges();

      expect(await harness.isChecked('option1')).toBe(true);
    });

    it('should allow multiple selections', async () => {
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.detectChanges();

      await harness.toggle('option1');
      await harness.toggle('option3');
      fixture.detectChanges();

      const selected = await harness.getSelectedValues();
      expect(selected).toContain('option1');
      expect(selected).toContain('option3');
      expect(selected).not.toContain('option2');
    });

    it('should deselect a checkbox on second click', async () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      await harness.toggle('option1');
      fixture.detectChanges();
      expect(await harness.isChecked('option1')).toBe(true);

      await harness.toggle('option1');
      fixture.detectChanges();
      expect(await harness.isChecked('option1')).toBe(false);
    });
  });

  describe('Value preselection', () => {
    it('should preselect a single value from comma-separated input', async () => {
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.componentRef.setInput('value', 'option2');
      fixture.detectChanges();

      expect(await harness.isChecked('option2')).toBe(true);
      expect(await harness.isChecked('option1')).toBe(false);
    });

    it('should preselect multiple values from comma-separated input', async () => {
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.componentRef.setInput('value', 'option1,option3');
      fixture.detectChanges();

      expect(await harness.isChecked('option1')).toBe(true);
      expect(await harness.isChecked('option3')).toBe(true);
      expect(await harness.isChecked('option2')).toBe(false);
    });

    it('should sync and update value from input when changed', async () => {
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.componentRef.setInput('value', 'option1');
      fixture.detectChanges();

      expect(await harness.isChecked('option1')).toBe(true);

      fixture.componentRef.setInput('value', 'option2,option3');
      fixture.detectChanges();

      expect(await harness.isChecked('option1')).toBe(false);
      expect(await harness.isChecked('option2')).toBe(true);
      expect(await harness.isChecked('option3')).toBe(true);
    });
  });

  describe('Validation', () => {
    it('should be valid when not required and nothing selected', () => {
      fixture.componentRef.setInput('required', false);
      fixture.detectChanges();

      expect(checkboxGroupComponent.valid()).toBe(true);
      expect(checkboxGroupComponent.validationErrors().length).toBe(0);
    });

    it('should be invalid when required and nothing selected', () => {
      fixture.componentRef.setInput('required', true);
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      expect(checkboxGroupComponent.valid()).toBe(false);
      expect(checkboxGroupComponent.validationErrors()).toContain('required');
    });

    it('should be valid when required and at least one option selected', async () => {
      fixture.componentRef.setInput('required', true);
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      await harness.toggle('option1');
      fixture.detectChanges();

      expect(checkboxGroupComponent.valid()).toBe(true);
      expect(checkboxGroupComponent.validationErrors().length).toBe(0);
    });

    it('should show error messages when touched and invalid', async () => {
      fixture.componentRef.setInput('required', true);
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      await harness.blur();
      fixture.detectChanges();

      const errorMessages = await harness.getErrorMessages();
      expect(errorMessages.length).toBeGreaterThan(0);
      expect(errorMessages[0]).toBe('This field is required.');
    });

    it('should not show errors before touched', () => {
      fixture.componentRef.setInput('required', true);
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      const errorContainer = fixture.nativeElement.querySelector('.gmd-checkbox-group__errors');
      expect(errorContainer).toBeNull();
    });
  });

  describe('Field state serialization', () => {
    it('should emit comma-separated value of selected items', async () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.detectChanges();

      await harness.toggle('option1');
      await harness.toggle('option3');
      fixture.detectChanges();

      expect(mockStore.updateField).toHaveBeenLastCalledWith(
        expect.objectContaining({
          fieldKey: 'test-key',
          value: 'option1,option3',
          valid: true,
        }),
      );
    });

    it('should emit empty string when nothing is selected', () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      expect(mockStore.updateField).toHaveBeenLastCalledWith(
        expect.objectContaining({
          fieldKey: 'test-key',
          value: '',
        }),
      );
    });
  });

  describe('Disabled state', () => {
    it('should be disabled when disabled input is true', async () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.componentRef.setInput('disabled', true);
      fixture.detectChanges();

      const isDisabled = await harness.isDisabled();
      expect(isDisabled).toBe(true);
    });

    it('should remove field from store when disabled', () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.componentRef.setInput('disabled', true);
      fixture.detectChanges();

      expect(mockStore.removeField).toHaveBeenCalled();
    });
  });

  describe('Store updates', () => {
    it('should update store with validation errors when required and invalid', async () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.componentRef.setInput('required', true);
      fixture.componentRef.setInput('options', 'option1,option2');
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

  describe('ARIA attributes', () => {
    it('should use fieldset/legend for semantic HTML', () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.componentRef.setInput('label', 'Choose options');
      fixture.detectChanges();

      const fieldset = fixture.nativeElement.querySelector('fieldset.gmd-checkbox-group');
      const legend = fixture.nativeElement.querySelector('legend.gmd-checkbox-group__group-label');
      expect(fieldset).toBeTruthy();
      expect(legend).toBeTruthy();
    });

    it('should set aria-required on fieldset when required', async () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      const ariaRequired = await harness.getAriaRequired();
      expect(ariaRequired).toBe('true');
    });

    it('should set aria-invalid and aria-describedby on fieldset when has errors', async () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      await harness.blur();
      fixture.detectChanges();

      const ariaInvalid = await harness.getAriaInvalid();
      const ariaDescribedby = await harness.getAriaDescribedby();
      expect(ariaInvalid).toBe('true');
      expect(ariaDescribedby).toBeTruthy();
    });
  });
});
