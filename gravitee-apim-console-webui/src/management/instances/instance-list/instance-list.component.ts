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
import { distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { BehaviorSubject, Subject } from 'rxjs';
import { isEqual } from 'lodash';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { InstanceService } from '../../../services-ngx/instance.service';

type TableData = {
  id: string;
  hostname: string;
  ip: string;
  port: string;
  state: 'STARTED' | 'STOPPED' | 'UNKNOWN';
  version: string;
  os: string;
  tags: string;
  tenant: string;
  lastHeartbeat: Date;
};

@Component({
  selector: 'instance-list',
  templateUrl: './instance-list.component.html',
  styleUrls: ['./instance-list.component.scss'],
  standalone: false,
})
export class InstanceListComponent implements OnInit, OnDestroy {
  displayedColumns = ['hostname', 'version', 'state', 'lastHeartbeat', 'os', 'ip-port', 'tenant', 'tags'];
  filteredTableData: TableData[] = [];
  nbTotalInstances = 0;

  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private readonly unsubscribe$ = new Subject<void>();

  constructor(private readonly instanceService: InstanceService) {}

  ngOnInit(): void {
    this.filters$
      .pipe(
        distinctUntilChanged(isEqual),
        switchMap((filters: GioTableWrapperFilters) =>
          this.instanceService.search(true, 0, 0, filters.pagination.index - 1, filters.pagination.size),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((searchResult) => {
        this.nbTotalInstances = searchResult.totalElements;
        this.filteredTableData = searchResult.content.map((instance) => ({
          id: instance.event,
          hostname: instance.hostname,
          state: instance.state,
          // Instance version is like "4.2.0-SNAPSHOT (build: 508664) revision#26c06dbf46547447c420f8683d62ada5c1e15617"
          // so keep only the version number for this screen
          version: instance.version.substring(0, instance.version.indexOf('(')),
          ip: instance.ip,
          port: instance.port,
          os: instance.operating_system_name,
          tags: (instance.tags ?? []).join(', '),
          tenant: instance.tenant ?? '',
          lastHeartbeat: new Date(instance.last_heartbeat_at),
        }));
      });
  }
  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }
}
