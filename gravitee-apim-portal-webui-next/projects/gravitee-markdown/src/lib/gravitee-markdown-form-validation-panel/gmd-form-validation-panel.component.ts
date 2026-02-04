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
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { GMD_FORM_STATE_STORE } from '../services/gmd-form-state.store';

/**
 * Validation panel component that displays form diagnostics and validation status.
 * Injects the form state store from parent scope (gmd-form-host).
 *
 * Displays:
 * - Configuration errors (critical)
 * - Configuration warnings (auto-corrected)
 * - Form validity status
 * - Invalid fields list
 * - Field values (debug)
 */
@Component({
  selector: 'gmd-form-validation-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './gmd-form-validation-panel.component.html',
  styleUrl: './gmd-form-validation-panel.component.scss',
})
export class GmdFormValidationPanelComponent {
  protected readonly store = inject(GMD_FORM_STATE_STORE);

  /**
   * Formats field value for display.
   * @param value - The field value to format
   * @returns Formatted value or '(empty)' if value is empty
   */
  formatValue(value: string): string {
    return value.trim().length === 0 ? '(empty)' : value;
  }
}
