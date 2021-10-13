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
import { FocusMonitor } from '@angular/cdk/a11y';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Component, DoCheck, ElementRef, HostBinding, Input, OnDestroy, Optional, Self, ViewChild } from '@angular/core';
import { ControlValueAccessor, NgControl } from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';
import { MatFormFieldControl } from '@angular/material/form-field';
import { isEmpty } from 'lodash';
import { Subject } from 'rxjs';

export type Tags = Array<string>;

@Component({
  selector: 'gio-form-tags-input',
  template: require('./gio-form-tags-input.component.html'),
  styles: [require('./gio-form-tags-input.component.scss')],
  host: {
    '[id]': 'id',
    '[attr.aria-describedby]': 'describedBy',
  },
  providers: [
    {
      provide: MatFormFieldControl,
      useExisting: GioFormTagsInputComponent,
    },
  ],
})
export class GioFormTagsInputComponent implements MatFormFieldControl<Tags>, ControlValueAccessor, DoCheck, OnDestroy {
  static nextId = 0;

  _onChange: (value: any) => void = () => ({});

  _onTouched: () => any = () => ({});
  touched = false;

  @Input('aria-label')
  ariaLabel: string;

  @Input()
  addOnBlur = true;

  /**
   * Function called each time a tag is added, it can be used to hook inside the
   * addition process to, for instance, have custom validation in the components
   * using `gio-form-tags-input`.
   *
   * Parameters are:
   * - `tag`: The value of the tag to add
   * - `validationCb`: The callback function to call when validation is done. If
   * called with `true` it will add the tag, otherwise it will just ignore it
   */
  @Input()
  tagValidationHook: (tag: string, validationCb: (shouldAddTag: boolean) => void) => void;

  @ViewChild('tagInput')
  tagInput: ElementRef<HTMLInputElement>;

  // From ControlValueAccessor interface
  get value(): Tags | null {
    return this._value;
  }
  set value(_tags: Tags | null) {
    this._value = _tags;
    this._onChange(_tags);
    this.stateChanges.next();
  }
  private _value: Tags | null = null;

  // From ControlValueAccessor interface
  stateChanges = new Subject<void>();

  // From ControlValueAccessor interface
  @HostBinding()
  id = `gio-form-tags-input-${GioFormTagsInputComponent.nextId++}`;

  // From ControlValueAccessor interface
  @Input()
  get placeholder() {
    return this._placeholder;
  }
  set placeholder(plh) {
    this._placeholder = plh;
    this.stateChanges.next();
  }
  private _placeholder: string;

  // From ControlValueAccessor interface
  focused: boolean;

  // From ControlValueAccessor interface
  get empty() {
    return isEmpty(this.value) && isEmpty(this.tagInput?.nativeElement?.value);
  }

  // From ControlValueAccessor interface
  @HostBinding('class.floating')
  get shouldLabelFloat() {
    return this.focused || !this.empty;
  }

  // From ControlValueAccessor interface
  @Input()
  get required() {
    return this._required;
  }
  set required(req) {
    this._required = coerceBooleanProperty(req);
    this.stateChanges.next();
  }
  private _required = false;

  // From ControlValueAccessor interface
  @Input()
  get disabled() {
    return this._disabled || (this.ngControl && this.ngControl.disabled);
  }
  set disabled(dis) {
    this._disabled = coerceBooleanProperty(dis);
    this.stateChanges.next();
  }
  private _disabled = false;

  // From ControlValueAccessor interface
  get errorState(): boolean {
    return (
      this.touched &&
      // if required check if is empty
      ((this.required && this.empty) ||
        // if there is a touched control check if there is an error
        (this.ngControl && this.ngControl.touched && !!this.ngControl.errors))
    );
  }

  // From ControlValueAccessor interface
  controlType?: string;

  // From ControlValueAccessor interface
  autofilled?: boolean;

  // From ControlValueAccessor interface
  userAriaDescribedBy?: string;

  constructor(
    // From ControlValueAccessor interface
    @Optional() @Self() public readonly ngControl: NgControl,
    private readonly elRef: ElementRef,
    private readonly fm: FocusMonitor,
  ) {
    // Replace the provider from above with this.
    if (this.ngControl != null) {
      // Setting the value accessor directly (instead of using
      // the providers) to avoid running into a circular import.
      this.ngControl.valueAccessor = this;
    }

    fm.monitor(elRef.nativeElement, true).subscribe((origin) => {
      this.focused = !!origin;
      this._onTouched();
      this.touched = true;
      this.stateChanges.next();
    });
  }

  ngDoCheck() {
    // sync control touched with local touched
    if (this.ngControl != null && this.touched !== this.ngControl.touched) {
      this.touched = this.ngControl.touched;
      this.stateChanges.next();
    }
  }

  ngOnDestroy() {
    this.stateChanges.complete();
    this.fm.stopMonitoring(this.elRef.nativeElement);
  }

  // From ControlValueAccessor interface
  writeValue(value: string[]): void {
    this._value = value;
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
  @HostBinding('attr.aria-describedby') describedBy = '';
  setDescribedByIds(ids: string[]): void {
    this.describedBy = ids.join(' ');
  }

  // From ControlValueAccessor interface
  // eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/no-empty-function
  onContainerClick(_event: MouseEvent): void {}

  addChipToFormControl(event: MatChipInputEvent) {
    const input = event.chipInput.inputElement;
    const tagToAdd = (event.value ?? '').trim();

    if (isEmpty(tagToAdd)) {
      return;
    }

    // Add the tag only if shouldAddTag in validationCb return true
    // Set default callback if not defined
    const tagValidationHook = this.tagValidationHook ?? ((_, validationCb) => validationCb(true));

    const validationCb = (shouldAddTag: boolean) => {
      // Add new Tag in form control
      if (shouldAddTag) {
        // Delete Tag if already existing
        const formControlValue = [...(this.value ?? [])].filter((v) => v !== tagToAdd);

        this.value = [...formControlValue, tagToAdd];
      }
    };

    // Reset the input value
    if (input) {
      input.value = '';
    }

    tagValidationHook(tagToAdd, validationCb);
  }

  removeChipToFormControl(tag: string) {
    this.value = [...(this.value ?? [])].filter((v) => v !== tag);
  }
}
