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

import { GmdRadioComponent } from './gmd-radio.component';
import { GmdRadioComponentHarness } from './gmd-radio.component.harness';
import { GMD_FORM_STATE_STORE } from '../../services/gmd-form-state.store';

@Component({
  template: `
    <gmd-radio
      [fieldKey]="fieldKey()"
      [name]="name()"
      [label]="label()"
      [value]="value()"
      [required]="required()"
      [options]="options()"
      [readonly]="readonly()"
      [disabled]="disabled()" />
  `,
  standalone: true,
  imports: [GmdRadioComponent],
})
class TestHostComponent {
  fieldKey = input<string | undefined>();
  name = input<string>('test-radio');
  label = input<string | undefined>();
  value = input<string | undefined>();
  required = input<boolean>(false);
  options = input<string>('');
  readonly = input<boolean>(false);
  disabled = input<boolean>(false);
}

describe('GmdRadioComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let radioComponent: GmdRadioComponent;
  let loader: HarnessLoader;
  let harness: GmdRadioComponentHarness;

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
    radioComponent = fixture.debugElement.query(p => p.name === 'gmd-radio')?.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GmdRadioComponentHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(radioComponent).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(radioComponent.value()).toBeUndefined();
    expect(radioComponent.required()).toBe(false);
    expect(radioComponent.readonly()).toBe(false);
    expect(radioComponent.disabled()).toBe(false);
  });

  it('should display label when provided', async () => {
    fixture.componentRef.setInput('label', 'Test Radio');
    fixture.detectChanges();

    const label = await harness.getLabel();
    expect(label).toContain('Test Radio');
  });

  it('should show required indicator when required is true', async () => {
    fixture.componentRef.setInput('label', 'Test Radio');
    fixture.componentRef.setInput('required', true);
    fixture.detectChanges();

    const hasIndicator = await harness.hasRequiredIndicator();
    expect(hasIndicator).toBe(true);
  });

  describe('Options parsing', () => {
    it('should parse comma-separated options', () => {
      fixture.componentRef.setInput('options', 'option1,option2,option3');
      fixture.detectChanges();

      const options = radioComponent.optionsVM();
      expect(options).toEqual(['option1', 'option2', 'option3']);
    });

    it('should parse JSON array options', () => {
      fixture.componentRef.setInput('options', '["option1","option2","option3"]');
      fixture.detectChanges();

      const options = radioComponent.optionsVM();
      expect(options).toEqual(['option1', 'option2', 'option3']);
    });

    it('should handle empty options', () => {
      fixture.componentRef.setInput('options', '');
      fixture.detectChanges();

      const options = radioComponent.optionsVM();
      expect(options).toEqual([]);
    });

    it('should trim whitespace from comma-separated options', () => {
      fixture.componentRef.setInput('options', 'option1 , option2 , option3');
      fixture.detectChanges();

      const options = radioComponent.optionsVM();
      expect(options).toEqual(['option1', 'option2', 'option3']);
    });

    it('should render options in template', async () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      const count = await harness.getOptionCount();
      expect(count).toBe(2);
    });
  });

  it('should update value when radio option is selected', async () => {
    fixture.componentRef.setInput('options', 'option1,option2');
    fixture.detectChanges();

    await harness.selectOption('option2');
    fixture.detectChanges();

    const selectedValue = await harness.getSelectedValue();
    expect(selectedValue).toBe('option2');
  });

  it('should be disabled when disabled input is true', async () => {
    fixture.componentRef.setInput('options', 'option1,option2');
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const isDisabled = await harness.isDisabled();
    expect(isDisabled).toBe(true);
  });

  it('should be readonly when readonly input is true', () => {
    fixture.componentRef.setInput('options', 'option1,option2');
    fixture.componentRef.setInput('readonly', true);
    fixture.detectChanges();

    const radioInputs = fixture.nativeElement.querySelectorAll('input[type="radio"]') as NodeListOf<HTMLInputElement>;
    radioInputs.forEach(input => {
      expect(input.readOnly).toBe(true);
    });
  });

  describe('Validation', () => {
    it('should be valid when not required and empty', () => {
      fixture.componentRef.setInput('required', false);
      fixture.detectChanges();

      expect(radioComponent.valid()).toBe(true);
      expect(radioComponent.validationErrors().length).toBe(0);
    });

    it('should be invalid when required and no option selected', () => {
      fixture.componentRef.setInput('required', true);
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      expect(radioComponent.valid()).toBe(false);
      expect(radioComponent.validationErrors()).toContain('required');
    });

    it('should be valid when required and option selected', () => {
      fixture.componentRef.setInput('required', true);
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.componentRef.setInput('value', 'option1');
      fixture.detectChanges();

      expect(radioComponent.valid()).toBe(true);
      expect(radioComponent.validationErrors().length).toBe(0);
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
  });

  describe('Store updates', () => {
    it('should update store when fieldKey is set and value changes', async () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.detectChanges();

      await harness.selectOption('option1');
      fixture.detectChanges();

      expect(mockStore.updateField).toHaveBeenLastCalledWith(
        expect.objectContaining({
          fieldKey: 'test-key',
          value: 'option1',
          valid: true,
        }),
      );
    });

    it('should update store with validation errors when invalid', async () => {
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

  describe('Initial value synchronization', () => {
    it('should sync and update value from input', async () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.componentRef.setInput('value', 'option1');
      fixture.detectChanges();

      expect(await harness.getSelectedValue()).toBe('option1');

      fixture.componentRef.setInput('value', 'option2');
      fixture.detectChanges();

      expect(await harness.getSelectedValue()).toBe('option2');
    });
  });

  describe('ARIA attributes', () => {
    it('should use fieldset/legend for semantic HTML', () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.componentRef.setInput('label', 'Choose option');
      fixture.detectChanges();

      const fieldset = fixture.nativeElement.querySelector('fieldset.gmd-radio');
      const legend = fixture.nativeElement.querySelector('legend.gmd-radio__group-label');
      expect(fieldset).toBeTruthy();
      expect(legend).toBeTruthy();
    });

    it('should set aria-labelledby on fieldset when label is provided', () => {
      fixture.componentRef.setInput('options', 'option1,option2');
      fixture.componentRef.setInput('label', 'Choose option');
      fixture.detectChanges();

      const fieldset = fixture.nativeElement.querySelector('fieldset.gmd-radio');
      expect(fieldset.getAttribute('aria-labelledby')).toBeTruthy();
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
