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
import { Component, computed, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTooltip } from '@angular/material/tooltip';

import { MatTooltipOnEllipsisDirective } from '../../directives/mat-tooltip-on-ellipsis.directive';

@Component({
  selector: 'app-ai-workspace-card',
  standalone: true,
  imports: [MatCardModule, MatTooltip, MatTooltipOnEllipsisDirective],
  templateUrl: './ai-workspace-card.component.html',
  styleUrl: './ai-workspace-card.component.scss',
})
export class AiWorkspaceCardComponent {
  readonly workspaceId = input.required<string>();
  readonly title = input.required<string>();
  readonly description = input<string>();
  readonly budgetLabel = input<string>();

  readonly cardSelect = output<string>();

  protected readonly openLabel = computed(
    () => $localize`:@@aiWorkspaceCardOpen:Open AI workspace ${this.title()}:workspaceName:`,
  );
}
