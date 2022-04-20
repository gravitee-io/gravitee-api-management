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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormGroup, FormControl } from '@angular/forms';
import { mapValues } from 'lodash';
import { BehaviorSubject, Subject } from 'rxjs';
import { distinctUntilChanged, switchMap, takeUntil, throttleTime } from 'rxjs/operators';
import { ApiService } from '../../../services-ngx/api.service';
import { AuditService } from '../../../services-ngx/audit.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

interface AuditDataTable {
  id: string;
  date: number;
  user: string;
  referenceType: string;
  reference: string;
  event: string;
  targets: Record<string, string>;
  patch: unknown;
  displayPatch: boolean;
}

@Component({
  selector: 'org-settings-audit',
  template: require('./org-settings-audit.component.html'),
  styles: [require('./org-settings-audit.component.scss')],
})
export class OrgSettingsAuditComponent implements OnInit, OnDestroy {
  public displayedColumns = ['date', 'user', 'referenceType', 'reference', 'event', 'targets', 'patch'];
  public filteredTableData: AuditDataTable[] = [];
  public nbTotalAudit = 0;

  public filtersForm = new FormGroup({
    event: new FormControl(),
  });

  public range = new FormGroup({
    start: new FormControl(),
    end: new FormControl(),
  });

  public eventsName$ = this.auditService.getAllEventsNameByOrganization();

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  // Create filters stream
  private filtersStream = new BehaviorSubject<GioTableWrapperFilters & { event?: string }>({
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  });

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.filtersForm.valueChanges.subscribe(({ event }) => {
      this.filtersStream.next({ ...this.filtersStream.value, event });
    });

    this.filtersStream
      .pipe(
        takeUntil(this.unsubscribe$),
        throttleTime(100),
        distinctUntilChanged(),
        switchMap(({ pagination, event }) => this.auditService.listByOrganization({ event }, pagination.index, pagination.size)),
      )
      .subscribe((auditsPage) => {
        this.nbTotalAudit = auditsPage.totalElements;
        this.filteredTableData = (auditsPage.content ?? []).map((audit) => ({
          id: audit.id,
          date: audit.createdAt,
          user: (auditsPage.metadata[`USER:${audit.user}:name`] as string) ?? audit.user,
          referenceType: audit.referenceType,
          reference: (auditsPage.metadata[`${audit.referenceType}:${audit.referenceId}:name`] as string) ?? audit.user,
          event: audit.event,
          targets: mapValues(audit.properties, (v, k) => auditsPage.metadata[k + ':' + v + ':name'] as string),
          patch: JSON.parse(audit.patch),
          displayPatch: false,
        }));
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filtersStream.next(filters);
  }
}
