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
import { ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { NgClass } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

type TimeframeOption = {
  id: '5m' | '1h' | '24h' | '7d' | '30d';
  label: string;
  durationMs: number;
};

const TIMEFRAME_OPTIONS: TimeframeOption[] = [
  { id: '5m', label: 'Last 5 min', durationMs: 5 * 60 * 1000 },
  { id: '1h', label: 'Last 1 hour', durationMs: 60 * 60 * 1000 },
  { id: '24h', label: 'Last 24 hours', durationMs: 24 * 60 * 60 * 1000 },
  { id: '7d', label: 'Last 7 days', durationMs: 7 * 24 * 60 * 60 * 1000 },
  { id: '30d', label: 'Last 30 days', durationMs: 30 * 24 * 60 * 60 * 1000 },
];

@Component({
  selector: 'analytics-timeframe',
  imports: [MatButtonModule, NgClass],
  templateUrl: './analytics-timeframe.component.html',
  styleUrl: './analytics-timeframe.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnalyticsTimeframeComponent implements OnInit {
  @Output()
  timeframeChange = new EventEmitter<{ from: number; to: number }>();

  protected readonly timeframeOptions = TIMEFRAME_OPTIONS;
  protected selectedTimeframeId: TimeframeOption['id'] = '1h';

  ngOnInit(): void {
    this.emitSelection(this.selectedTimeframeId);
  }

  protected onSelect(optionId: TimeframeOption['id']): void {
    if (this.selectedTimeframeId === optionId) {
      return;
    }

    this.selectedTimeframeId = optionId;
    this.emitSelection(optionId);
  }

  private emitSelection(optionId: TimeframeOption['id']): void {
    const selectedOption = this.timeframeOptions.find((option) => option.id === optionId);
    if (!selectedOption) {
      return;
    }

    const to = Date.now();
    const from = to - selectedOption.durationMs;
    this.timeframeChange.emit({ from, to });
  }
}
