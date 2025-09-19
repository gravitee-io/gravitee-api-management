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

const basicBlock: ComponentSuggestion = {
  label: 'GmdBlock - Basic',
  insertText: `<gmd-block>
	$1
</gmd-block>`,
  detail: 'Component template',
};

const blockWithContent: ComponentSuggestion = {
  label: 'GmdBlock - With Content',
  insertText: `<gmd-block>
	# Your Title Here
	
	Some **markdown** content with *formatting*.
	
	- List item 1
	- List item 2
	- List item 3
	
	> This is a blockquote
</gmd-block>`,
  detail: 'Component template with example content',
};

const styledBlock: ComponentSuggestion = {
  label: 'GmdBlock - Styled',
  insertText: `<gmd-block>
	<div style="background: #f8f9fa; padding: 16px; border-radius: 8px;">
		# Styled Block
		
		This block has custom styling applied.
		
		- Feature 1
		- Feature 2
		- Feature 3
	</div>
</gmd-block>`,
  detail: 'Component template with styling',
};

export const gmdBlockConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [basicBlock, blockWithContent, styledBlock],
  attributeSuggestions: [],
  hoverDocumentation: {
    label: 'GmdBlock',
    description: 'A simple block component that projects content. The markdown service handles preprocessing of the content.',
  },
  attributeHoverDocumentation: {},
};
