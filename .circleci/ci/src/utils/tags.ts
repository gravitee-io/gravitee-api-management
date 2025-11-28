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
import { sanitizeBranch } from './branch';

/**
 * Computes a Docker tag suffix based on the provided SHA1 hash.
 * If a SHA1 hash is provided, the suffix will be the first 7 characters of the hash.
 * If no SHA1 hash is provided, the suffix will default to 'latest'.
 *
 * @param {string} [sha1] The optional SHA1 hash used to generate the tag suffix.
 * @return {string} The computed Docker tag suffix.
 */
export function computeDockerTagSuffix(sha1?: string): string {
  return sha1 ? `${sha1.substring(0, 7)}` : 'latest';
}

export function computeImagesTag(branch: string, sha1?: string): string {
  return `${sanitizeBranch(branch)}-${computeDockerTagSuffix(sha1)}`;
}
