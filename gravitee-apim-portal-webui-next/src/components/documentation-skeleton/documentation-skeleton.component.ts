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

export type DocumentationSkeletonLineType = 'h1' | 'h2' | 'body';

export interface DocumentationSkeletonRow {
  type: DocumentationSkeletonLineType;
  widthPercent: number;
}

export const DEFAULT_DOCUMENTATION_SKELETON_ROWS: readonly DocumentationSkeletonRow[] = [
  { type: 'h1', widthPercent: 60 },
  { type: 'body', widthPercent: 65 },
  { type: 'h2', widthPercent: 55 },
  { type: 'body', widthPercent: 60 },
  { type: 'body', widthPercent: 55 },
  { type: 'body', widthPercent: 55 },
  { type: 'h2', widthPercent: 38 },
  { type: 'body', widthPercent: 55 },
  { type: 'body', widthPercent: 42 },
  { type: 'body', widthPercent: 50 },
];

@Component({
  selector: 'app-documentation-skeleton',
  templateUrl: './documentation-skeleton.component.html',
  styleUrl: './documentation-skeleton.component.scss',
  host: { class: 'documentation-skeleton' },
})
export class DocumentationSkeletonComponent {
  rows = input<readonly DocumentationSkeletonRow[]>(DEFAULT_DOCUMENTATION_SKELETON_ROWS);
  barStaggerMs = input(60);
}
