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
import { AsyncPipe, CommonModule, NgIf } from '@angular/common';
import { Component, effect, ElementRef, HostListener, input, Input, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatOption } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatFormField, MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatSelect } from '@angular/material/select';

import { Category } from '../../entities/categories/categories';

@Component({
  selector: 'app-api-filter',
  standalone: true,
  imports: [
    MatButtonToggleGroup,
    MatButtonToggle,
    AsyncPipe,
    MatIcon,
    NgIf,
    MatFormField,
    ReactiveFormsModule,
    MatOption,
    MatSelect,
    MatFormFieldModule,
    CommonModule,
    MatMenuTrigger,
    MatButtonModule,
    MatMenuModule,
  ],
  templateUrl: './api-filter.component.html',
  styleUrl: './api-filter.component.scss',
})
export class ApiFilterComponent {
  @Input()
  filterList: Category[] = [];

  filterParam = input('');
  selectedFilter = output<string>();

  public selectedOption!: string | undefined;
  public currentValue: string = '';
  public isFilterOpen: boolean = false;
  public toggleControl = new FormControl(this.currentValue);

  constructor(private eRef: ElementRef) {
    effect(() => {
      const currentParamValue = this.filterList.slice(4).filter(filter => filter.id === this.filterParam());
      if (currentParamValue.length) {
        this.selectedOption = currentParamValue[0].name;
      } else {
        this.currentValue = this.filterParam();
        this.toggleControl.setValue(this.filterParam());
        this.selectedOption = '';
      }
    });
  }

  @HostListener('document:click', ['$event']) handleClickOutside(event: Event) {
    if (this.isFilterOpen && !this.eRef.nativeElement.contains(event.target)) {
      this.isFilterOpen = false;
    }
  }

  toggleDropdown(): void {
    this.isFilterOpen = !this.isFilterOpen;
  }

  onSelectedFilter(filterId: string | undefined): void {
    this.selectedFilter.emit(filterId ?? '');
  }

  selectOption(option: Category): void {
    this.onSelectedFilter(option.id);
    this.selectedOption = option.name;
    this.isFilterOpen = false;
  }
}
