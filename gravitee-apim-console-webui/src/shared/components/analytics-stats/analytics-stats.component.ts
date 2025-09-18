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
import { Component, input } from "@angular/core";
import { MatTooltip } from "@angular/material/tooltip";
import { MatIcon } from "@angular/material/icon";
import { DecimalPipe } from "@angular/common";

import { FormatDurationPipe } from "../../pipes/format-duration.pipe";
import { FormatNumberPipe } from "../../pipes/format-number.pipe";

export type StatsUnitType = 'ms';

export type StatsWidgetData = { stats: number; statsUnit: StatsUnitType };

@Component({
  selector: 'analytics-stats',
  imports: [FormatDurationPipe, FormatNumberPipe, MatTooltip, MatIcon, DecimalPipe],
  templateUrl: './analytics-stats.component.html',
  styleUrl: './analytics-stats.component.scss',
})
export class AnalyticsStatsComponent {
  input = input<StatsWidgetData>();
}
