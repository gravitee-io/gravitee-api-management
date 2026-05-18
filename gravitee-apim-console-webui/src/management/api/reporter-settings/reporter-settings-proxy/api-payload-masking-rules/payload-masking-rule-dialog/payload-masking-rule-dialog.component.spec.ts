/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { PayloadMaskingRuleDialogComponent, PayloadMaskingRuleDialogData } from './payload-masking-rule-dialog.component';

import { PayloadMaskingRule } from '../../../../../../entities/management-api-v2';

describe('PayloadMaskingRuleDialogComponent', () => {
  let fixture: ComponentFixture<PayloadMaskingRuleDialogComponent>;
  let component: PayloadMaskingRuleDialogComponent;
  const mockDialogRef = { close: jest.fn() };

  async function setup(data: PayloadMaskingRuleDialogData) {
    await TestBed.configureTestingModule({
      imports: [PayloadMaskingRuleDialogComponent, NoopAnimationsModule, MatIconTestingModule],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PayloadMaskingRuleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => jest.clearAllMocks());

  describe('New rule mode', () => {
    beforeEach(async () => setup({ rule: null }));

    it('should show "New payload masking rule" title', () => {
      const title: HTMLElement = fixture.nativeElement.querySelector('[mat-dialog-title]');
      expect(title.textContent?.trim()).toBe('New payload masking rule');
    });

    it('should show "Add" submit button label', () => {
      const btn: HTMLElement = fixture.nativeElement.querySelector('[mat-flat-button]');
      expect(btn.textContent?.trim()).toBe('Add');
    });

    it('should default to FULL masking type, AUTO format, BOTH phase', () => {
      const value = component['form'].getRawValue();
      expect(value.maskingType).toBe('FULL');
      expect(value.format).toBe('AUTO');
      expect(value.phase).toBe('BOTH');
    });

    it('should have invalid form when path is empty', () => {
      expect(component['form'].invalid).toBe(true);
    });

    it('should close with undefined when cancelled', () => {
      component.onCancel();
      expect(mockDialogRef.close).toHaveBeenCalledWith(undefined);
    });

    it('should not close on save when form is invalid', () => {
      component.onSave();
      expect(mockDialogRef.close).not.toHaveBeenCalled();
    });
  });

  describe('Edit rule mode', () => {
    const existingRule: PayloadMaskingRule & { _id: string } = {
      _id: 'test-id',
      path: '$.password',
      maskingStrategy: { type: 'FULL', replacement: '[HIDDEN]' },
      phase: 'REQUEST',
      format: 'JSON',
    };

    beforeEach(async () => setup({ rule: existingRule }));

    it('should show "Edit payload masking rule" title', () => {
      const title: HTMLElement = fixture.nativeElement.querySelector('[mat-dialog-title]');
      expect(title.textContent?.trim()).toBe('Edit payload masking rule');
    });

    it('should show "Save" submit button label', () => {
      const btn: HTMLElement = fixture.nativeElement.querySelector('[mat-flat-button]');
      expect(btn.textContent?.trim()).toBe('Save');
    });

    it('should pre-populate form values from existing rule', () => {
      const value = component['form'].getRawValue();
      expect(value.path).toBe('$.password');
      expect(value.maskingType).toBe('FULL');
      expect(value.fullReplacement).toBe('[HIDDEN]');
      expect(value.phase).toBe('REQUEST');
      expect(value.format).toBe('JSON');
    });
  });

  describe('FULL masking save', () => {
    beforeEach(async () => setup({ rule: null }));

    it('should close with correct PayloadMaskingRule for FULL masking with replacement', () => {
      component['form'].patchValue({ path: '$.password', maskingType: 'FULL', fullReplacement: '[MASKED]', format: 'JSON', phase: 'BOTH' });
      component.onSave();

      expect(mockDialogRef.close).toHaveBeenCalledWith<[PayloadMaskingRule]>({
        path: '$.password',
        format: 'JSON',
        phase: 'BOTH',
        maskingStrategy: { type: 'FULL', replacement: '[MASKED]' },
      });
    });

    it('should omit replacement when fullReplacement is blank (uses server default)', () => {
      component['form'].patchValue({ path: '$.password', maskingType: 'FULL', fullReplacement: '', format: 'AUTO', phase: 'BOTH' });
      component.onSave();

      const result = mockDialogRef.close.mock.calls[0][0] as PayloadMaskingRule;
      expect(result.maskingStrategy?.replacement).toBeUndefined();
    });
  });

  describe('PARTIAL masking save', () => {
    beforeEach(async () => setup({ rule: null }));

    it('should close with correct PayloadMaskingRule for PARTIAL masking', () => {
      component['form'].patchValue({
        path: '$.creditCard',
        maskingType: 'PARTIAL',
        prefixLength: 0,
        suffixLength: 4,
        maskChar: '*',
        format: 'JSON',
        phase: 'REQUEST',
      });
      component.onSave();

      expect(mockDialogRef.close).toHaveBeenCalledWith<[PayloadMaskingRule]>({
        path: '$.creditCard',
        format: 'JSON',
        phase: 'REQUEST',
        maskingStrategy: { type: 'PARTIAL', prefixLength: 0, suffixLength: 4, replacement: '*' },
      });
    });

    it('should default maskChar to * when left blank', () => {
      component['form'].patchValue({
        path: '$.creditCard',
        maskingType: 'PARTIAL',
        prefixLength: 0,
        suffixLength: 4,
        maskChar: '',
        format: 'AUTO',
        phase: 'BOTH',
      });
      component.onSave();

      const result = mockDialogRef.close.mock.calls[0][0] as PayloadMaskingRule;
      expect(result.maskingStrategy?.replacement).toBe('*');
    });

    it('should be invalid when prefixLength is negative', () => {
      component['form'].patchValue({ path: '$.creditCard', maskingType: 'PARTIAL', prefixLength: -1 });
      expect(component['form'].controls.prefixLength.hasError('min')).toBe(true);
    });
  });

  describe('isPartial signal', () => {
    beforeEach(async () => setup({ rule: null }));

    it('should be false when maskingType is FULL', () => {
      component['form'].patchValue({ maskingType: 'FULL' });
      fixture.detectChanges();
      expect(component['isPartial']()).toBe(false);
    });

    it('should be true when maskingType is PARTIAL', () => {
      component['form'].patchValue({ maskingType: 'PARTIAL' });
      fixture.detectChanges();
      expect(component['isPartial']()).toBe(true);
    });
  });

  describe('partialPreview', () => {
    beforeEach(async () => setup({ rule: null }));

    it('should show masked middle for 0 prefix 4 suffix', () => {
      component['form'].patchValue({ maskingType: 'PARTIAL', prefixLength: 0, suffixLength: 4, maskChar: '*' });
      fixture.detectChanges();
      expect(component['partialPreview']()).toBe('**********1234');
    });

    it('should show masked middle for 3 prefix 3 suffix', () => {
      component['form'].patchValue({ maskingType: 'PARTIAL', prefixLength: 3, suffixLength: 3, maskChar: 'X' });
      fixture.detectChanges();
      expect(component['partialPreview']()).toBe('ABCXXXXXXXX234');
    });

    it('should show original sample when prefix + suffix covers the full length', () => {
      component['form'].patchValue({ maskingType: 'PARTIAL', prefixLength: 7, suffixLength: 7, maskChar: '*' });
      fixture.detectChanges();
      expect(component['partialPreview']()).toBe('ABCDEFGHIJ1234');
    });
  });
});
