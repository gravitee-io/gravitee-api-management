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
import { CdkTrapFocus } from '@angular/cdk/a11y';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

import { MobileClassDirective } from '../../directives/mobile-class.directive';

@Component({
  selector: 'app-side-panel',
  imports: [CdkTrapFocus, MatButtonModule, MobileClassDirective],
  templateUrl: './side-panel.component.html',
  styleUrl: './side-panel.component.scss',
})
export class SidePanelComponent {
  panelTestId = input.required<string>();
  ariaLabel = input.required<string>();

  closed = output<void>();
}
