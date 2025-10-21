/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Directive, inject, input, computed, ElementRef, Renderer2, effect } from '@angular/core';

import { ObservabilityBreakpointService } from '../services/observability-breakpoint.service';

/**
 * Conditionally applies a CSS class to the host element when the viewport is detected as mobile
 * by the `ObservabilityBreakpointService`.
 *
 * @usage
 * <div [appIsMobile]="'my-mobile-class'">...</div>
 *
 * @param appIsMobile The string name of the CSS class to apply when the state is mobile.
 */
@Directive({
  selector: '[appIsMobile]',
  standalone: true,
})
export class MobileClassDirective {
  public appIsMobile = input<string>();

  private el = inject(ElementRef);
  private renderer = inject(Renderer2);
  private isMobileState = inject(ObservabilityBreakpointService).isMobile;
  private activeMobileClass = computed(() => {
    return this.isMobileState() ? this.appIsMobile() : null;
  });

  constructor() {
    effect(onCleanup => {
      const className = this.activeMobileClass();
      if (className) {
        this.renderer.addClass(this.el.nativeElement, className);
      }
      onCleanup(() => {
        if (className) {
          this.renderer.removeClass(this.el.nativeElement, className);
        }
      });
    });
  }
}
