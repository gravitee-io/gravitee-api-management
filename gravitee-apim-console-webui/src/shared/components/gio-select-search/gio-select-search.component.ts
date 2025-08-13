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
import { Component, ElementRef, forwardRef, input, signal, ViewChild, OnDestroy, inject, DestroyRef } from '@angular/core';
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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GioSelectSearchOverlayComponent } from './gio-select-search-overlay.component';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

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
  // Inputs
  options = input.required<SelectOption[]>();
  label = input.required<string>();
  placeholder = input<string>('Search...');

  // Internal state
  protected isOpen = signal(false);
  protected multiple: boolean = true;
  private overlayRef: OverlayRef | null = null;
  private readonly destroyRef = inject(DestroyRef);

  // Form control integration
  protected _value: string[] = [];
  protected isDisabled = false;
  private onChange = (_value: string[]) => {};
  private onTouched = () => {};

  // Computed values
  selectedCount = signal(0);

  private readonly overlay = inject(Overlay);
  private readonly overlayPositionBuilder = inject(OverlayPositionBuilder);

  @ViewChild('trigger', { static: false }) trigger!: ElementRef<HTMLElement>;

  ngOnDestroy() {
    this.closeOverlay();
  }

  // ControlValueAccessor implementation
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

  toggleOverlay(event: Event) {
    event.stopPropagation();
    if (this.isDisabled) return;

    if (this.isOpen()) {
      this.closeOverlay();
    } else {
      this.openOverlay();
    }
  }

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
    const componentRef = this.overlayRef.attach(portal);

    // Pass data to overlay component
    componentRef.instance.options.set(this.options());
    componentRef.instance.selectedValues = [...this._value];
    componentRef.instance.placeholder = this.placeholder();

    // Handle selection changes
    componentRef.instance.selectionChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((selectedValue: string) => {
      this.onSelectionChange(selectedValue);
      componentRef.instance.selectedValues = [...this._value];
    });
    componentRef.instance.clearSelectionChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.updateValue([]);
      componentRef.instance.selectedValues = [...this._value];
    });

    this.overlayRef.backdropClick().subscribe(() => this.closeOverlay());

    this.isOpen.set(true);
  }

  private closeOverlay() {
    this.overlayRef?.detach();
    this.overlayRef?.dispose();
    this.overlayRef = null;
    this.isOpen.set(false);
  }

  private updateValue(value: string[]) {
    this._value = value || [];
    this.selectedCount.set(this._value.length);
    this.onChange(this._value);
    this.onTouched();
  }
}
