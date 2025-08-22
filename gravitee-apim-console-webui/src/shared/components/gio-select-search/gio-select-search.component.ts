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
import {
  Component,
  ElementRef,
  forwardRef,
  input,
  signal,
  ViewChild,
  OnDestroy,
  inject,
  DestroyRef,
  effect,
  ComponentRef,
  Signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { Overlay, OverlayPositionBuilder, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { merge, Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, switchMap } from 'rxjs/operators';

import { GioSelectSearchOverlayComponent } from './gio-select-search-overlay.component';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface ResultsLoaderInput {
  searchTerm: string;
  page: number;
}

export interface ResultsLoaderOutput {
  data: SelectOption[];
  hasNextPage: boolean;
}

type ResultsState = ResultsLoaderOutput & {
  isLoading: boolean;
};

@Component({
  selector: 'gio-select-search',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatIconModule,
    MatInputModule,
    MatButtonModule,
    GioIconsModule,
  ],
  templateUrl: './gio-select-search.component.html',
  styleUrl: './gio-select-search.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioSelectSearchComponent),
      multi: true,
    },
  ],
})
export class GioSelectSearchComponent implements ControlValueAccessor, OnDestroy {
  // INPUTS
  options = input<SelectOption[]>([]);
  label = input.required<string>();
  placeholder = input<string>('Search...');
  resultsLoader = input<(data: ResultsLoaderInput) => Observable<ResultsLoaderOutput>>(({ searchTerm }) => {
    const data: SelectOption[] = searchTerm
      ? this.options().filter((option) => option.label.toLowerCase().includes(searchTerm.toLowerCase()))
      : this.options();
    return of({ data, hasNextPage: false });
  });

  // INTERNAL STATE & SIGNALS
  protected isOpen = signal(false);
  protected selectedCount = signal(0);
  protected multiple: boolean = true;

  // FORM CONTROL INTEGRATION
  protected _value: string[] = [];
  protected isDisabled = false;
  private onChange = (_value: string[]) => {};
  private onTouched = () => {};

  // PAGINATION & SEARCH STATE
  private searchParams = signal<{ searchTerm: string; page: number }>({
    searchTerm: '',
    page: 1,
  });
  private accumulatedOptions: SelectOption[] = [];
  private searchState$ = toObservable(
    computed(() => ({
      searchTerm: this.searchParams().searchTerm,
      page: this.searchParams().page,
      isOpen: this.isOpen(),
    })),
  );

  private resultsState: Signal<ResultsState> = toSignal(this.loadResults$());

  // OVERLAY & COMPONENT REFERENCES
  private overlayRef: OverlayRef | null = null;
  private componentRef: ComponentRef<GioSelectSearchOverlayComponent> = null;

  // INJECTED SERVICES
  private readonly overlay = inject(Overlay);
  private readonly overlayPositionBuilder = inject(OverlayPositionBuilder);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('trigger', { static: false }) trigger!: ElementRef<HTMLElement>;

  constructor() {
    effect(() => {
      if (this.isOpen() && this.componentRef) {
        this.updateOverlayData(this.componentRef);
      }
    });
  }

  ngOnDestroy() {
    this.closeOverlay();
  }

  // CONTROL VALUE ACCESSOR IMPLEMENTATION
  writeValue(value: string[]): void {
    this._value = value || [];
    this.selectedCount.set(this._value.length);
  }

