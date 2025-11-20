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
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { WebhookLogsMoreFiltersFormComponent } from './components/webhook-logs-more-filters-form/webhook-logs-more-filters-form.component';

import { DEFAULT_PERIOD } from '../../../runtime-logs/models';
import { WebhookMoreFiltersForm } from '../../models/webhook-logs.models';

@Component({
  selector: 'webhook-logs-more-filters',
  templateUrl: './webhook-logs-more-filters.component.html',
  styleUrls: ['./webhook-logs-more-filters.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, GioIconsModule, WebhookLogsMoreFiltersFormComponent],
})
export class WebhookLogsMoreFiltersComponent implements OnInit, OnChanges {
  @Output() closeMoreFiltersEvent = new EventEmitter<void>();
  @Output() applyMoreFiltersEvent = new EventEmitter<WebhookMoreFiltersForm>();
  @Input() showMoreFilters = false;
  @Input() formValues: WebhookMoreFiltersForm = { period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] };
  @Input() callbackUrls: string[] = [];

  moreFiltersFormControl: FormControl<WebhookMoreFiltersForm>;
  isInvalid = false;

  constructor(private readonly destroyRef: DestroyRef) {
    this.moreFiltersFormControl = new FormControl<WebhookMoreFiltersForm>(
      { period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] },
      { nonNullable: true },
    );
  }

  ngOnInit(): void {
    this.isInvalid = this.moreFiltersFormControl.invalid;

    this.moreFiltersFormControl.statusChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.isInvalid = this.moreFiltersFormControl.invalid;
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.formValues && this.moreFiltersFormControl) {
      this.updateFormFromInput(this.formValues);
    }
  }

  private updateFormFromInput(formValues: WebhookMoreFiltersForm): void {
    if (this.moreFiltersFormControl) {
      const values: WebhookMoreFiltersForm = {
        period: formValues?.period ?? DEFAULT_PERIOD,
        from: formValues?.from ?? null,
        to: formValues?.to ?? null,
        callbackUrls: formValues?.callbackUrls ?? [],
      };
      this.moreFiltersFormControl.patchValue(values, { emitEvent: false });
      this.isInvalid = this.moreFiltersFormControl.invalid;
    }
  }

  resetMoreFilters(): void {
    this.moreFiltersFormControl.patchValue({ period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] });
    this.apply();
  }

  close(): void {
    this.closeMoreFiltersEvent.emit();
  }

  apply(): void {
    this.applyMoreFiltersEvent.emit(this.moreFiltersFormControl.value);
    this.close();
  }
}
