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

import { Component, EventEmitter, forwardRef, Input, Output } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { AutocompleteOptions, DisplayValueWithFn, GioFormTagsInputModule } from '@gravitee/ui-particles-angular';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';

import { ApplicationService } from '../../../../../../../../services-ngx/application.service';
import { MultiFilter } from '../../../../../runtime-logs/models';

@Component({
  selector: 'webhook-logs-applications-filter',
  templateUrl: './webhook-logs-applications-filter.component.html',
  styleUrls: ['./webhook-logs-applications-filter.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, MatFormFieldModule, GioFormTagsInputModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => WebhookLogsApplicationsFilterComponent),
    },
  ],
})
export class WebhookLogsApplicationsFilterComponent implements ControlValueAccessor {
  private _onChange: (value: string[]) => void = () => ({});
  private _onTouched: () => void = () => ({});
  private _selectedApplications: string[] = [];

  @Output() applicationCache: EventEmitter<MultiFilter> = new EventEmitter();
  private _applicationsCache: MultiFilter = [];

  public autocompleteOptions: (searchTerm: string) => Observable<AutocompleteOptions> = (searchTerm: string) => {
    return this.applicationService.list(undefined, searchTerm).pipe(
      map(apps => {
        return apps?.data?.map(application => {
          const data = {
            value: application.id,
            label: `${application.name} ( ${application.owner?.displayName} )`,
          };
          if (!this._applicationsCache.some(cache => cache.value === application.id)) {
            this._applicationsCache.push(data);
          }
          return data;
        });
      }),
      tap(() => {
        this.applicationCache.emit(this._applicationsCache);
      }),
    );
  };

  public displayValueWith: DisplayValueWithFn = (value: string) => {
    return this.applicationService.getById(value).pipe(
      map(application => {
        const label = `${application.name} ( ${application.owner?.displayName} )`;
        if (!this._applicationsCache.some(cache => cache.value === application.id)) {
          this._applicationsCache.push({ value: application.id, label });
          this.applicationCache.emit(this._applicationsCache);
        }
        return label;
      }),
      catchError(() => {
        return of(value);
      }),
    );
  };

  constructor(private readonly applicationService: ApplicationService) {}

  public registerOnChange(fn: any): void {
    this._onChange = fn;
  }

  public registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  public writeValue(value: string[]): void {
    this.selectedApplications = value;
  }

  public get selectedApplications(): string[] {
    return this._selectedApplications;
  }

  public set selectedApplications(value: string[]) {
    if (value !== this._selectedApplications) {
      this._selectedApplications = value;
      this._onChange(value);
    }
  }

  // From ControlValueAccessor interface
  setDisabledState(isDisabled: boolean): void {
    this._disabled = coerceBooleanProperty(isDisabled);
  }

  @Input()
  public get disabled(): boolean {
    return this._disabled;
  }

  public set disabled(dis: boolean) {
    this._disabled = coerceBooleanProperty(dis);
  }

  private _disabled = false;
}
