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

import { FocusMonitor } from '@angular/cdk/a11y';
import { AsyncPipe } from '@angular/common';
import { Component, ElementRef, forwardRef, input, InputSignal, OnInit, ViewChild } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatIconButton } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { MatError } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTable, MatTableModule } from '@angular/material/table';
import { dropRight, isEmpty } from 'lodash';
import { Observable, of } from 'rxjs';
import { map, startWith, tap } from 'rxjs/operators';

import { FormHeaderFieldMapper, HEADER_NAMES } from './consumer-configuration-headers.model';
import { MobileClassDirective } from '../../../../directives/mobile-class.directive';
import { Header } from '../../../../entities/subscription';
import { AccordionModule } from '../../../accordion/accordion.module';

/**
 * Based on https://particles.gravitee.io/?path=/docs/components-form-headers--readme
 */
@Component({
  selector: 'app-consumer-configuration-headers',
  templateUrl: './consumer-configuration-headers.component.html',
  styleUrls: ['./consumer-configuration-headers.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ConsumerConfigurationHeadersComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ConsumerConfigurationHeadersComponent),
      multi: true,
    },
  ],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatIconModule,
    MatInputModule,
    MatAutocompleteModule,
    MatOptionModule,
    MatError,
    MatIconButton,
    AsyncPipe,
    MatTableModule,
    AccordionModule,
    MobileClassDirective,
  ],
})
export class ConsumerConfigurationHeadersComponent implements OnInit, ControlValueAccessor, Validator {
  @ViewChild(MatTable) table!: MatTable<Header>;

  public headerFieldMapper: InputSignal<FormHeaderFieldMapper> = input<FormHeaderFieldMapper>({
    keyName: 'name',
    valueName: 'value',
  });
  public autocompleteDisabled: InputSignal<boolean> = input<boolean>(false);
  public headersFormArray = new FormArray([
    new FormGroup({
      name: new FormControl('', Validators.pattern('^\\S*$')),
      value: new FormControl(''),
    }),
  ]);
  public mainForm = new FormGroup({
    headers: this.headersFormArray,
  });
  displayedColumns: string[] = ['name', 'value', 'actions'];

  private headers: Header[] = [];
  private filteredHeaderNames: Observable<string[]>[] = [];

  constructor(
    private readonly fm: FocusMonitor,
    private readonly elRef: ElementRef,
  ) {}

  public ngOnInit(): void {
    // When user start to complete last header add new empty one at the end
    this.headersFormArray?.valueChanges
      .pipe(
        map(headers =>
          headers.map(header => ({
            name: header.name ?? '',
            value: header.value ?? '',
          })),
        ),
        tap(headers => headers.length > 0 && this._onChange(removeLastEmptyHeader(headers))),
        tap((headers: Header[]) => {
          if (headers.length > 0 && (headers[headers.length - 1].name !== '' || headers[headers.length - 1].value !== '')) {
            this.addEmptyHeader();
            if (this.table) this.table.renderRows();
          }
        }),
      )
      .subscribe();

    this.fm.monitor(this.elRef.nativeElement, true).subscribe(() => {
      this._onTouched();
    });
  }

  public initHeadersForm(): void {
    // Clear all previous headers
    this.headersFormArray.clear();

    // Populate headers array from headers
    this.headers.forEach(({ name, value }, headerIndex) => {
      this.headersFormArray.push(
        new FormGroup({
          name: this.initKeyFormControl(name, headerIndex),
          value: new FormControl({ value, disabled: this.headersFormArray.disabled }),
        }),
        {
          emitEvent: false,
        },
      );
    });

    // add one empty header at the end
    this.addEmptyHeader();
    if (this.table) this.table.renderRows();
  }

  public writeValue(value: Record<string, string>[] | null): void {
    if (!value) {
      return;
    }

    this.headers = [...(value ?? [])].map((header: Record<string, string>) => ({
      name: header[this.headerFieldMapper().keyName],
      value: header[this.headerFieldMapper().valueName],
    }));

    this.initHeadersForm();
  }

  public registerOnChange(fn: (headers: unknown) => void): void {
    if (this.headerFieldMapper()) {
      this._onChange = (headers: Header[] | null) => fn(this.toHeaders(headers));
      return;
    }
    this._onChange = fn;
  }

  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled === this.headersFormArray.disabled) {
      // do nothing if the disabled state is not changed
      return;
    }

    if (isDisabled) {
      this.headersFormArray.disable({ emitEvent: false });
      this.removeLastHeader();
      return;
    }
    this.headersFormArray.enable({ emitEvent: false });
    this.addEmptyHeader();
  }

  public validate(_control: AbstractControl): ValidationErrors | null {
    if (this.headersFormArray.invalid) {
      return { invalid: true };
    }
    return null;
  }

  public getFilteredHeaderNames(headerIndex: number, headerName: string | null | undefined): Observable<string[]> {
    if (!this.filteredHeaderNames[headerIndex]) {
      return this.filteredHeaderNames[headerIndex];
    }
    if (headerName != null && headerName != '') {
      this.filteredHeaderNames[headerIndex] = of(headerName).pipe(map(value => this._filter(value)));
      return this.filteredHeaderNames[headerIndex];
    }
    return of(HEADER_NAMES);
  }

  public onDeleteHeader(headerIndex: number): void {
    this._onTouched();
    this.mainForm.controls.headers.removeAt(headerIndex);
    this.table.renderRows();
  }

  private _onChange: (_headers: Header[] | null) => void = () => ({});
  private _onTouched: () => void = () => ({});

  private initKeyFormControl(key: string, headerIndex: number) {
    const control = new FormControl({ value: key, disabled: this.headersFormArray.disabled }, Validators.pattern('^\\S*$'));
    const filteredKeys = control.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value ?? '')),
    );
    this.filteredHeaderNames.splice(headerIndex, 0, filteredKeys);
    return control;
  }

  private addEmptyHeader() {
    if (this.headersFormArray.disabled) {
      return;
    }

    this.headersFormArray.push(
      new FormGroup({
        name: this.initKeyFormControl('', this.headersFormArray.length),
        value: new FormControl(''),
      }),
      { emitEvent: false },
    );
  }

  private removeLastHeader() {
    this.headersFormArray.removeAt(this.headersFormArray.length - 1, { emitEvent: false });
  }

  private _filter(value: string): string[] {
    const filterValue = value.toLowerCase();
    return HEADER_NAMES.filter(option => option.toLowerCase().includes(filterValue));
  }

  private toHeaders(headers: Header[] | null) {
    return [...(headers ?? [])].map(header => {
      return {
        [this.headerFieldMapper().keyName]: header.name,
        [this.headerFieldMapper().valueName]: header.value,
      };
    });
  }
}

const removeLastEmptyHeader = (headers: Header[]) => {
  const lastHeader = headers[headers.length - 1];

  if (lastHeader && isEmpty(lastHeader.name) && isEmpty(lastHeader.value)) {
    return dropRight(headers);
  }
  return headers;
};
