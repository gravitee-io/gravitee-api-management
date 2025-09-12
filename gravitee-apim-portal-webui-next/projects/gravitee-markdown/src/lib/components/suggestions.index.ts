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
import { ComponentSuggestion } from './grid/grid.suggestions';

export { ComponentSuggestion } from './grid/grid.suggestions';

// Export all component suggestions
export { gridSuggestions } from './grid/grid.suggestions';
export { cellSuggestions } from './grid/cell/cell.suggestions';

// Import suggestions for combination
import { gridSuggestions } from './grid/grid.suggestions';
import { cellSuggestions } from './grid/cell/cell.suggestions';

// Combined suggestions array
export const allComponentSuggestions: ComponentSuggestion[] = [
  ...gridSuggestions,
  ...cellSuggestions,
];
