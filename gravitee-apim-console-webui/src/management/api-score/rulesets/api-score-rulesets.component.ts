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

import { Component, OnInit } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { RulesetV2Service } from '../../../services-ngx/ruleset-v2.service';
import { ScoringRulesetsResponse } from '../../../entities/management-api-v2/api/v4/ruleset';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-api-score-rulesets',
  templateUrl: './api-score-rulesets.component.html',
  styleUrl: './api-score-rulesets.component.scss',
})
export class ApiScoreRulesetsComponent implements OnInit {
  rulesets$: Observable<ScoringRulesetsResponse>;

  constructor(
    private readonly rulesetV2Service: RulesetV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.rulesets$ = this.rulesetV2Service.listRulesets().pipe(
      catchError(({ error }) => {
        this.snackBarService.error(error.message);
        return EMPTY;
      }),
    );
  }
}
