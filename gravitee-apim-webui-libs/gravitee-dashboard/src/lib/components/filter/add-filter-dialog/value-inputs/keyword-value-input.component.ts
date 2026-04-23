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
import { OverlayContainer } from '@angular/cdk/overlay';
import { AsyncPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocomplete, MatAutocompleteSelectedEvent, MatAutocompleteTrigger, MatOption } from '@angular/material/autocomplete';
import { MatChipGrid, MatChipInput, MatChipRemove, MatChipRow } from '@angular/material/chips';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { BehaviorSubject, combineLatest, fromEvent, Observable, of, Subscription } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { FILTER_VALUES_PROVIDER, FilterValueItem } from '../../filter-providers';
import { ID_BASED_FILTER_NAMES } from '../../filter.model';

interface KeywordOption {
  value: string;
  label: string;
}

@Component({
  selector: 'gd-keyword-value-input',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatFormField,
    MatLabel,
    MatInput,
    MatAutocomplete,
    MatAutocompleteTrigger,
    MatOption,
    MatChipGrid,
    MatChipRow,
    MatChipInput,
    MatChipRemove,
    MatIcon,
  ],
  styleUrl: './keyword-value-input.component.scss',
  template: `
    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="gd-value-input">
      <mat-label>Filter value</mat-label>
      <mat-chip-grid #chipGrid aria-label="Selected values">
        @for (chip of selectedChips(); track chip.value) {
          <mat-chip-row (removed)="removeValue(chip.value)">
            {{ chip.label }}
            <button matChipRemove [attr.aria-label]="'Remove ' + chip.label">
              <mat-icon>cancel</mat-icon>
            </button>
          </mat-chip-row>
        }
        <input
          #valueInput
          [placeholder]="chipInputPlaceholder()"
          matAutocompletePosition="below"
          [formControl]="searchControl"
          [matAutocomplete]="auto"
          [matChipInputFor]="chipGrid"
          [matChipInputAddOnBlur]="false"
        />
      </mat-chip-grid>
      <mat-autocomplete
        #auto="matAutocomplete"
        class="gd-value-input-autocomplete-panel"
        [hideSingleSelectionIndicator]="true"
        (opened)="onAutocompletePanelOpened()"
        (closed)="onAutocompletePanelClosed()"
        (optionSelected)="onAutocompleteOptionSelected($event)"
      >
        @if (panelState$ | async; as panel) {
          @if (panel.loading) {
            <mat-option disabled>Loading…</mat-option>
          } @else {
            @for (option of panel.options; track option.value) {
              <mat-option [value]="option.value" [disabled]="isOptionSelected(option)">
                <span class="gd-keyword-option">
                  <span class="gd-keyword-option__label">{{ option.label }}</span>
                  @if (isOptionSelected(option)) {
                    <mat-icon class="gd-keyword-option__check">check</mat-icon>
                  }
                </span>
              </mat-option>
            }
            @if (panel.options.length === 0) {
              <mat-option disabled>No matching values</mat-option>
            }
          }
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
})
export class KeywordValueInputComponent {
  /** Distance from bottom (px) that still counts as "near end" (small panels). */
  private static readonly scrollLoadThresholdPx = 100;
  /** Start loading the next page after this fraction of the scroll range (≈ 7/8). */
  private static readonly scrollPrefetchRatio = 7 / 8;

  private readonly valuesProvider = inject(FILTER_VALUES_PROVIDER);
  private readonly destroyRef = inject(DestroyRef);
  private readonly overlayContainer = inject(OverlayContainer);
  /** Material exposes `#panel` on the real scroll container; `querySelector` is unreliable because
   * `panelClass` is not a MatAutocomplete @Input in MDC — only `class` applies to the inner panel. */
  private readonly keywordAutocomplete = viewChild(MatAutocomplete);

  filterName = input.required<string>();
  timeFrom = input<number | undefined>(undefined);
  timeTo = input<number | undefined>(undefined);
  selectedValues = input<string[]>([]);
  valuesChange = output<string[]>();

  protected readonly searchControl = new FormControl<string>('', { nonNullable: true });
  protected readonly valueInput = viewChild<ElementRef<HTMLInputElement>>('valueInput');

  protected readonly chipInputPlaceholder = computed(() => (this.selectedChips().length > 0 ? '' : this.searchLabel()));

  private readonly loading$ = new BehaviorSubject<boolean>(false);
  private readonly hasNextPage$ = new BehaviorSubject<boolean>(false);

  private readonly page$ = new BehaviorSubject<number>(1);
  private readonly accumulatedOptions$ = new BehaviorSubject<KeywordOption[]>([]);
  private readonly labelByValue = signal<Map<string, string>>(new Map());
  private panelScrollSub?: Subscription;

  protected readonly panelState$ = combineLatest({
    loading: this.loading$,
    options: this.accumulatedOptions$.asObservable(),
    hasNextPage: this.hasNextPage$,
  });

  protected isIdBased = computed(() => ID_BASED_FILTER_NAMES.includes(this.filterName()));
  protected searchLabel = computed(() => (this.isIdBased() ? 'Search by name' : 'Search (exact match)'));

  protected selectedChips = computed(() => {
    const labels = this.labelByValue();
    return this.selectedValues().map(value => ({
      value,
      label: labels.get(value) ?? value,
    }));
  });

  constructor() {
    const searchTerm$ = this.searchControl.valueChanges.pipe(
      startWith(''),
      debounceTime(200),
      distinctUntilChanged(),
      tap(() => {
        this.accumulatedOptions$.next([]);
        if (this.page$.getValue() !== 1) {
          this.page$.next(1);
        }
      }),
    );

    searchTerm$
      .pipe(
        switchMap(term => this.page$.pipe(switchMap(page => this.fetchKeywordPage(term, page)))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.destroyRef.onDestroy(() => this.detachPanelScrollListener());
  }

  protected onAutocompletePanelOpened(): void {
    this.detachPanelScrollListener();
    queueMicrotask(() => {
      requestAnimationFrame(() => {
        const panelRoot = this.getAutocompletePanelElement();
        if (!panelRoot) {
          return;
        }
        const scrollSurface = this.getAutocompleteScrollSurface(panelRoot);
        this.panelScrollSub = fromEvent(scrollSurface, 'scroll', { passive: true })
          .pipe(
            debounceTime(80),
            filter(() => !this.loading$.getValue() && this.hasNextPage$.getValue()),
            filter(() => this.shouldPrefetchNextPage(scrollSurface)),
          )
          .subscribe(() => this.requestNextKeywordPage());
      });
    });
  }

  protected onAutocompletePanelClosed(): void {
    this.detachPanelScrollListener();
  }

  protected isOptionSelected(option: KeywordOption): boolean {
    return this.selectedValues().includes(option.value);
  }

  protected onAutocompleteOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value as string;
    if (value == null || value === '') {
      return;
    }
    const current = this.selectedValues();
    if (current.includes(value)) {
      return;
    }
    const label = this.accumulatedOptions$.getValue().find(o => o.value === value)?.label ?? this.labelByValue().get(value) ?? value;
    this.updateLabels([{ value, label }]);
    this.valuesChange.emit([...current, value]);
    this.clearSearchInputAfterPick();
  }

  protected removeValue(value: string): void {
    const next = this.selectedValues().filter(v => v !== value);
    this.valuesChange.emit(next);
  }

  private fetchKeywordPage(term: string, page: number): Observable<void> {
    this.loading$.next(true);
    return this.valuesProvider
      .getValues({
        filterName: this.filterName(),
        query: term || undefined,
        from: this.timeFrom(),
        to: this.timeTo(),
        page,
        perPage: 10,
      })
      .pipe(
        catchError(() => of({ data: [] as FilterValueItem[], hasNextPage: false })),
        map(result => ({
          options: result.data.map(
            (item): KeywordOption => ({
              value: item.id ?? item.value,
              label: item.label,
            }),
          ),
          hasNextPage: result.hasNextPage,
        })),
        tap(({ options, hasNextPage }) => {
          this.updateLabels(options);
          const current = this.accumulatedOptions$.getValue();
          const merged = page === 1 ? options : this.mergeUnique(current, options);
          this.accumulatedOptions$.next(merged);
          this.hasNextPage$.next(hasNextPage);
        }),
        map(() => undefined),
        finalize(() => {
          this.loading$.next(false);
          queueMicrotask(() => this.tryLoadNextPageIfPanelStillAtBottom());
        }),
      );
  }

  private clearSearchInputAfterPick(): void {
    if (this.searchControl.value) {
      this.searchControl.setValue('');
    } else {
      this.searchControl.setValue('', { emitEvent: false });
    }
    const input = this.valueInput()?.nativeElement;
    if (input) input.value = '';
  }

  private mergeUnique(current: KeywordOption[], incoming: KeywordOption[]): KeywordOption[] {
    const seen = new Set(current.map(o => o.value));
    const additions = incoming.filter(o => !seen.has(o.value));
    return [...current, ...additions];
  }

  private updateLabels(options: KeywordOption[]): void {
    const next = new Map(this.labelByValue());
    options.forEach(o => next.set(o.value, o.label));
    this.labelByValue.set(next);
  }

  private getAutocompletePanelElement(): HTMLElement | null {
    const overlayHost = this.overlayContainer.getContainerElement();
    const fromAutocomplete = this.keywordAutocomplete()?.panel?.nativeElement;
    if (fromAutocomplete instanceof HTMLElement && overlayHost.contains(fromAutocomplete)) {
      return fromAutocomplete;
    }
    return overlayHost.querySelector<HTMLElement>('.mat-mdc-autocomplete-panel.gd-value-input-autocomplete-panel');
  }

  private getAutocompleteScrollSurface(panelRoot: HTMLElement): HTMLElement {
    const overflowScrollable = (el: HTMLElement): boolean => {
      const y = getComputedStyle(el).overflowY;
      const allowsScroll = y === 'auto' || y === 'scroll' || y === 'overlay';
      return allowsScroll && el.scrollHeight > el.clientHeight + 1;
    };

    if (overflowScrollable(panelRoot)) {
      return panelRoot;
    }
    for (const el of Array.from(panelRoot.querySelectorAll<HTMLElement>('*'))) {
      if (overflowScrollable(el)) {
        return el;
      }
    }
    return panelRoot;
  }

  private shouldPrefetchNextPage(scrollEl: HTMLElement): boolean {
    const scrollTop = scrollEl.scrollTop;
    const visible = scrollEl.clientHeight;
    const total = scrollEl.scrollHeight;
    const maxScroll = total - visible;
    if (maxScroll <= 1) {
      return false;
    }
    const distanceFromBottom = total - scrollTop - visible;
    const scrolledRatio = scrollTop / maxScroll;
    return (
      scrolledRatio >= KeywordValueInputComponent.scrollPrefetchRatio ||
      distanceFromBottom <= KeywordValueInputComponent.scrollLoadThresholdPx
    );
  }

  private requestNextKeywordPage(): void {
    if (this.loading$.getValue() || !this.hasNextPage$.getValue()) {
      return;
    }
    this.page$.next(this.page$.getValue() + 1);
  }

  private tryLoadNextPageIfPanelStillAtBottom(): void {
    const panelRoot = this.getAutocompletePanelElement();
    if (!panelRoot) {
      return;
    }
    if (!this.hasNextPage$.getValue() || this.loading$.getValue()) {
      return;
    }
    const surface = this.getAutocompleteScrollSurface(panelRoot);
    if (!this.shouldPrefetchNextPage(surface)) {
      return;
    }
    this.requestNextKeywordPage();
  }

  private detachPanelScrollListener(): void {
    this.panelScrollSub?.unsubscribe();
    this.panelScrollSub = undefined;
  }
}
