/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { ApplicationMetadataFormat } from '../../types/applicationNotification';

export const METADATA_FORMATS: ApplicationMetadataFormat[] = ['STRING', 'NUMERIC', 'BOOLEAN', 'DATE', 'MAIL', 'URL'];
export const METADATA_PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
export const DEFAULT_METADATA_PAGE_SIZE = 10;
