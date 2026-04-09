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
import { Component, input } from '@angular/core';

export interface SidenavSkeletonRow {
  depth: number;
  widthPercent: number;
}

export const DEFAULT_SIDENAV_SKELETON_ROWS: readonly SidenavSkeletonRow[] = [
  { depth: 0, widthPercent: 75 },
  { depth: 1, widthPercent: 76 },
  { depth: 1, widthPercent: 85 },
  { depth: 0, widthPercent: 75 },
  { depth: 1, widthPercent: 80 },
  { depth: 2, widthPercent: 65 },
  { depth: 2, widthPercent: 60 },
  { depth: 1, widthPercent: 72 },
  { depth: 1, widthPercent: 68 },
  { depth: 0, widthPercent: 60 },
];

@Component({
  selector: 'app-sidenav-skeleton',
  templateUrl: './sidenav-skeleton.component.html',
  styleUrl: './sidenav-skeleton.component.scss',
  host: { class: 'sidenav-skeleton' },
})
export class SidenavSkeletonComponent {
  rows = input<readonly SidenavSkeletonRow[]>(DEFAULT_SIDENAV_SKELETON_ROWS);
  barStaggerMs = input(80);
}
