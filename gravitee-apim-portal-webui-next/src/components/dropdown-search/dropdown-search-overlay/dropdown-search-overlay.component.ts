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
import { CdkScrollable } from '@angular/cdk/scrolling';
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatIcon } from '@angular/material/icon';
import { MatFormField, MatInput } from '@angular/material/input';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, startWith, tap } from 'rxjs/operators';

import { SelectOption } from '../dropdown-search.types';

@Component({
  selector: 'app-dropdown-search-overlay',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormField, MatInput, MatIcon, MatButton, MatCheckbox, CdkScrollable],
  templateUrl: './dropdown-search-overlay.component.html',
  styleUrl: './dropdown-search-overlay.component.scss',
})
export class DropdownSearchOverlayComponent implements OnInit, AfterViewInit {
  @ViewChild('searchInput', { static: false }) searchInput!: ElementRef<HTMLInputElement>;
  @ViewChild(CdkScrollable) scrollable: CdkScrollable | undefined;

  options = signal<SelectOption[]>([]);
  selectedValues: string[] = [];
  placeholder = 'Search...';
  isLoading = false;
  hasNextPage = false;

  searchControl = new FormControl('');

  selectionChange = new Subject<string>();
  clearSelectionChange = new Subject<void>();
  searchChange = new Subject<string>();
  loadMoreChange = new Subject<void>();
  close = new Subject<void>();

  private readonly scrollThreshold = 100;
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.searchControl.valueChanges
      .pipe(
        startWith(''),
        distinctUntilChanged(),
        tap(value => {
          this.scrollToTop();
          this.searchChange.next(value || '');
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  ngAfterViewInit() {
    this.scrollable
      ?.elementScrolled()
      .pipe(debounceTime(150), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.scrollable || this.isLoading || !this.hasNextPage) {
          return;
        }

        const element = this.scrollable?.getElementRef().nativeElement;
        const scrollPosition = element.scrollTop + element.clientHeight;
        const scrollHeight = element.scrollHeight;

        if (scrollHeight - scrollPosition <= this.scrollThreshold) {
          this.loadMoreChange.next();
        }
      });
  }

  toggleOption(option: SelectOption) {
    if (option.disabled) return;
    this.selectionChange.next(option.value);
  }

  clearSelection() {
    this.clearSelectionChange.next();
  }

  private scrollToTop() {
    if (this.scrollable) {
      const element = this.scrollable.getElementRef().nativeElement;
      element.scrollTop = 0;
    }
  }
}
