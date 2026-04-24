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
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { EnumValueInputComponent } from './enum-value-input.component';
import { FilterDefinition } from '../../filter.model';

describe('EnumValueInputComponent', () => {
  const inCapableEnum: FilterDefinition = {
    name: 'HTTP_STATUS_GROUP',
    label: 'Status Code Group',
    type: 'ENUM',
    operators: ['EQ', 'IN'],
    values: ['2XX', '3XX', '4XX', '5XX'],
  };

  const eqOnlyEnum: FilterDefinition = {
    name: 'SIMPLE',
    label: 'Simple',
    type: 'ENUM',
    operators: ['EQ'],
    values: ['A', 'B'],
  };

  const notInCapable: FilterDefinition = {
    name: 'X',
    label: 'X',
    type: 'ENUM',
    operators: ['EQ', 'NEQ', 'NOT_IN'],
    values: ['1', '2'],
  };

  function isMulti(c: EnumValueInputComponent): boolean {
    return (c as unknown as { isMultiSelect: () => boolean }).isMultiSelect();
  }

  it('should use mat-select multiple after IN was normalized to EQ (definition allows IN)', async () => {
    await TestBed.configureTestingModule({
      imports: [EnumValueInputComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    const fixture = TestBed.createComponent(EnumValueInputComponent);
    const cmp = fixture.componentInstance;
    fixture.componentRef.setInput('definition', inCapableEnum);
    fixture.componentRef.setInput('selectedOperator', 'EQ');
    fixture.componentRef.setInput('selectedValues', ['2XX']);
    fixture.detectChanges();

    expect(isMulti(cmp)).toBe(true);
  });

  it('should not use multiple mode when definition has no IN/NOT_IN', async () => {
    await TestBed.configureTestingModule({
      imports: [EnumValueInputComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    const fixture = TestBed.createComponent(EnumValueInputComponent);
    const cmp = fixture.componentInstance;
    fixture.componentRef.setInput('definition', eqOnlyEnum);
    fixture.componentRef.setInput('selectedOperator', 'EQ');
    fixture.componentRef.setInput('selectedValues', ['A']);
    fixture.detectChanges();

    expect(isMulti(cmp)).toBe(false);
  });

  it('should use multiple for NOT_IN', async () => {
    await TestBed.configureTestingModule({
      imports: [EnumValueInputComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    const fixture = TestBed.createComponent(EnumValueInputComponent);
    const cmp = fixture.componentInstance;
    fixture.componentRef.setInput('definition', notInCapable);
    fixture.componentRef.setInput('selectedOperator', 'NOT_IN');
    fixture.componentRef.setInput('selectedValues', ['1']);
    fixture.detectChanges();

    expect(isMulti(cmp)).toBe(true);
  });

  it('should use multiple when NOT_IN-capable and operator is NEQ after single-value normalization', async () => {
    await TestBed.configureTestingModule({
      imports: [EnumValueInputComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    const fixture = TestBed.createComponent(EnumValueInputComponent);
    const cmp = fixture.componentInstance;
    fixture.componentRef.setInput('definition', notInCapable);
    fixture.componentRef.setInput('selectedOperator', 'NEQ');
    fixture.componentRef.setInput('selectedValues', ['1']);
    fixture.detectChanges();

    expect(isMulti(cmp)).toBe(true);
  });
});
