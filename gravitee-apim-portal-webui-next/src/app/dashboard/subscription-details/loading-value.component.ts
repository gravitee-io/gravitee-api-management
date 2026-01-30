  /*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { Component, input } from '@angular/core';

@Component({
  selector: '[appLoadingValue]',
  standalone: true,
  template: `
    @if (value()) {
      <ng-content></ng-content>
    } @else {
      <span class="skeleton-label"></span>
    }
  `,
  styles: [
    `
      .skeleton-label {
        display: inline-block;
        height: 1em;
        width: 100%;
        max-width: 300px; // TODO use a relative limit
        border-radius: 4px;

        // The Shimmer Effect
        background: #eee;
        background: linear-gradient(
            110deg,
            #ececec 8%,
            #f5f5f5 18%,
            #ececec 33%
        );
        background-size: 200% 100%;
        animation: shimmer 1.5s linear infinite;
      }

      @keyframes shimmer {
        to {
          background-position-x: -200%;
        }
      }
    `,
  ],
})
export class LoadingValueComponent {
  value = input<unknown>(null, { alias: 'appLoadingValue' });
}
