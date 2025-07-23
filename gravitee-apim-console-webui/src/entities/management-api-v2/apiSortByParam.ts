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
export type ApiSortByParam =
  | 'name'
  | '-name'
  | 'api_type'
  | '-api_type'
  | 'status'
  | '-status'
  | 'paths'
  | '-paths'
  | 'tags_asc'
  | '-tags_desc'
  | 'categories_asc'
  | '-categories_desc'
  | 'owner'
  | '-owner'
  | 'visibility'
  | '-visibility';

export function apiSortByParamFromString(sort: string): ApiSortByParam {
  if (!sort) {
    return undefined;
  }
  const desc = sort.startsWith('-');
  if (sort.endsWith('name')) {
    return desc ? '-name' : 'name';
  }
  if (sort.endsWith('apiType')) {
    return desc ? '-api_type' : 'api_type';
  }
  if (sort.endsWith('states')) {
    return desc ? '-status' : 'status';
  }
  if (sort.endsWith('access')) {
    return desc ? '-paths' : 'paths';
  }
  if (sort.endsWith('tags')) {
    return desc ? '-tags_desc' : 'tags_asc';
  }
  if (sort.endsWith('categories')) {
    return desc ? '-categories_desc' : 'categories_asc';
  }
  if (sort.endsWith('owner')) {
    return desc ? '-owner' : 'owner';
  }
  if (sort.endsWith('visibility')) {
    return desc ? '-visibility' : 'visibility';
  }
  return desc ? '-name' : 'name';
}
