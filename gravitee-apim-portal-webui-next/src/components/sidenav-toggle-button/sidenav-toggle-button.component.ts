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
import { Component, input, output } from '@angular/core';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'app-sidenav-toggle-button-component',
  template: `
    <button class="documentation-folder__sidenav__collapse-button" mat-button (click)="toggleState.emit()">
      @if (collapsed()) {
        <span class="material-icons icon-medium">keyboard_double_arrow_right</span>
      } @else {
        <span class="material-icons icon-medium">keyboard_double_arrow_left</span>
      }
    </button>
  `,
  styles: [
    `
      @use '../../scss/theme' as app-theme;
      @use '@angular/material' as mat;

      button {
        min-width: 48px;

        @include mat.button-overrides(
          (
            text-label-text-color: app-theme.$default-text-color,
            text-container-shape: 5px,
            text-with-icon-horizontal-padding: 0px,
          )
        );
      }
    `,
  ],
  imports: [MatButton],
})
export class SidenavToggleButtonComponent {
  collapsed = input.required<boolean>();
  toggleState = output<void>();
}
