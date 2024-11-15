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
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ApiScoreRoutingModule } from './api-score-routing.module';
import { ApiScoreDashboardModule } from './dashboard/api-score-dashboard.module';
import { ApiScoreNavigationComponent } from './navigation/api-score-navigation.component';
import { ApiScoreRulesetsModule } from './rulesets/api-score-rulesets.module';

import { ApiScoringService } from '../../services-ngx/api-scoring.service';

@NgModule({
  declarations: [],
  imports: [CommonModule, ApiScoreRoutingModule, ApiScoreDashboardModule, ApiScoreRulesetsModule, ApiScoreNavigationComponent],
  providers: [ApiScoringService],
})
export class ApiScoreModule {}
