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
import { Component, Input } from '@angular/core';

import { ScoringAsset, ScoringDiagnostic, ScoringSeverity } from '../api-scoring.model';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

@Component({
  selector: 'app-api-scoring-list',
  templateUrl: './api-scoring-list.component.html',
  styleUrl: './api-scoring-list.component.scss',
  standalone: false,
})
export class ApiScoringListComponent {
  private _asset: ScoringAsset;
  protected readonly ScoringSeverity = ScoringSeverity;

  public displayedColumns: string[] = ['severity', 'location', 'recommendation', 'path'];
  public diagnosticsFiltered: ScoringDiagnostic[] = [];
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 5 },
    searchTerm: '',
    sort: { active: null, direction: null },
  };
  public totalDiagnostics: number;

  @Input()
  public get asset(): ScoringAsset {
    return this._asset;
  }
  set asset(value: ScoringAsset) {
    this._asset = value;
    this.runFilters(this.filters);
  }

  public runFilters(filters: GioTableWrapperFilters): void {
    const filtered = gioTableFilterCollection(this.asset.diagnostics, filters);
    this.diagnosticsFiltered = filtered.filteredCollection;
    this.totalDiagnostics = filtered.unpaginatedLength;
  }

  public openEditor(e: MouseEvent): void {
    e.stopPropagation();
  }
}