  registerOnChange(fn: (value: string[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
  }

  // PUBLIC METHODS
  toggleOverlay(event: Event) {
    event.stopPropagation();
    if (this.isDisabled) return;

    if (this.isOpen()) {
      this.closeOverlay();
    } else {
      this.openOverlay();
    }
  }

  // OVERLAY MANAGEMENT
  private openOverlay() {
    if (!this.trigger) return;

    if (this.overlayRef) {
      this.closeOverlay();
    }

    const positionStrategy = this.overlayPositionBuilder
      .flexibleConnectedTo(this.trigger)
      .withPositions([
        {
          originX: 'start',
          originY: 'bottom',
          overlayX: 'start',
          overlayY: 'top',
        },
        {
          originX: 'start',
          originY: 'top',
          overlayX: 'start',
          overlayY: 'bottom',
        },
      ])
      .withGrowAfterOpen(true)
      .withPush(true);

    this.overlayRef = this.overlay.create({
      positionStrategy,
      hasBackdrop: true,
      backdropClass: 'cdk-overlay-transparent-backdrop',
      scrollStrategy: this.overlay.scrollStrategies.reposition(),
    });

    const portal = new ComponentPortal(GioSelectSearchOverlayComponent);
    this.componentRef = this.overlayRef.attach(portal);

    // Pass data to overlay component
    this.updateOverlayData(this.componentRef);

    // Handle selection changes
    this.componentRef.instance.selectionChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((selectedValue: string) => {
      this.onSelectionChange(selectedValue);
      this.componentRef.instance.selectedValues = [...this._value];
    });

    this.componentRef.instance.clearSelectionChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.updateValue([]);
      this.componentRef.instance.selectedValues = [...this._value];
    });

    this.componentRef.instance.loadMoreChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.searchParams.update((params) => ({
        ...params,
        page: params.page + 1,
      }));
    });

    this.componentRef.instance.searchChange.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe((searchTerm: string) => {
      this.searchParams.set({
        searchTerm: searchTerm,
        page: 1, // Reset page to 1 on new search
      });
    });

    this.overlayRef.backdropClick().subscribe(() => this.closeOverlay());

    this.isOpen.set(true);
  }

  private updateOverlayData(componentRef: ComponentRef<GioSelectSearchOverlayComponent>) {
    if (!componentRef || !componentRef.instance) {
      return;
    }

    componentRef.instance.options.set(this.resultsState().data);
    componentRef.instance.selectedValues = [...this._value];
    componentRef.instance.placeholder = this.placeholder();
    componentRef.instance.isLoading = this.resultsState().isLoading;
    componentRef.instance.hasNextPage = this.resultsState().hasNextPage;
  }

  private closeOverlay() {
    this.overlayRef?.detach();
    this.overlayRef?.dispose();
    this.overlayRef = null;
    this.componentRef = null;
    this.isOpen.set(false);
  }

  // VALUE MANAGEMENT
  private onSelectionChange(selectedValue: string) {
    const currentValues = this._value || [];

    const index = currentValues.indexOf(selectedValue);
    if (index > -1) {
      currentValues.splice(index, 1);
    } else {
      currentValues.push(selectedValue);
    }

    this.updateValue(currentValues);
  }

  private updateValue(value: string[]) {
    this._value = value || [];
    this.selectedCount.set(this._value.length);
    this.onChange(this._value);
    this.onTouched();
  }

  // PAGINATION & SEARCH LOGIC
  private loadResults$(): Observable<ResultsState> {
    return this.searchState$.pipe(
      distinctUntilChanged((prev, curr) => prev.searchTerm === curr.searchTerm && prev.page === curr.page && prev.isOpen === curr.isOpen),
      switchMap(({ searchTerm, page, isOpen }) => {
        // If component is closed, return default state
        if (!isOpen) {
          return of({ data: [], isLoading: false, hasNextPage: false });
        }

        if (page === 1) {
          this.accumulatedOptions = [];
        }

        const loadingState = of({
          data: this.accumulatedOptions,
          isLoading: true,
          hasNextPage: true,
        });

        return merge(loadingState, this.accumulateNewPageFromBackend$(searchTerm, page));
      }),
    );
  }

  private accumulateNewPageFromBackend$(searchTerm: string, page: number): Observable<ResultsState> {
    return this.resultsLoader()({ searchTerm, page }).pipe(map(({ data, hasNextPage }) => this.accumulateNewOptions(data, hasNextPage)));
  }

  private accumulateNewOptions(newOptions: SelectOption[], hasNextPage: boolean): ResultsState {
    const newUniqueOptions = newOptions.filter((option) => !this.accumulatedOptions.some(({ value }) => value === option.value));
    this.accumulatedOptions = [...this.accumulatedOptions, ...newUniqueOptions];

    return {
      data: this.accumulatedOptions,
      isLoading: false,
      hasNextPage,
    };
  }
}
