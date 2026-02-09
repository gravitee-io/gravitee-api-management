/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ComponentSuggestion } from '../../models/componentSuggestion';
import { ComponentSuggestionConfiguration } from '../../models/componentSuggestionConfiguration';

const grid1Column: ComponentSuggestion = {
  label: 'Grid - 1 column',
  insertText: `<gmd-grid columns="1">
	<gmd-cell>
		$1
	</gmd-cell>
</gmd-grid>`,
  detail: 'Component template',
};

const grid2Columns: ComponentSuggestion = {
  label: 'Grid - 2 columns',
  insertText: `<gmd-grid columns="2">
	<gmd-cell>
		$1
	</gmd-cell>
	<gmd-cell>
		$2
	</gmd-cell>
</gmd-grid>`,
  detail: 'Component template',
};

const grid3Columns: ComponentSuggestion = {
  label: 'Grid - 3 columns',
  insertText: `<gmd-grid columns="3">
	<gmd-cell>
		$1
	</gmd-cell>
	<gmd-cell>
		$2
	</gmd-cell>
	<gmd-cell>
		$3
	</gmd-cell>
</gmd-grid>`,
  detail: 'Component template',
};

const grid4Columns: ComponentSuggestion = {
  label: 'Grid - 4 columns',
  insertText: `<gmd-grid columns="4">
	<gmd-cell>
		$1
	</gmd-cell>
	<gmd-cell>
		$2
	</gmd-cell>
	<gmd-cell>
		$3
	</gmd-cell>
	<gmd-cell>
		$4
	</gmd-cell>
</gmd-grid>`,
  detail: 'Component template',
};

const grid5Columns: ComponentSuggestion = {
  label: 'Grid - 5 columns',
  insertText: `<gmd-grid columns="5">
	<gmd-cell>
		$1
	</gmd-cell>
	<gmd-cell>
		$2
	</gmd-cell>
	<gmd-cell>
		$3
	</gmd-cell>
	<gmd-cell>
		$4
	</gmd-cell>
	<gmd-cell>
		$5
	</gmd-cell>
</gmd-grid>`,
  detail: 'Component template',
};

const grid6Columns: ComponentSuggestion = {
  label: 'Grid - 6 columns',
  insertText: `<gmd-grid columns="6">
	<gmd-cell>
		$1
	</gmd-cell>
	<gmd-cell>
		$2
	</gmd-cell>
	<gmd-cell>
		$3
	</gmd-cell>
	<gmd-cell>
		$4
	</gmd-cell>
	<gmd-cell>
		$5
	</gmd-cell>
	<gmd-cell>
		$6
	</gmd-cell>
</gmd-grid>`,
  detail: 'Component template',
};

const emptyGrid2Columns: ComponentSuggestion = {
  label: 'Grid - Empty 2 columns',
  insertText: `<gmd-grid columns="2">
	$1
</gmd-grid>`,
  detail: 'Component template',
};

const grid2Columns2Rows: ComponentSuggestion = {
  label: 'Grid - 2 columns 2 rows',
  insertText: `<gmd-grid columns="2">
	<gmd-cell>
		$1
	</gmd-cell>
	<gmd-cell>
		$2
	</gmd-cell>
	<gmd-cell>
		$3
	</gmd-cell>
	<gmd-cell>
		$4
	</gmd-cell>
</gmd-grid>`,
  detail: 'Component template',
};

export const gridConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [grid1Column, grid2Columns, grid3Columns, grid4Columns, grid5Columns, grid6Columns, emptyGrid2Columns, grid2Columns2Rows],
  attributeSuggestions: [
    {
      label: 'columns="1"',
      insertText: 'columns="1"',
      detail: 'Number of columns to display (1-6)',
    },
  ],
  hoverDocumentation: {
    label: 'Grid',
    description: 'Layout component for organizing content in columns and rows',
  },
  attributeHoverDocumentation: {
    columns: 'Number of columns to display (1-6)',
  },
};
