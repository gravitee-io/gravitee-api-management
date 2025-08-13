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
import { Component, computed, ElementRef, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { debounceTime, distinctUntilChanged, startWith, tap } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { MatOptionModule } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

@Component({
  selector: 'gio-select-search-overlay',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    GioIconsModule,
    MatOptionModule,
    MatCardModule,
  ],
  templateUrl: './gio-select-search-overlay.component.html',
  styleUrls: ['./gio-select-search-overlay.component.scss'],
})
export class GioSelectSearchOverlayComponent {
  // Input from gio-select-search component
  options = signal<SelectOption[]>([]);
  selectedValues: string[] = [];
  placeholder = 'Search...';

  searchControl = new FormControl('');
  private readonly searchControlValue = toSignal(
    this.searchControl.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => this.scrollToTop()),
    ),
  );

  filteredOptions = computed(() => {
    const searchTerm = this.searchControlValue();
    if (!searchTerm) {
      return this.options();
    }
    const term = searchTerm.toLowerCase();
    return this.options().filter((option) => option.label.toLowerCase().includes(term));
  });

  @ViewChild('searchInput', { static: false }) searchInput!: ElementRef<HTMLInputElement>;
  @ViewChild('optionsContainer', { static: false }) optionsContainer!: ElementRef<HTMLDivElement>;

  // Output to gio-select-search component
  selectionChange = new Subject<string>();
  clearSelectionChange = new Subject<void>();
  close = new Subject<void>();

  toggleOption(option: SelectOption) {
    if (option.disabled) return;
    this.selectionChange.next(option.value);
  }

  clearSelection() {
    this.clearSelectionChange.next();
  }

  private scrollToTop() {
    if (this.optionsContainer) {
      this.optionsContainer.nativeElement.scrollTop = 0;
    }
  }
}
