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

import { Component, DestroyRef, forwardRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, FormArray, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { map, tap } from 'rxjs/operators';

import { MobileClassDirective } from '../../directives/mobile-class.directive';

export interface KeyValuePair {
  key: string;
  value: string;
}

@Component({
  selector: 'app-form-key-value-pairs',
  templateUrl: './form-key-value-pairs.component.html',
  styleUrls: ['./form-key-value-pairs.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FormKeyValuePairsComponent),
      multi: true,
    },
  ],
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatIconModule, MatInputModule, MatButtonModule, MobileClassDirective],
})
export class FormKeyValuePairsComponent implements OnInit, ControlValueAccessor {
  public metadataFormArray = new FormArray([
    new FormGroup({
      key: new FormControl(''),
      value: new FormControl(''),
    }),
  ]);

  private readonly destroyRef = inject(DestroyRef);

  public ngOnInit(): void {
    this.metadataFormArray.valueChanges
      .pipe(
        map(pairs =>
          pairs.map(pair => ({
            key: (pair.key ?? '').trim(),
            value: (pair.value ?? '').trim(),
          })),
        ),
        tap(pairs => {
          const validPairs = pairs.filter(p => p.key || p.value);
          this._onChange(this.toRecord(validPairs));
          this.ensureTrailingEmptyRow();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  public writeValue(value: Record<string, string> | null): void {
    this.metadataFormArray.clear({ emitEvent: false });

    if (value) {
      Object.entries(value).forEach(([key, val]) => {
        this.metadataFormArray.push(
          new FormGroup({
            key: new FormControl(key.trim()),
            value: new FormControl(val.trim()),
          }),
          { emitEvent: false },
        );
      });
    }

    this.ensureTrailingEmptyRow();
  }

  public registerOnChange(fn: (value: Record<string, string> | null) => void): void {
    this._onChange = fn;
  }

  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  public setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.metadataFormArray.disable({ emitEvent: false }) : this.metadataFormArray.enable({ emitEvent: false });
    this.ensureTrailingEmptyRow();
  }

  public onDeletePair(pairIndex: number): void {
    this._onTouched();
    this.metadataFormArray.removeAt(pairIndex);
  }

  public onTouched(): void {
    this._onTouched();
  }

  isDeleteVisible(i: number): boolean {
    return !this.metadataFormArray.disabled && i < this.metadataFormArray.length - 1;
  }

  private _onChange: (_value: Record<string, string> | null) => void = () => ({});
  private _onTouched: () => void = () => ({});

  private ensureTrailingEmptyRow(): void {
    if (this.metadataFormArray.disabled) {
      const lastIndex = this.metadataFormArray.length - 1;
      const lastControl = lastIndex >= 0 ? this.metadataFormArray.at(lastIndex) : null;
      const isEmpty = lastControl && !lastControl.get('key')?.value && !lastControl.get('value')?.value;
      if (isEmpty && lastIndex >= 0) {
        this.metadataFormArray.removeAt(lastIndex, { emitEvent: false });
      }
      return;
    }

    if (this.metadataFormArray.length === 0) {
      this.metadataFormArray.push(this.createEmptyGroup(), { emitEvent: false });
      return;
    }

    const lastControl = this.metadataFormArray.at(-1) as FormGroup;
    const lastIsEmpty = !lastControl.get('key')?.value && !lastControl.get('value')?.value;

    if (!lastIsEmpty) {
      this.metadataFormArray.push(this.createEmptyGroup(), { emitEvent: false });
    }
  }

  private createEmptyGroup(): FormGroup {
    return new FormGroup({
      key: new FormControl(''),
      value: new FormControl(''),
    });
  }

  private toRecord(pairs: KeyValuePair[] | null): Record<string, string> | null {
    if (!pairs || pairs.length === 0) {
      return null;
    }

    const record: Record<string, string> = {};
    pairs.forEach(pair => {
      if (pair.key) {
        record[pair.key] = pair.value || '';
      }
    });

    return Object.keys(record).length > 0 ? record : null;
  }
}
