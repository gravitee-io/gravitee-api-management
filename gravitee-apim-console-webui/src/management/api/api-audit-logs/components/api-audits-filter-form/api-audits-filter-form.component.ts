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
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { isEqual } from 'lodash';
import { Moment } from 'moment';

import { endOfDay } from '../../../../../util/date.util';

export interface ApiAuditFilter {
  events?: string[];
  from?: number;
  to?: number;
}

@Component({
  selector: 'api-audits-filter-form',
  templateUrl: './api-audits-filter-form.component.html',
  styleUrls: ['./api-audits-filter-form.component.scss'],
})
export class ApiAuditsFilterFormComponent implements OnInit, OnDestroy {
  @Input()
  public loading: boolean;

  @Input()
  public events: string[];

  @Output()
  public filtersChange = new EventEmitter<ApiAuditFilter>();

  protected filtersForm = new FormGroup({
    events: new FormControl<string[]>([]),
    range: new FormGroup({
      start: new FormControl<Moment>(null),
      end: new FormControl<Moment>(null),
    }),
  });

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  ngOnInit(): void {
    this.filtersForm.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$))
      .subscribe(({ events, range }) => {
        this.filtersChange.emit({
          events,
          from: range?.start?.valueOf() ?? null,
          to: endOfDay(range?.end) ?? null,
        });
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
