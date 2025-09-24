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
import { ComponentSuggestionConfiguration } from '../models/componentSuggestionConfiguration';
import { cardSubtitleConfiguration } from './card/components/card-subtitle/gmd-card-subtitle.suggestions';
import { gridConfiguration } from './grid/grid.suggestions';
import { ComponentSelector } from '../models/componentSelector';
import { mdBlockConfiguration } from './block/gmd-md.suggestions';
import { cardTitleConfiguration } from './card/components/card-title/gmd-card-title.suggestions';
import { cardConfiguration } from './card/gmd-card.suggestions';
import { cellConfiguration } from './grid/cell/cell.suggestions';

export const componentSuggestionMap: Record<string, ComponentSuggestionConfiguration> = {
  [ComponentSelector.GRID]: gridConfiguration,
  [ComponentSelector.CELL]: cellConfiguration,
  [ComponentSelector.CARD]: cardConfiguration,
  [ComponentSelector.CARD_TITLE]: cardTitleConfiguration,
  [ComponentSelector.CARD_SUBTITLE]: cardSubtitleConfiguration,
  [ComponentSelector.MD_BLOCK]: mdBlockConfiguration,
};
