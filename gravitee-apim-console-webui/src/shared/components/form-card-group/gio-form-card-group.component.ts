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
import { ChangeDetectorRef, Component, ContentChildren, forwardRef, QueryList } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { GioFormCardComponent } from './gio-form-card.component';

@Component({
  selector: 'gio-form-card-group',
  template: require('./gio-form-card-group.component.html'),
  styles: [require('./gio-form-card-group.component.scss')],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => GioFormCardGroupComponent),
    },
  ],
})
export class GioFormCardGroupComponent implements ControlValueAccessor {
  @ContentChildren(forwardRef(() => GioFormCardComponent), { descendants: true })
  private selectCardsList: QueryList<GioFormCardComponent>;

  selection: string;

  // TODO: not implemented
  disabled = false;

  _onChange: (value: any) => void = () => ({});

  _onTouched: () => any = () => ({});

  constructor(private readonly changeDetector: ChangeDetectorRef) {}

  ngAfterContentInit() {
    // Mark this component as initialized in AfterContentInit because the initial value can
    // possibly be set by NgModel on MatRadioGroup, and it is possible that the OnInit of the
    // NgModel occurs *after* the OnInit of the MatRadioGroup.
    // this._isInitialized = true;
    this.selectCardsList.forEach((card) => {
      if (card.value === this.selection) {
        card.selected = true;

        this.changeDetector.markForCheck();
        card._markForCheck();
      }
      card.onSelectFn = (value) => {
        this.updateCardsSelection(value);
      };
    });
  }

  // From ControlValueAccessor interface
  writeValue(value: any): void {
    this.selection = value;
    this.updateCardsSelection(value);
    this.changeDetector.markForCheck();
  }

  // From ControlValueAccessor interface
  registerOnChange(fn: any): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  // From ControlValueAccessor interface
  setDisabledState(isDisabled: boolean): void {
    // TODO: not implemented
    this.disabled = isDisabled;
    this.changeDetector.markForCheck();
  }

  updateCardsSelection(value: string) {
    if (this.selectCardsList) {
      this.selectCardsList.forEach((card) => {
        const newSelectedValue = card.value === value;

        if (newSelectedValue !== card.selected) {
          card.selected = card.value === value;
          card._markForCheck();
        }
      });

      this.selection = value;
      this._onChange(this.selection);
      this._onTouched();
    }
  }
}
