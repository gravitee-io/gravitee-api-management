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
import { Overlay, OverlayContainer, OverlayModule, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { AsyncPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  output,
  signal,
  TemplateRef,
  viewChild,
  ViewContainerRef,
  ViewEncapsulation,
} from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocomplete, MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatChipGrid, MatChipInput, MatChipRemove, MatChipRow } from '@angular/material/chips';
import { MatOption, MatPseudoCheckbox, MatPseudoCheckboxState } from '@angular/material/core';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { BehaviorSubject, combineLatest, fromEvent, Observable, of, Subscription } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { isMultiSelectForFilter } from './filter-value-multi-select.util';
import { FILTER_VALUES_PROVIDER, FilterValueItem } from '../../filter-providers';
import { FilterDefinition, FilterValueSelection, ID_BASED_FILTER_NAMES } from '../../filter.model';

interface KeywordOption {
  value: string;
  label: string;
}

@Component({
  selector: 'gd-keyword-value-input',
  standalone: true,
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AsyncPipe,
    OverlayModule,
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
    MatPseudoCheckbox,
  ],
  styleUrl: './keyword-value-input.component.scss',
  template: `
    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="gd-value-input gd-keyword-value-input">
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
        @if (isMultiSelect()) {
          <input
            #valueInput
            type="text"
            [placeholder]="chipInputPlaceholder()"
            matAutocompletePosition="below"
            [formControl]="searchControl"
            [matChipInputFor]="chipGrid"
            [matChipInputAddOnBlur]="false"
            [attr.aria-expanded]="valuesOverlayOpen()"
            aria-haspopup="listbox"
            [attr.aria-controls]="valuesOverlayOpen() ? valuesListboxId : null"
            (focusin)="onMultiTriggerFocusIn()"
            (input)="onMultiTriggerInput()"
          />
        } @else {
          <input
            #valueInput
            #autoTrigger="matAutocompleteTrigger"
            [placeholder]="chipInputPlaceholder()"
            matAutocompletePosition="below"
            [formControl]="searchControl"
            [matAutocomplete]="keywordAuto"
            [matChipInputFor]="chipGrid"
            [matChipInputAddOnBlur]="false"
          />
          <mat-autocomplete
            #keywordAuto="matAutocomplete"
            class="gd-value-input-autocomplete-panel"
            [hideSingleSelectionIndicator]="true"
            (opened)="onAutocompletePanelOpened()"
            (closed)="onAutocompletePanelClosed()"
            (optionSelected)="onAutocompleteOptionSelected($event)"
          >
            @if (panelState$ | async; as panel) {
              @if (panel.loading && panel.options.length === 0) {
                <mat-option disabled>Loading…</mat-option>
              } @else if (panel.options.length > 0) {
                @for (option of panel.options; track option.value) {
                  <mat-option [value]="option.value">
                    <span class="gd-keyword-option">
                      <mat-pseudo-checkbox [state]="pseudoCheckboxState(option)" [disabled]="false" />
                      <span class="gd-keyword-option__label">{{ option.label }}</span>
                    </span>
                  </mat-option>
                }
              } @else if (!panel.loading) {
                <mat-option disabled>No matching values</mat-option>
              }
            }
          </mat-autocomplete>
        }
      </mat-chip-grid>
      @if (isMultiSelect()) {
        <ng-template #valuesOverlayTpl>
          <div class="gd-keyword-values-overlay-panel" [attr.id]="valuesListboxId" role="presentation">
            @let panel = panelState();
            @if (panel.loading && panel.options.length === 0) {
              <div class="gd-keyword-values-overlay__hint">Loading…</div>
            } @else if (panel.options.length > 0) {
              <div class="gd-keyword-values-overlay__scroll" role="listbox" [attr.aria-label]="searchLabel()" tabindex="-1">
                @for (option of panel.options; track option.value) {
                  <button
                    type="button"
                    class="gd-value-input-option-row"
                    role="option"
                    [attr.aria-selected]="selectedValues().includes(option.value)"
                    (click)="onMultiOptionClicked(option)"
                  >
                    <mat-pseudo-checkbox [state]="pseudoCheckboxState(option)" [disabled]="false" />
                    <span class="gd-value-input-option-row__label">{{ option.label }}</span>
                  </button>
                }
              </div>
            } @else if (!panel.loading) {
              <div class="gd-keyword-values-overlay__hint gd-keyword-values-overlay__hint--muted">No matching values</div>
            }
          </div>
        </ng-template>
      }
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
  private readonly overlay = inject(Overlay);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  definition = input.required<FilterDefinition>();
  selectedOperator = input.required<string>();
  timeFrom = input<number | undefined>(undefined);
  timeTo = input<number | undefined>(undefined);
  selectedValues = input<string[]>([]);
  /** When aligned with `selectedValues`, seeds chip labels in the dialog (e.g. edit mode). */
  valueLabels = input<string[] | undefined>(undefined);
  valuesChange = output<FilterValueSelection>();

  protected readonly searchControl = new FormControl<string>('', { nonNullable: true });
  protected readonly valueInput = viewChild<ElementRef<HTMLInputElement>>('valueInput');
  private readonly autocompleteTrigger = viewChild(MatAutocompleteTrigger);
  private readonly keywordAutocomplete = viewChild(MatAutocomplete);
  private readonly valuesOverlayTpl = viewChild<TemplateRef<void>>('valuesOverlayTpl');

  private readonly loading$ = new BehaviorSubject<boolean>(false);
  private readonly hasNextPage$ = new BehaviorSubject<boolean>(false);

  private readonly page$ = new BehaviorSubject<number>(1);
  private readonly accumulatedOptions$ = new BehaviorSubject<KeywordOption[]>([]);
  private readonly labelByValue = signal<Map<string, string>>(new Map());
  private panelScrollSub?: Subscription;
  private autocompletePanelOpen = false;
  /** CDK overlay for multi-select value list (stable scroll, no mat-autocomplete close cycle). */
  private multiOverlayRef?: OverlayRef;
  private multiBackdropSub?: Subscription;
  private multiEscapeSub?: Subscription;
  /** Stable id for aria-controls on the chip input when the overlay listbox is open. */
  protected readonly valuesListboxId = `gd-keyword-values-${Math.random().toString(36).slice(2, 11)}`;
  protected readonly valuesOverlayOpen = signal(false);

  /**
   * Last debounced search term used for a page-1 fetch. Used with `canReuseAccumulatedOptionsForTerm`
   * and for single-select autocomplete restore after Material assigns the option value.
   */
  private lastKeywordSearchTermForList = '';
  /** Bounds key from the last completed page-1 fetch — must invalidate cache when `timeFrom`/`timeTo` inputs change. */
  private lastKeywordFetchBoundsKey = '';

  private readonly injector = inject(Injector);

  protected readonly panelState$ = combineLatest({
    loading: this.loading$,
    options: this.accumulatedOptions$.asObservable(),
    hasNextPage: this.hasNextPage$,
  });

  protected readonly panelState = toSignal(this.panelState$, {
    initialValue: { loading: false, options: [] as KeywordOption[], hasNextPage: false },
  });

  protected readonly isMultiSelect = computed(() => isMultiSelectForFilter(this.definition(), this.selectedOperator()));

  protected readonly isIdBased = computed(() => ID_BASED_FILTER_NAMES.includes(this.definition().name as string));
  protected readonly searchLabel = computed(() => (this.isIdBased() ? 'Search by name' : 'Search (exact match)'));

  protected readonly chipInputPlaceholder = computed(() => (this.selectedChips().length > 0 ? '' : this.searchLabel()));

  protected readonly selectedChips = computed(() => {
    const labels = this.labelByValue();
    return this.selectedValues().map(value => ({
      value,
      label: labels.get(value) ?? value,
    }));
  });

  constructor() {
    effect(
      () => {
        const ids = this.selectedValues();
        const fromParent = this.valueLabels();
        if (fromParent == null || fromParent.length !== ids.length) {
          return;
        }
        const next = new Map<string, string>();
        ids.forEach((id, i) => next.set(id, fromParent[i]!));
        this.labelByValue.set(next);
      },
      { allowSignalWrites: true },
    );

    effect(() => {
      if (!this.isMultiSelect() && this.multiOverlayRef?.hasAttached()) {
        this.closeMultiValuesOverlay();
      }
    });

    // Re-bind scroll when the list grows (first paint had no scroll surface / wrong height).
    effect(() => {
      if (!this.isMultiSelect() || !this.valuesOverlayOpen()) {
        return;
      }
      const panel = this.panelState();
      void panel.options.length;
      void panel.loading;
      void panel.hasNextPage;
      queueMicrotask(() => {
        requestAnimationFrame(() => this.attachValuesPanelScrollListener());
      });
    });

    const debouncedTerm$ = this.searchControl.valueChanges.pipe(startWith(''), debounceTime(200), distinctUntilChanged());

    const timeBoundsKey$ = toObservable(
      computed(() => this.fetchTimeBoundsKey()),
      { injector: this.injector },
    );

    combineLatest({ term: debouncedTerm$, boundsKey: timeBoundsKey$ })
      .pipe(
        tap(({ term }) => {
          if (this.canReuseAccumulatedOptionsForTerm(term)) {
            return;
          }
          this.accumulatedOptions$.next([]);
          if (this.page$.getValue() !== 1) {
            this.page$.next(1);
          }
        }),
        switchMap(({ term }) =>
          this.page$.pipe(
            switchMap(page => {
              if (page === 1 && this.canReuseAccumulatedOptionsForTerm(term)) {
                return of(undefined);
              }
              return this.fetchKeywordPage(term, page);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.destroyRef.onDestroy(() => {
      this.detachPanelScrollListener();
      this.unsubscribeMultiOverlayUi();
      this.multiOverlayRef?.dispose();
      this.multiOverlayRef = undefined;
    });
  }

  /** Public for unit tests: attach scroll listener to the values panel (autocomplete or CDK overlay). */
  attachAutocompletePanelScrollListener(): void {
    this.attachValuesPanelScrollListener();
  }

  protected pseudoCheckboxState(option: KeywordOption): MatPseudoCheckboxState {
    return this.selectedValues().includes(option.value) ? 'checked' : 'unchecked';
  }

  protected onMultiTriggerFocusIn(): void {
    if (!this.isMultiSelect()) {
      return;
    }
    this.ensureMultiValuesOverlay();
  }

  protected onMultiTriggerInput(): void {
    if (!this.isMultiSelect()) {
      return;
    }
    this.ensureMultiValuesOverlay();
  }

  protected onMultiOptionClicked(option: KeywordOption): void {
    const value = option.value;
    const current = [...this.selectedValues()];
    const idx = current.indexOf(value);
    if (idx >= 0) {
      current.splice(idx, 1);
    } else {
      current.push(value);
      this.updateLabels([{ value, label: option.label }]);
    }
    this.emitSelection(current);
    this.changeDetectorRef.markForCheck();
  }

  protected onAutocompleteOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value as string;
    if (value == null || value === '') {
      return;
    }

    if (this.isMultiSelect()) {
      return;
    }
    const label = this.accumulatedOptions$.getValue().find(o => o.value === value)?.label ?? this.labelByValue().get(value) ?? value;
    this.updateLabels([{ value, label }]);
    this.emitSelection([value]);
    this.clearSearchInputAfterPick();
  }

  protected removeValue(value: string): void {
    const next = this.selectedValues().filter(v => v !== value);
    this.emitSelection(next);
  }

  protected onAutocompletePanelOpened(): void {
    this.autocompletePanelOpen = true;
    this.attachValuesPanelScrollListener();
  }

  protected onAutocompletePanelClosed(): void {
    this.autocompletePanelOpen = false;
    this.detachPanelScrollListener();
  }

  private emitSelection(values: string[]): void {
    const map = this.labelByValue();
    const valueLabels = values.map(id => map.get(id) ?? id);
    this.valuesChange.emit({ values, valueLabels });
  }

  private fetchTimeBoundsKey(): string {
    return `${this.timeFrom() ?? ''}\u0000${this.timeTo() ?? ''}`;
  }

  private canReuseAccumulatedOptionsForTerm(term: string): boolean {
    return (
      term === this.lastKeywordSearchTermForList &&
      this.fetchTimeBoundsKey() === this.lastKeywordFetchBoundsKey &&
      this.accumulatedOptions$.getValue().length > 0 &&
      !this.loading$.getValue()
    );
  }

  private fetchKeywordPage(term: string, page: number): Observable<void> {
    if (page === 1) {
      this.lastKeywordSearchTermForList = term;
      this.lastKeywordFetchBoundsKey = this.fetchTimeBoundsKey();
    }
    this.loading$.next(true);
    return this.valuesProvider
      .getValues({
        filterName: this.definition().name as string,
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
          let merged = page === 1 ? options : this.mergeUnique(current, options);
          if (page === 1) {
            merged = this.mergeSelectedIntoOptions(merged);
          }
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
    if (input) {
      input.value = '';
    }
  }

  private mergeUnique(current: KeywordOption[], incoming: KeywordOption[]): KeywordOption[] {
    const seen = new Set(current.map(o => o.value));
    const additions = incoming.filter(o => !seen.has(o.value));
    return [...current, ...additions];
  }

  private mergeSelectedIntoOptions(options: KeywordOption[]): KeywordOption[] {
    const map = this.labelByValue();
    const have = new Set(options.map(o => o.value));
    const extras: KeywordOption[] = [];
    for (const v of this.selectedValues()) {
      if (!have.has(v)) {
        extras.push({ value: v, label: map.get(v) ?? v });
      }
    }
    return extras.length > 0 ? this.mergeUnique(extras, options) : options;
  }

  private updateLabels(options: KeywordOption[]): void {
    const next = new Map(this.labelByValue());
    options.forEach(o => next.set(o.value, o.label));
    this.labelByValue.set(next);
  }

  private ensureMultiValuesOverlay(): void {
    const tpl = this.valuesOverlayTpl();
    const inputEl = this.valueInput()?.nativeElement;
    if (!tpl || !inputEl || !this.isMultiSelect()) {
      return;
    }
    const originEl = (inputEl.closest('.mat-mdc-form-field') as HTMLElement | null) ?? inputEl;
    if (this.multiOverlayRef?.hasAttached()) {
      const width = originEl.getBoundingClientRect().width;
      if (width > 0) {
        this.multiOverlayRef.updateSize({ width });
      }
      this.multiOverlayRef.updatePosition();
      return;
    }
    this.createAndAttachMultiOverlay(tpl, originEl);
  }

  private createAndAttachMultiOverlay(tpl: TemplateRef<void>, origin: HTMLElement): void {
    if (this.multiOverlayRef) {
      this.unsubscribeMultiOverlayUi();
      this.multiOverlayRef.dispose();
      this.multiOverlayRef = undefined;
    }

    const positionStrategy = this.overlay
      .position()
      .flexibleConnectedTo(origin)
      .withViewportMargin(8)
      .withLockedPosition(true)
      .withPositions([
        { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top', offsetY: 8 },
        { originX: 'start', originY: 'top', overlayX: 'start', overlayY: 'bottom', offsetY: -8 },
      ])
      .withPush(false);

    const width = Math.max(origin.getBoundingClientRect().width, 1);

    this.multiOverlayRef = this.overlay.create({
      positionStrategy,
      scrollStrategy: this.overlay.scrollStrategies.reposition(),
      hasBackdrop: true,
      backdropClass: 'cdk-overlay-transparent-backdrop',
      minHeight: 120,
      maxHeight: 280,
      width,
      disposeOnNavigation: true,
      panelClass: ['gd-value-input-values-panel', 'gd-keyword-values-overlay'],
    });

    this.multiOverlayRef.attach(new TemplatePortal(tpl, this.viewContainerRef));

    this.multiBackdropSub = this.multiOverlayRef.backdropClick().subscribe(() => this.closeMultiValuesOverlay());
    this.multiEscapeSub = this.multiOverlayRef
      .keydownEvents()
      .pipe(filter(e => e.key === 'Escape'))
      .subscribe(e => {
        e.preventDefault();
        this.closeMultiValuesOverlay();
      });

    this.valuesOverlayOpen.set(true);
    this.changeDetectorRef.markForCheck();
    queueMicrotask(() => this.attachValuesPanelScrollListener());
  }

  private closeMultiValuesOverlay(): void {
    this.unsubscribeMultiOverlayUi();
    if (this.multiOverlayRef?.hasAttached()) {
      this.multiOverlayRef.detach();
    }
    this.valuesOverlayOpen.set(false);
    this.detachPanelScrollListener();
    this.changeDetectorRef.markForCheck();
  }

  private unsubscribeMultiOverlayUi(): void {
    this.multiBackdropSub?.unsubscribe();
    this.multiEscapeSub?.unsubscribe();
    this.multiBackdropSub = undefined;
    this.multiEscapeSub = undefined;
  }

  private attachValuesPanelScrollListener(): void {
    this.detachPanelScrollListener();
    queueMicrotask(() => {
      requestAnimationFrame(() => {
        const panelRoot = this.getValuesPanelScrollRoot();
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

  private getValuesPanelScrollRoot(): HTMLElement | null {
    if (this.isMultiSelect()) {
      if (this.multiOverlayRef?.hasAttached()) {
        const fromRef = this.multiOverlayRef.overlayElement.querySelector<HTMLElement>('.gd-keyword-values-overlay__scroll');
        if (fromRef) {
          return fromRef;
        }
      }
      try {
        const host = this.overlayContainer.getContainerElement();
        return host.querySelector<HTMLElement>('.gd-keyword-values-overlay__scroll');
      } catch {
        return null;
      }
    }
    return this.getAutocompletePanelElement();
  }

  private getAutocompletePanelElement(): HTMLElement | null {
    try {
      const overlayHost = this.overlayContainer.getContainerElement();
      const fromAutocomplete = this.keywordAutocomplete()?.panel?.nativeElement;
      if (fromAutocomplete instanceof HTMLElement && overlayHost.contains(fromAutocomplete)) {
        return fromAutocomplete;
      }
      return overlayHost.querySelector<HTMLElement>('.mat-mdc-autocomplete-panel.gd-value-input-autocomplete-panel');
    } catch {
      return null;
    }
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
    if (!this.isValuesPanelOpen()) {
      return;
    }
    const panelRoot = this.getValuesPanelScrollRoot();
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

  private isValuesPanelOpen(): boolean {
    return this.isMultiSelect() ? this.valuesOverlayOpen() : this.autocompletePanelOpen;
  }

  private detachPanelScrollListener(): void {
    this.panelScrollSub?.unsubscribe();
    this.panelScrollSub = undefined;
  }
}
