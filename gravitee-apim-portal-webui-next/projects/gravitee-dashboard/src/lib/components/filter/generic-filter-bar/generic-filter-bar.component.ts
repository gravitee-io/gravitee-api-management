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
import { AsyncPipe } from '@angular/common';
import { Component, effect, input, OnInit, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { distinctUntilChanged, Observable } from 'rxjs';

import { FilterName } from '../../widget/model/request/enum/filter-name';
import { SelectOption } from '../dropdown-search/dropdown-search-overlay/dropdown-search-overlay.component';
import { DropdownSearchComponent, ResultsLoaderInput, ResultsLoaderOutput } from '../dropdown-search/dropdown-search.component';

export interface SelectedFilter {
  parentKey: string;
  value: string;
}

export interface Filter {
  key: FilterName;
  label: string;
  data?: SelectOption[];
  data$?: Observable<SelectOption[]>;
  dataLoader?: (input: ResultsLoaderInput) => Observable<ResultsLoaderOutput>;
}

@Component({
  selector: 'gd-generic-filter-bar',
  imports: [DropdownSearchComponent, AsyncPipe, ReactiveFormsModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './generic-filter-bar.component.html',
  styleUrl: './generic-filter-bar.component.scss',
})
export class GenericFilterBarComponent implements OnInit {
  filters = input.required<Filter[]>();
  currentSelectedFilters = input.required<SelectedFilter[]>();

  selectedFilters = output<SelectedFilter[]>();

  form = new FormGroup<Record<string, FormControl<string[]>>>({});

  constructor() {
    effect(() => {
      this.filters().forEach(filter => {
        if (!this.form.contains(filter.key)) {
          this.form.addControl(filter.key, new FormControl<string[]>([], { nonNullable: true }));
        }
      });
    });

    effect(() => {
      // For each current selected filter, set the value of the corresponding control
      this.currentSelectedFilters().forEach(selectedFilter => {
        const control = this.form.get(selectedFilter.parentKey);
        if (control) {
          const currentValues = control.value;
          if (!currentValues.includes(selectedFilter.value)) {
            control.setValue([...currentValues, selectedFilter.value]);
          }
        }
      });
    });
  }

  ngOnInit(): void {
    this.form.valueChanges.pipe(distinctUntilChanged()).subscribe(value => {
      const selected: SelectedFilter[] = [];
      for (const key in value) {
        const selectedOptions = value[key];
        if (selectedOptions) {
          selectedOptions.forEach(option => {
            selected.push({ parentKey: key, value: option });
          });
        }
      }
      this.selectedFilters.emit(selected);
    });
  }
}
