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

import { RedactionRuleDialogComponent, RedactionRuleDialogData } from './redaction-rule-dialog.component';

import { RedactionRule } from '../../../../../../entities/management-api-v2';

describe('RedactionRuleDialogComponent', () => {
  let fixture: ComponentFixture<RedactionRuleDialogComponent>;
  let component: RedactionRuleDialogComponent;
  const mockDialogRef = { close: jest.fn() };

  async function setup(data: RedactionRuleDialogData) {
    await TestBed.configureTestingModule({
      imports: [RedactionRuleDialogComponent, NoopAnimationsModule, MatIconTestingModule],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RedactionRuleDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => jest.clearAllMocks());

  describe('New rule mode', () => {
    beforeEach(async () => setup({ rule: null, existingPatterns: [] }));

    it('should show "New redaction rule" title', () => {
      const title: HTMLElement = fixture.nativeElement.querySelector('[mat-dialog-title]');
      expect(title.textContent?.trim()).toBe('New redaction rule');
    });

    it('should show "Add" submit button label', () => {
      const btn: HTMLElement = fixture.nativeElement.querySelector('[mat-flat-button]');
      expect(btn.textContent?.trim()).toBe('Add');
    });

    it('should default to FULL masking type', () => {
      expect(component['form'].getRawValue().maskingType).toBe('FULL');
    });

    it('should have invalid form when pattern is empty', () => {
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
    const existingRule: RedactionRule & { _id: string } = {
      _id: 'test-id',
      attributeNamePattern: 'api-key',
      maskingStrategy: { type: 'FULL', replacement: '[HIDDEN]' },
    };

    beforeEach(async () => setup({ rule: existingRule, existingPatterns: [] }));

    it('should show "Edit redaction rule" title', () => {
      const title: HTMLElement = fixture.nativeElement.querySelector('[mat-dialog-title]');
      expect(title.textContent?.trim()).toBe('Edit redaction rule');
    });

    it('should show "Save" submit button label', () => {
      const btn: HTMLElement = fixture.nativeElement.querySelector('[mat-flat-button]');
      expect(btn.textContent?.trim()).toBe('Save');
    });

    it('should pre-populate form values from existing rule', () => {
      const value = component['form'].getRawValue();
      expect(value.attributeNamePattern).toBe('api-key');
      expect(value.maskingType).toBe('FULL');
      expect(value.fullReplacement).toBe('[HIDDEN]');
    });
  });

  describe('duplicate pattern validation', () => {
    it('should be invalid when pattern matches an existing pattern', async () => {
      await setup({ rule: null, existingPatterns: ['api-key', 'authorization'] });
      component['form'].patchValue({ attributeNamePattern: 'api-key' });
      expect(component['form'].controls.attributeNamePattern.hasError('duplicate')).toBe(true);
      expect(component['form'].invalid).toBe(true);
    });

    it('should be valid when pattern does not match any existing pattern', async () => {
      await setup({ rule: null, existingPatterns: ['api-key', 'authorization'] });
      component['form'].patchValue({ attributeNamePattern: 'user-id' });
      expect(component['form'].controls.attributeNamePattern.hasError('duplicate')).toBe(false);
    });

    it('should allow editing a rule without flagging its own pattern as duplicate', async () => {
      const rule: RedactionRule & { _id: string } = {
        _id: 'test-id',
        attributeNamePattern: 'api-key',
        maskingStrategy: { type: 'FULL' },
      };
      // Parent excludes the current rule's pattern from existingPatterns
      await setup({ rule, existingPatterns: ['authorization'] });
      expect(component['form'].controls.attributeNamePattern.hasError('duplicate')).toBe(false);
      expect(component['form'].valid).toBe(true);
    });
  });

  describe('FULL masking save', () => {
    beforeEach(async () => setup({ rule: null, existingPatterns: [] }));

    it('should close with correct RedactionRule for FULL masking with replacement', () => {
      component['form'].patchValue({ attributeNamePattern: 'user.email', maskingType: 'FULL', fullReplacement: '[MASKED]' });
      component.onSave();

      expect(mockDialogRef.close).toHaveBeenCalledWith<[RedactionRule]>({
        attributeNamePattern: 'user.email',
        maskingStrategy: { type: 'FULL', replacement: '[MASKED]' },
      });
    });

    it('should omit replacement when fullReplacement is blank (uses server default)', () => {
      component['form'].patchValue({ attributeNamePattern: 'user.email', maskingType: 'FULL', fullReplacement: '' });
      component.onSave();

      const result = mockDialogRef.close.mock.calls[0][0] as RedactionRule;
      expect(result.maskingStrategy?.replacement).toBeUndefined();
    });

    it('should omit valuePattern when left blank', () => {
      component['form'].patchValue({ attributeNamePattern: 'user.email', maskingType: 'FULL', valuePattern: '' });
      component.onSave();

      const result = mockDialogRef.close.mock.calls[0][0] as RedactionRule;
      expect(result.valuePattern).toBeUndefined();
    });

    it('should include valuePattern when provided', () => {
      component['form'].patchValue({ attributeNamePattern: 'user.email', maskingType: 'FULL', valuePattern: '^Bearer ' });
      component.onSave();

      const result = mockDialogRef.close.mock.calls[0][0] as RedactionRule;
      expect(result.valuePattern).toBe('^Bearer ');
    });
  });

  describe('PARTIAL masking save', () => {
    beforeEach(async () => setup({ rule: null, existingPatterns: [] }));

    it('should close with correct RedactionRule for PARTIAL masking', () => {
      component['form'].patchValue({
        attributeNamePattern: 'payment.card',
        maskingType: 'PARTIAL',
        prefixLength: 0,
        suffixLength: 4,
        maskChar: '*',
      });
      component.onSave();

      expect(mockDialogRef.close).toHaveBeenCalledWith<[RedactionRule]>({
        attributeNamePattern: 'payment.card',
        maskingStrategy: { type: 'PARTIAL', prefixLength: 0, suffixLength: 4, replacement: '*' },
      });
    });

    it('should default maskChar to * when left blank', () => {
      component['form'].patchValue({
        attributeNamePattern: 'payment.card',
        maskingType: 'PARTIAL',
        prefixLength: 0,
        suffixLength: 4,
        maskChar: '',
      });
      component.onSave();

      const result = mockDialogRef.close.mock.calls[0][0] as RedactionRule;
      expect(result.maskingStrategy?.replacement).toBe('*');
    });

    it('should be invalid when prefixLength is negative', () => {
      component['form'].patchValue({ attributeNamePattern: 'payment.card', maskingType: 'PARTIAL', prefixLength: -1 });
      expect(component['form'].controls.prefixLength.hasError('min')).toBe(true);
      expect(component['form'].invalid).toBe(true);
    });

    it('should be invalid when suffixLength is negative', () => {
      component['form'].patchValue({ attributeNamePattern: 'payment.card', maskingType: 'PARTIAL', suffixLength: -1 });
      expect(component['form'].controls.suffixLength.hasError('min')).toBe(true);
      expect(component['form'].invalid).toBe(true);
    });
  });

  describe('isPartial signal', () => {
    beforeEach(async () => setup({ rule: null, existingPatterns: [] }));

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
    beforeEach(async () => setup({ rule: null, existingPatterns: [] }));

    it('should show masked middle for 0 prefix 4 suffix', () => {
      component['form'].patchValue({ maskingType: 'PARTIAL', prefixLength: 0, suffixLength: 4, maskChar: '*' });
      fixture.detectChanges();
      // ABCDEFGHIJ1234 (14 chars), suffix 4 → last 4 = "1234", rest masked
      expect(component['partialPreview']()).toBe('**********1234');
    });

    it('should show masked middle for 3 prefix 3 suffix', () => {
      component['form'].patchValue({ maskingType: 'PARTIAL', prefixLength: 3, suffixLength: 3, maskChar: 'X' });
      fixture.detectChanges();
      // ABCDEFGHIJ1234 (14 chars) → ABC + 8 X's + 234
      expect(component['partialPreview']()).toBe('ABCXXXXXXXX234');
    });

    it('should show original sample when prefix + suffix covers the full length', () => {
      component['form'].patchValue({ maskingType: 'PARTIAL', prefixLength: 7, suffixLength: 7, maskChar: '*' });
      fixture.detectChanges();
      // 7 + 7 = 14 >= 14 → nothing to mask, original value is preserved
      expect(component['partialPreview']()).toBe('ABCDEFGHIJ1234');
    });

    it('should show original sample when prefix + suffix exceeds the sample length', () => {
      component['form'].patchValue({ maskingType: 'PARTIAL', prefixLength: 10, suffixLength: 10, maskChar: '*' });
      fixture.detectChanges();
      expect(component['partialPreview']()).toBe('ABCDEFGHIJ1234');
    });
  });
});
