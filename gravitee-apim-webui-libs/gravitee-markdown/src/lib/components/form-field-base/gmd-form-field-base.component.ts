/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Directive, effect, inject, OnDestroy } from '@angular/core';
import { uniqueId } from 'lodash';

import { GmdFieldState } from '../../models/formField';
import { GMD_FORM_STATE_STORE } from '../../services/gmd-form-state.store';

/**
 * Abstract base class for GMD form field components.
 * Provides a common store management lifecycle and state synchronization.
 *
 * Child components must implement:
 * - trackProperties(): Track reactive values that trigger store updates
 * - buildFieldState(): Create the GmdFieldState object specific to the field type
 * - isDisabled(): Return whether the field is currently disabled
 */
@Directive()
export abstract class GmdFormFieldBase implements OnDestroy {
  protected readonly store = inject(GMD_FORM_STATE_STORE);
  protected readonly id: string = uniqueId();

  // Setup reactive effect to update store whenever dependencies change
  private readonly storeSync = effect(() => {
    // Track all reactive properties
    this.trackProperties();
    this.updateStore();
  });

  ngOnDestroy(): void {
    this.store.removeField(this.id);
  }

  /**
   * Update the store with the current field state.
   * Automatically removes a field from the store if disabled.
   */
  protected updateStore(): void {
    if (this.isDisabled()) {
      this.store.removeField(this.id);
      return;
    }

    const state = this.buildFieldState();
    this.store.updateField(state);
  }

  /**
   * Track reactive properties that should trigger store updates.
   * Call each reactive signal/computed to register them as dependencies in the effect.
   */
  protected abstract trackProperties(): void;

  /**
   * Build the GmdFieldState object for this specific field type.
   */
  protected abstract buildFieldState(): GmdFieldState;

  /**
   * Return whether this field is currently disabled.
   */
  protected abstract isDisabled(): boolean;
}
