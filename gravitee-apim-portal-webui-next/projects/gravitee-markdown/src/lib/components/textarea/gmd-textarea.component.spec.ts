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
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdTextareaComponent } from './gmd-textarea.component';
import { GmdFieldState } from '../../models/formField';

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
      [disabled]="disabled()" />
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
  minLength = input<number | string | null>(null);
  maxLength = input<number | string | null>(null);
  rows = input<number>(4);
  readonly = input<boolean>(false);
  disabled = input<boolean>(false);
}

describe('GmdTextareaComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let textareaComponent: GmdTextareaComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    textareaComponent = fixture.debugElement.query(p => p.name === 'gmd-textarea')?.componentInstance;
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

  it('should display label when provided', () => {
    fixture.componentRef.setInput('label', 'Test Textarea');
    fixture.detectChanges();

    const labelElement = fixture.nativeElement.querySelector('.gmd-textarea__label');
    expect(labelElement).toBeTruthy();
    expect(labelElement.textContent.trim()).toContain('Test Textarea');
  });

  it('should show placeholder when provided', () => {
    fixture.componentRef.setInput('placeholder', 'Enter text here');
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    expect(textarea.placeholder).toBe('Enter text here');
  });

  it('should show required indicator when required is true', () => {
    fixture.componentRef.setInput('label', 'Test Textarea');
    fixture.componentRef.setInput('required', true);
    fixture.detectChanges();

    const requiredIndicator = fixture.nativeElement.querySelector('.gmd-textarea__required');
    expect(requiredIndicator).toBeTruthy();
    expect(requiredIndicator.textContent.trim()).toBe('*');
  });

  it('should update value when textarea input changes', () => {
    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    textarea.value = 'test value';
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(textarea.value).toBe('test value');
  });

  it('should set rows attribute', () => {
    fixture.componentRef.setInput('rows', 10);
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    expect(textarea.rows).toBe(10);
  });

  it('should be readonly when readonly input is true', () => {
    fixture.componentRef.setInput('readonly', true);
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    expect(textarea.readOnly).toBe(true);
  });

  it('should be disabled when disabled input is true', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    expect(textarea.disabled).toBe(true);
  });

  describe('Validation', () => {
    it('should be valid when not required and empty', () => {
      fixture.componentRef.setInput('required', false);
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(true);
      expect(textareaComponent.errors().length).toBe(0);
    });

    it('should be invalid when required and empty', () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(false);
      expect(textareaComponent.errors()).toContain('required');
    });

    it('should be valid when required and has value', () => {
      fixture.componentRef.setInput('required', true);
      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.value = 'test';
      textarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(true);
      expect(textareaComponent.errors().length).toBe(0);
    });

    it('should validate minLength', () => {
      fixture.componentRef.setInput('minLength', 5);
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.value = 'test';
      textarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(false);
      expect(textareaComponent.errors()).toContain('minLength');
    });

    it('should validate maxLength', () => {
      fixture.componentRef.setInput('maxLength', 5);
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.value = 'too long value';
      textarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(textareaComponent.valid()).toBe(false);
      expect(textareaComponent.errors()).toContain('maxLength');
    });

    it('should show error messages when touched and invalid', () => {
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.dispatchEvent(new Event('blur'));
      fixture.detectChanges();

      const errorElement = fixture.nativeElement.querySelector('.gmd-textarea__error');
      expect(errorElement).toBeTruthy();
      expect(errorElement.textContent.trim()).toBe('This field is required.');
    });
  });

  describe('Event emission', () => {
    it('should emit gmdFieldStateChange event when fieldKey is set and value changes', async () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.detectChanges();
      await fixture.whenStable();

      const eventPromise = new Promise<CustomEvent<GmdFieldState>>(resolve => {
        const textareaElement = fixture.nativeElement.querySelector('gmd-textarea');
        textareaElement.addEventListener(
          'gmdFieldStateChange',
          (event: Event) => {
            resolve(event as CustomEvent<GmdFieldState>);
          },
          { once: true },
        );
      });

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.value = 'test value';
      textarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      // Wait for effect() to trigger and setTimeout in emitState to complete
      await new Promise(resolve => setTimeout(resolve, 50));

      const customEvent = await eventPromise;
      expect(customEvent.detail.key).toBe('test-key');
      expect(customEvent.detail.value).toBe('test value');
      expect(customEvent.detail.valid).toBe(true);
    });

    it('should not emit event when fieldKey is not set', async () => {
      fixture.componentRef.setInput('fieldKey', undefined);
      fixture.detectChanges();

      let eventEmitted = false;
      const textareaElement = fixture.nativeElement.querySelector('gmd-textarea');
      textareaElement.addEventListener('gmdFieldStateChange', () => {
        eventEmitted = true;
      });

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.value = 'test';
      textarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      // Wait to ensure no event is emitted
      await new Promise(resolve => setTimeout(resolve, 50));
      expect(eventEmitted).toBe(false);
    });

    it('should emit event with validation errors when invalid', async () => {
      fixture.componentRef.setInput('fieldKey', 'test-key');
      fixture.componentRef.setInput('required', true);
      fixture.detectChanges();

      const eventPromise = new Promise<CustomEvent<GmdFieldState>>(resolve => {
        const textareaElement = fixture.nativeElement.querySelector('gmd-textarea');
        textareaElement.addEventListener('gmdFieldStateChange', (event: Event) => {
          resolve(event as CustomEvent<GmdFieldState>);
        });
      });

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      textarea.dispatchEvent(new Event('blur'));
      fixture.detectChanges();

      await new Promise(resolve => setTimeout(resolve, 10));

      const customEvent = await eventPromise;
      expect(customEvent.detail.key).toBe('test-key');
      expect(customEvent.detail.valid).toBe(false);
      expect(customEvent.detail.errors).toContain('required');
    });
  });

  describe('Initial value synchronization', () => {
    it('should sync and update value from input', () => {
      fixture.componentRef.setInput('value', 'first value');
      fixture.detectChanges();

      let textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      expect(textarea.value).toBe('first value');

      fixture.componentRef.setInput('value', 'second value');
      fixture.detectChanges();

      textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      expect(textarea.value).toBe('second value');
    });

    it('should handle undefined value', () => {
      fixture.componentRef.setInput('value', undefined);
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      expect(textarea.value).toBe('');
    });
  });

  describe('Number-like validation inputs', () => {
    it.each([
      { input: 5, expected: '5', type: 'numeric' },
      { input: '10', expected: '10', type: 'string' },
    ])('should accept $type minLength', ({ input, expected }) => {
      fixture.componentRef.setInput('minLength', input);
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      expect(textarea.getAttribute('minlength')).toBe(expected);
    });

    it.each([
      { input: 20, expected: '20', type: 'numeric' },
      { input: '30', expected: '30', type: 'string' },
    ])('should accept $type maxLength', ({ input, expected }) => {
      fixture.componentRef.setInput('maxLength', input);
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
      expect(textarea.getAttribute('maxlength')).toBe(expected);
    });
  });
});
