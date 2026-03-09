/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { FormKeyValuePairsComponent } from './form-key-value-pairs.component';

describe('FormKeyValuePairsComponent', () => {
  let component: FormKeyValuePairsComponent;
  let fixture: ComponentFixture<FormKeyValuePairsComponent>;

  beforeEach(waitForAsync(async () => {
    await TestBed.configureTestingModule({
      imports: [FormKeyValuePairsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FormKeyValuePairsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  }));

  it('creates', () => {
    expect(component).toBeTruthy();
  });

  describe('initial state', () => {
    it('starts with a single empty pair', () => {
      expect(component.metadataFormArray.value).toEqual([{ key: '', value: '' }]);
    });
  });

  describe('writeValue (CVA)', () => {
    it('populates pairs from Record and keeps trailing empty row', () => {
      component.writeValue({ 'app-name': 'MyApp', version: '1.0' });

      expect(component.metadataFormArray.value).toEqual([
        { key: 'app-name', value: 'MyApp' },
        { key: 'version', value: '1.0' },
        { key: '', value: '' },
      ]);
    });

    it('trims keys and values when writing', () => {
      component.writeValue({ '  key  ': '  value  ' });

      expect(component.metadataFormArray.value).toEqual([
        { key: 'key', value: 'value' },
        { key: '', value: '' },
      ]);
    });

    it('clears pairs when null is written, leaving only trailing empty row', () => {
      component.writeValue({ k1: 'v1' });
      expect(component.metadataFormArray.length).toBeGreaterThan(0);

      component.writeValue(null);

      expect(component.metadataFormArray.value).toEqual([{ key: '', value: '' }]);
    });
  });

  describe('change propagation (registerOnChange)', () => {
    it('calls onChange with Record when user edits a row', waitForAsync(async () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.metadataFormArray.at(0).patchValue({ key: 'test-key', value: 'test-value' });

      await fixture.whenStable();

      expect(onChangeSpy).toHaveBeenLastCalledWith({ 'test-key': 'test-value' });
    }));

    it('filters out empty pairs from onChange payload', waitForAsync(async () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.metadataFormArray.at(0).patchValue({ key: 'valid-key', value: 'valid-value' });
      await fixture.whenStable();

      // trailing row should exist already; keep it empty
      const trailingIndex = component.metadataFormArray.length - 1;
      component.metadataFormArray.at(trailingIndex).patchValue({ key: '', value: '' });
      await fixture.whenStable();

      expect(onChangeSpy).toHaveBeenLastCalledWith({ 'valid-key': 'valid-value' });
    }));

    it('overwrites duplicate keys with the last value', waitForAsync(async () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.metadataFormArray.at(0).patchValue({ key: 'dup', value: 'v1' });
      await fixture.whenStable();

      const trailingIndex = component.metadataFormArray.length - 1;
      component.metadataFormArray.at(trailingIndex).patchValue({ key: 'dup', value: 'v2' });
      await fixture.whenStable();

      expect(onChangeSpy).toHaveBeenLastCalledWith({ dup: 'v2' });
    }));

    it('adds a new trailing empty row when the last row becomes non-empty', waitForAsync(async () => {
      component.metadataFormArray.at(0).patchValue({ key: 'k1', value: 'v1' });

      await fixture.whenStable();

      expect(component.metadataFormArray.value).toEqual([
        { key: 'k1', value: 'v1' },
        { key: '', value: '' },
      ]);
    }));

    it('does not add extra trailing row if last row is still empty', waitForAsync(async () => {
      component.metadataFormArray.at(0).patchValue({ key: '', value: '' });

      await fixture.whenStable();

      expect(component.metadataFormArray.length).toBe(1);
    }));
  });

  describe('touched propagation (registerOnTouched)', () => {
    it('calls onTouched on explicit blur hook', () => {
      const onTouchedSpy = jest.fn();
      component.registerOnTouched(onTouchedSpy);

      component.onTouched();

      expect(onTouchedSpy).toHaveBeenCalled();
    });

    it('calls onTouched when deleting a row', () => {
      component.writeValue({ key1: 'val1', key2: 'val2' });

      const onTouchedSpy = jest.fn();
      component.registerOnTouched(onTouchedSpy);

      component.onDeletePair(0);

      expect(onTouchedSpy).toHaveBeenCalled();
    });
  });

  describe('disabled state (setDisabledState)', () => {
    it('disables the form array', () => {
      component.setDisabledState(true);
      expect(component.metadataFormArray.disabled).toBe(true);
    });

    it('enables the form array back', () => {
      component.setDisabledState(true);
      component.setDisabledState(false);
      expect(component.metadataFormArray.enabled).toBe(true);
    });

    it('removes trailing empty row when disabled', waitForAsync(async () => {
      component.metadataFormArray.at(0).patchValue({ key: 'k1', value: 'v1' });
      await fixture.whenStable();

      expect(component.metadataFormArray.length).toBe(2);

      component.setDisabledState(true);
      await fixture.whenStable();

      expect(component.metadataFormArray.value).toEqual([{ key: 'k1', value: 'v1' }]);
    }));

    it('restores trailing empty row when enabled again', waitForAsync(async () => {
      component.metadataFormArray.at(0).patchValue({ key: 'k1', value: 'v1' });
      await fixture.whenStable();

      component.setDisabledState(true);
      await fixture.whenStable();

      component.setDisabledState(false);
      await fixture.whenStable();

      expect(component.metadataFormArray.value).toEqual([
        { key: 'k1', value: 'v1' },
        { key: '', value: '' },
      ]);
    }));
  });

  describe('deleting pairs', () => {
    it('removes the pair at given index', () => {
      component.writeValue({ key1: 'val1', key2: 'val2' });
      const initialLength = component.metadataFormArray.length;

      component.onDeletePair(0);

      expect(component.metadataFormArray.length).toBe(initialLength - 1);
      expect(component.metadataFormArray.at(0).value).toEqual({ key: 'key2', value: 'val2' });
    });
  });
});
