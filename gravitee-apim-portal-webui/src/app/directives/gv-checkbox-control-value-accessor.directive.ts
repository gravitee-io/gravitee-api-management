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
import { Directive, ElementRef, forwardRef, Renderer2 } from '@angular/core';
import { CheckboxControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

const GV_CHECKBOX_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => GvCheckboxControlValueAccessorDirective),
  multi: true,
};

@Directive({
  selector: 'gv-checkbox[formControlName],gv-checkbox[formControl],gv-checkbox[ngModel],gv-checkbox[gvControl]',
  providers: [GV_CHECKBOX_ACCESSOR],
})
export class GvCheckboxControlValueAccessorDirective extends CheckboxControlValueAccessor {
  constructor(_renderer: Renderer2, _elementRef: ElementRef) {
    super(_renderer, _elementRef);
  }
}
