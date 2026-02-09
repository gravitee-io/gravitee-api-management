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
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdFormValidationPanelComponent } from './gmd-form-validation-panel.component';
import { GMD_FORM_STATE_STORE, GmdFormStateStore } from '../services/gmd-form-state.store';

describe('GmdFormValidationPanelComponent', () => {
  let component: GmdFormValidationPanelComponent;
  let fixture: ComponentFixture<GmdFormValidationPanelComponent>;
  let store: GmdFormStateStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GmdFormValidationPanelComponent],
      providers: [
        {
          provide: GMD_FORM_STATE_STORE,
          useFactory: () => new GmdFormStateStore(),
        },
      ],
    }).compileComponents();

    store = TestBed.inject(GMD_FORM_STATE_STORE);
    fixture = TestBed.createComponent(GmdFormValidationPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should inject GMD_FORM_STATE_STORE', () => {
    expect(component['store']).toBeTruthy();
    expect(component['store']).toBe(store);
  });

  it('should display valid form status when form is valid', () => {
    fixture.detectChanges();

    const validBadge = fixture.nativeElement.querySelector('.status-panel__badge--valid');
    expect(validBadge).toBeTruthy();
    expect(validBadge.textContent).toContain('Valid');
  });

  it('should display invalid form status when form has invalid required fields', () => {
    store.updateField({
      id: 'test-1',
      fieldKey: 'email',
      value: '',
      valid: false,
      required: true,
      touched: true,
      validationErrors: ['required'],
    });
    fixture.detectChanges();

    const invalidBadge = fixture.nativeElement.querySelector('.status-panel__badge--invalid');
    expect(invalidBadge).toBeTruthy();
    expect(invalidBadge.textContent).toContain('Invalid');
  });

  it('should display configuration errors section when critical errors exist', () => {
    store.updateField({
      id: 'test-1',
      fieldKey: 'email',
      value: '',
      valid: true,
      required: false,
      touched: false,
      validationErrors: [],
      configErrors: [
        {
          code: 'invalidRegex',
          message: 'Invalid regex pattern',
          severity: 'error',
          field: 'pattern',
        },
      ],
    });
    fixture.detectChanges();

    const errorSection = fixture.nativeElement.querySelector('.status-panel__section--error');
    expect(errorSection).toBeTruthy();
    expect(errorSection.textContent).toContain('Configuration Errors');
    expect(errorSection.textContent).toContain('Invalid regex pattern');
  });

  it('should display configuration warnings section when warnings exist', () => {
    store.updateField({
      id: 'test-1',
      fieldKey: 'email',
      value: '',
      valid: true,
      required: false,
      touched: false,
      validationErrors: [],
      configErrors: [
        {
          code: 'normalizedValue',
          message: 'Value was normalized',
          severity: 'warning',
          field: 'minLength',
          normalizedTo: '5',
        },
      ],
    });
    fixture.detectChanges();

    const warningSection = fixture.nativeElement.querySelector('.status-panel__section--warning');
    expect(warningSection).toBeTruthy();
    expect(warningSection.textContent).toContain('Warnings (Auto-Corrected)');
    expect(warningSection.textContent).toContain('Value was normalized');
  });

  it('should display invalid fields list when fields are invalid', () => {
    store.updateField({
      id: 'test-1',
      fieldKey: 'email',
      value: '',
      valid: false,
      required: true,
      touched: true,
      validationErrors: ['required', 'pattern'],
    });
    fixture.detectChanges();

    const subtitle = fixture.nativeElement.querySelector('.status-panel__subtitle');
    expect(subtitle).toBeTruthy();
    expect(subtitle.textContent).toContain('Invalid fields:');

    const fieldKey = fixture.nativeElement.querySelector('.status-panel__key');
    expect(fieldKey.textContent).toContain('email');

    const fieldErrors = fixture.nativeElement.querySelector('.status-panel__muted');
    expect(fieldErrors.textContent).toContain('required, pattern');
  });

  it('should display required fields count', () => {
    store.updateField({
      id: 'test-1',
      fieldKey: 'email',
      value: 'test@test.com',
      valid: true,
      required: true,
      touched: true,
      validationErrors: [],
    });

    store.updateField({
      id: 'test-2',
      fieldKey: 'name',
      value: '',
      valid: false,
      required: true,
      touched: false,
      validationErrors: ['required'],
    });

    fixture.detectChanges();

    const meta = fixture.nativeElement.querySelector('.status-panel__meta');
    expect(meta).toBeTruthy();
    expect(meta.textContent).toContain('(1/2 required fields filled)');
  });

  it('should display field values in details section', () => {
    store.updateField({
      id: 'test-1',
      fieldKey: 'email',
      value: 'test@example.com',
      valid: true,
      required: false,
      touched: true,
      validationErrors: [],
    });
    fixture.detectChanges();

    const details = fixture.nativeElement.querySelector('.status-panel__details');
    expect(details).toBeTruthy();

    const summary = details.querySelector('.status-panel__summary');
    expect(summary.textContent).toContain('Field values (1)');
  });

  it('should format empty field values as "(empty)"', () => {
    expect(component.formatValue('')).toBe('(empty)');
    expect(component.formatValue('   ')).toBe('(empty)');
  });

  it('should not format non-empty field values', () => {
    expect(component.formatValue('test')).toBe('test');
    expect(component.formatValue('  test  ')).toBe('  test  ');
  });

  it('should hide fieldKey prefix for gmd- prefixed keys in config errors', () => {
    store.updateField({
      id: 'gmd-test-1',
      fieldKey: 'gmd-internal-field',
      value: '',
      valid: true,
      required: false,
      touched: false,
      validationErrors: [],
      configErrors: [
        {
          code: 'emptyFieldKey',
          message: 'Field key is empty',
          severity: 'error',
          field: 'fieldKey',
        },
      ],
    });
    fixture.detectChanges();

    const errorItem = fixture.nativeElement.querySelector('.status-panel__item');
    expect(errorItem.textContent).not.toContain('gmd-internal-field:');
    expect(errorItem.textContent).toContain('Field key is empty');
  });

  it('should not display validation panel when no errors or warnings exist', () => {
    store.updateField({
      id: 'test-1',
      fieldKey: 'email',
      value: 'test@test.com',
      valid: true,
      required: false,
      touched: false,
      validationErrors: [],
    });
    fixture.detectChanges();

    const validationPanel = fixture.nativeElement.querySelector('.status-panel--validation');
    expect(validationPanel).toBeFalsy();
  });
});
