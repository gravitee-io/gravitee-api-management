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
import { CdkAccordionModule } from '@angular/cdk/accordion';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GioBannerModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';

import { EndpointHealthCheckConditionComponent } from './endpoint-health-check-condition/endpoint-health-check-condition.component';
import { RequestMetricsRateConditionComponent } from './request-metrics-rate-condition/request-metrics-rate-condition.component';
import { RequestMetricsAggregationConditionComponent } from './request-metrics-aggregation-condition/request-metrics-aggregation-condition.component';
import {
  AggregationConditionComponent,
  CompareConditionComponent,
  StringConditionComponent,
  ThresholdConditionComponent,
  ThresholdRangeConditionComponent,
} from './components';
import { MetricsSimpleConditionComponent } from './metrics-simple-condition';
import { MissingDataConditionComponent } from './missing-data-condition/missing-data-condition.component';

@NgModule({
  declarations: [
    MissingDataConditionComponent,
    MetricsSimpleConditionComponent,
    CompareConditionComponent,
    ThresholdConditionComponent,
    ThresholdRangeConditionComponent,
    StringConditionComponent,
    RequestMetricsAggregationConditionComponent,
    RequestMetricsRateConditionComponent,
    EndpointHealthCheckConditionComponent,
    AggregationConditionComponent,
  ],
  exports: [
    MissingDataConditionComponent,
    MetricsSimpleConditionComponent,
    CompareConditionComponent,
    ThresholdConditionComponent,
    ThresholdRangeConditionComponent,
    StringConditionComponent,
    RequestMetricsAggregationConditionComponent,
    RequestMetricsRateConditionComponent,
    EndpointHealthCheckConditionComponent,
    AggregationConditionComponent,
  ],
  imports: [
    CommonModule,
    CdkAccordionModule,
    FormsModule,
    GioBannerModule,
    GioIconsModule,
    MatInputModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTooltipModule,
    ReactiveFormsModule,
  ],
})
export class RuntimeAlertCreateConditionModule {}
