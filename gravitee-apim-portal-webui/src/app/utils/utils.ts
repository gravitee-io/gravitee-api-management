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

export interface Header {
  name: string;
  value: string;
}

export function createPromiseList(size) {
  const deferredList = [];
  const list = new Array(size).fill(null).map(() => new Promise((resolve, reject) => deferredList.push({ resolve, reject })));
  return { list, deferredList };
}

export function formatCurlCommandLine(url: string, ...headers: Header[]): string {
  const headersFormatted = headers
    // keep the line break
    .reduce(
      (acc, header) =>
        acc +
        `--header "${header.name}: ${header.value}" \\
     `,
      ' ',
    );

  return `curl${headersFormatted}${url}`;
}

export function formatOpenSslCommandLine(url: string): string {
  return `openssl s_client -connect ${url}`;
}
