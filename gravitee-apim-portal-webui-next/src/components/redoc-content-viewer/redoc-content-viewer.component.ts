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

import { RedocService } from '../../services/redoc.service';

const REDOC_OPTIONS = {
  hideDownloadButton: false,
  theme: {
    breakpoints: {
      medium: '50rem',
      large: '75rem',
    },
  },
};

@Component({
  selector: 'app-redoc-content-viewer',
  imports: [],
  templateUrl: './redoc-content-viewer.component.html',
  styleUrl: './redoc-content-viewer.component.scss',
})
export class RedocContentViewerComponent {
  content = input.required<string>();

  private readonly destroyRef = inject(DestroyRef);
  private pendingTimeoutId: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private element: ElementRef,
    private redocService: RedocService,
  ) {
    effect(onCleanup => {
      const spec = this.content();
      this.pendingTimeoutId = setTimeout(() => {
        this.pendingTimeoutId = null;
        const redocElement = this.element.nativeElement?.querySelector('#redoc') as HTMLElement | null;
        if (redocElement && spec) {
          redocElement.innerHTML = '';
          this.redocService.init(spec, REDOC_OPTIONS, redocElement);
        }
      }, 0);

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
}
