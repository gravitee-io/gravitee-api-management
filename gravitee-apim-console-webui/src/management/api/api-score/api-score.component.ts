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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ApiScore } from './api-score.model';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api } from '../../../entities/management-api-v2';
import { ApiScoreService } from '../../../services-ngx/api-score.service';

@Component({
  selector: 'app-api-score',
  templateUrl: './api-score.component.html',
  styleUrl: './api-score.component.scss',
})
export class ApiScoreComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public status = 'all';
  public isLoading = false;
  public apiScore: ApiScore;
  public api: Api;

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly apiScoreService: ApiScoreService,
  ) {}

  ngOnInit() {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (api) => {
          this.api = api;
        },
      });

    this.apiScoreService
      .getAllClear()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (apiScore) => {
          this.apiScore = apiScore;
        },
      });
  }

  public evaluate() {
    this.apiScoreService.evaluate().subscribe();
  }
}
