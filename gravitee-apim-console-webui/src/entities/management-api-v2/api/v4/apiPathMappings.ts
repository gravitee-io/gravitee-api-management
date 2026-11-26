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
import { ApiV4 } from './apiV4';
import { HttpListener } from './httpListener';

export interface ApiPathMappings {
  pathMappings: string[];
}

export interface ImportApiPathMappings {
  pageId: string;
  overwrite?: boolean;
}

export function firstHttpListener(api: ApiV4 | undefined): HttpListener | undefined {
  return api?.listeners?.find((listener): listener is HttpListener => listener.type === 'HTTP');
}

export function storedPathMappings(api: ApiV4 | undefined): string[] {
  const listener = firstHttpListener(api);
  return listener?.pathMappings ? [...listener.pathMappings] : [];
}

export function hasPathMappings(api: ApiV4 | undefined): boolean {
  return storedPathMappings(api).length > 0;
}
