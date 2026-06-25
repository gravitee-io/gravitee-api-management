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
import { AbstractControl } from '@angular/forms';

export interface ScheduleLimitError {
  technicalCode?: string;
  message?: string;
  parameters?: {
    field?: string;
    schedule?: string;
    minimumInterval?: string;
  };
}

export const formatScheduleInterval = (milliseconds: number): string => {
  const units = [
    { milliseconds: 86_400_000, label: 'day' },
    { milliseconds: 3_600_000, label: 'hour' },
    { milliseconds: 60_000, label: 'minute' },
    { milliseconds: 1_000, label: 'second' },
  ];
  const unit = units.find(candidate => milliseconds >= candidate.milliseconds && milliseconds % candidate.milliseconds === 0);
  if (!unit) {
    return `${milliseconds} milliseconds`;
  }
  const value = milliseconds / unit.milliseconds;
  return `${value} ${unit.label}${value === 1 ? '' : 's'}`;
};

export const getMinimumIntervalError = (error: ScheduleLimitError): string | null => {
  if (error.technicalCode !== 'schedule.minimumIntervalExceeded') {
    return null;
  }
  const minimumInterval = Number(error.parameters?.minimumInterval);
  return Number.isFinite(minimumInterval)
    ? `Schedule must not run more frequently than every ${formatScheduleInterval(minimumInterval)}`
    : (error.message ?? 'Schedule exceeds the minimum interval configured by your administrator.');
};

export const applyMinimumIntervalError = (error: ScheduleLimitError, control: AbstractControl): boolean => {
  const message = getMinimumIntervalError(error);
  if (!message) {
    return false;
  }
  control.setErrors({ ...control.errors, minimumInterval: message });
  return true;
};

export const getMinimumIntervalHint = (milliseconds: number): string | null =>
  milliseconds > 0 ? `Minimum interval configured by your administrator: ${formatScheduleInterval(milliseconds)}` : null;
