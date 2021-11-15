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
import { Directive, HostListener, ElementRef, Renderer2 } from '@angular/core';

@Directive({
  selector: 'form[gioFormFocusInvalid]',
})
export class GioFormFocusInvalidDirective {
  constructor(private el: ElementRef, private renderer: Renderer2) {}

  @HostListener('submit')
  onFormSubmit() {
    try {
      const invalidControl = this.renderer.selectRootElement('.ng-invalid[formControlName]', true);
      if (invalidControl) {
        invalidControl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    } catch (error) {
      // Best effort. If the focus doesn't work it's not very important
      // 🧪 Useful to avoid som error in tests
    }
  }
}
