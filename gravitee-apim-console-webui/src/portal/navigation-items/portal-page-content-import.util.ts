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
import { load } from 'js-yaml';
import { map, Observable } from 'rxjs';

import { PortalPageContentType } from '../../entities/management-api-v2';

export const IMPORTABLE_FILE_EXTENSIONS: readonly string[] = ['.md', '.yaml', '.yml', '.json'];

export const MAX_IMPORT_FILE_SIZE_MB = 10;
const MAX_IMPORT_FILE_SIZE_BYTES = MAX_IMPORT_FILE_SIZE_MB * 1024 * 1024;

export interface ImportedFileContent {
  content: string;
  contentType: PortalPageContentType;
}

export class ImportFileError extends Error {}

export function validateImportFile(file: File): string | null {
  const extension = extractExtension(file.name);
  if (!IMPORTABLE_FILE_EXTENSIONS.includes(extension)) {
    return `Unsupported file type "${extension}". Supported extensions: ${IMPORTABLE_FILE_EXTENSIONS.join(', ')}`;
  }
  if (file.size > MAX_IMPORT_FILE_SIZE_BYTES) {
    return `"${file.name}" exceeds the ${MAX_IMPORT_FILE_SIZE_MB} MB import limit`;
  }
  return null;
}

export function readImportedFile(file: File): Observable<ImportedFileContent> {
  return readFileAsText(file).pipe(
    map(content => {
      const contentType = detectContentType(file.name, content);
      if (!contentType) {
        throw new ImportFileError(
          `Cannot determine the API type of "${file.name}": the document needs a root "openapi", "swagger" or "asyncapi" property`,
        );
      }
      return { content, contentType };
    }),
  );
}

export function detectContentType(fileName: string, content: string): PortalPageContentType | null {
  if (fileName.toLowerCase().endsWith('.md')) {
    return 'GRAVITEE_MARKDOWN';
  }
  // .yaml/.yml/.json can hold either spec type: only the document's root property tells them apart
  let document: unknown;
  try {
    document = load(content);
  } catch {
    return null;
  }
  if (!(document instanceof Object)) {
    return null;
  }
  if ('asyncapi' in document) {
    return 'ASYNCAPI';
  }
  return 'openapi' in document || 'swagger' in document ? 'OPENAPI' : null;
}

export function extractTitleFromFileName(fileName: string): string {
  const extension = extractExtension(fileName);
  return fileName.toLowerCase().endsWith(extension) ? fileName.slice(0, -extension.length) : fileName;
}

function extractExtension(fileName: string): string {
  return fileName.includes('.') ? `.${fileName.split('.').pop()!.toLowerCase()}` : fileName;
}

function readFileAsText(file: File): Observable<string> {
  return new Observable<string>(subscriber => {
    const reader = new FileReader();
    reader.onload = () => {
      subscriber.next(reader.result as string);
      subscriber.complete();
    };
    reader.onerror = () => subscriber.error(reader.error);
    reader.readAsText(file);
    return () => reader.abort();
  });
}
