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
import { AsyncPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, Router } from '@angular/router';
import { map, Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApisListComponent } from '../components/apis-list/apis-list.component';

@Component({
  selector: 'app-tabs-view',
  standalone: true,
  imports: [AsyncPipe, MatCardModule, ApisListComponent],
  templateUrl: './tabs-view.component.html',
  styleUrl: './tabs-view.component.scss',
})
export class TabsViewComponent implements OnInit {
  filterAndQuery$: Observable<{ query: string }> = of();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit() {
    this.filterAndQuery$ = this.route.queryParams.pipe(
      map(queryParams => ({
        query: queryParams['query'] ?? '',
      })),
    );
  }

  onSearchResults(searchInput: string) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        query: searchInput,
      },
    });
  }
}
