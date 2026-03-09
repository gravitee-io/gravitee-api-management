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
import { Component, DestroyRef, effect, ElementRef, inject, input } from '@angular/core';

import { REDOC_BREAKPOINT_LARGE, REDOC_BREAKPOINT_MEDIUM } from '../../services/redoc-defaults';
import { RedocService } from '../../services/redoc.service';

/**
 * Delay in milliseconds before initializing Redoc.
 * Set to 0 to defer initialization to the next event loop tick,
 * ensuring the DOM container is available.
 */
const RENDER_DELAY_MS = 0;

@Component({
  selector: 'app-redoc-content-viewer',
  imports: [],
  templateUrl: './redoc-content-viewer.component.html',
  styleUrl: './redoc-content-viewer.component.scss',
})
export class RedocContentViewerComponent {
  content = input.required<string>();

  private readonly destroyRef = inject(DestroyRef);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly redocService = inject(RedocService);
  private pendingTimeoutId: number | null = null;

  constructor() {
    effect(onCleanup => {
      this.scheduleRedocInit(this.content());
      onCleanup(() => {
        if (this.pendingTimeoutId !== null) {
          clearTimeout(this.pendingTimeoutId);
          this.pendingTimeoutId = null;
        }
      });
    });

    this.destroyRef.onDestroy(() => {
      const redocElement = this.element.nativeElement?.querySelector('#redoc') as HTMLElement | null;
      if (redocElement) {
        redocElement.innerHTML = '';
      }
    });
  }

  /**
   * Schedules Redoc initialization after the next tick so the #redoc container is in the DOM.
   * The innerHTML is cleared before initialization to ensure a clean render state.
   */
  private scheduleRedocInit(spec: string): void {
    this.pendingTimeoutId = window.setTimeout(() => this.initializeRedoc(spec), RENDER_DELAY_MS);
  }

  /**
   * Initializes Redoc in the #redoc container: clears pending timeout, queries the container,
   * clears its content, and calls RedocService.init with options (breakpoints from host CSS vars).
   */
  private initializeRedoc(spec: string): void {
    this.pendingTimeoutId = null;
    const redocElement = this.element.nativeElement?.querySelector('#redoc') as HTMLElement | null;
    if (!redocElement || !spec) {
      return;
    }
    redocElement.innerHTML = '';
    this.redocService.init(spec, this.getRedocOptions(), redocElement);
  }

  /**
   * Builds Redoc options with breakpoints read from host CSS custom properties
   * (--redoc-breakpoint-medium, --redoc-breakpoint-large), with fallbacks.
   */
  private getRedocOptions(): Record<string, unknown> {
    const style = this.element.nativeElement && window.getComputedStyle(this.element.nativeElement);
    const medium = style?.getPropertyValue('--redoc-breakpoint-medium')?.trim() || REDOC_BREAKPOINT_MEDIUM;
    const large = style?.getPropertyValue('--redoc-breakpoint-large')?.trim() || REDOC_BREAKPOINT_LARGE;
    return {
      hideDownloadButton: false,
      theme: {
        breakpoints: { medium, large },
      },
    };
  }
}
