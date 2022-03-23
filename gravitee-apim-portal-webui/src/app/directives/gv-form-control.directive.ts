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
import { AfterViewInit, Directive, ElementRef } from '@angular/core';
import { NgControl, NgControlStatus } from '@angular/forms';

@Directive({
  selector:
    'gv-input[formControl],gv-input[formControlName],' +
    'gv-text[formControl],gv-text[formControlName],' +
    'gv-select[formControl],gv-select[formControlName],' +
    'gv-date-picker[formControl],gv-date-picker[formControlName],' +
    '[gvControl]',
})
export class GvFormControlDirective extends NgControlStatus implements AfterViewInit {
  private control: NgControl;

  constructor(control: NgControl, private elementRef: ElementRef) {
    super(control);
    this.control = control;
  }

  private update(status: string | null) {
    setTimeout(() => {
      if (status === 'VALID') {
        this.elementRef.nativeElement.setAttribute('valid', true);
        this.elementRef.nativeElement.removeAttribute('invalid');
      } else if (status === 'INVALID') {
        this.elementRef.nativeElement.setAttribute('invalid', true);
        this.elementRef.nativeElement.removeAttribute('valid');
      }
    }, 0);
  }

  ngAfterViewInit() {
    this.update(this.control.status);
    if (this.control.statusChanges) {
      this.control.statusChanges.subscribe(status => {
        this.update(status);
      });
    }
  }
}
