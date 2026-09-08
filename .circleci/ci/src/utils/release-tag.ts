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
/**
 * The core reactor releases under a prefixed tag. The product keeps the bare version — users know
 * "APIM 4.13.0" as a set of images and a bundle, produced by the distribution — so only this lane's
 * tags carry a prefix, and no existing consumer of the version has to change.
 */
export const CORE_TAG_PATTERN = '^core_(\\d+\\.\\d+\\.\\d+(-[a-z]+\\.\\d+)?)$';

/**
 * The same pattern, as CircleCI wants it in a job filter. A workflow generated for a tag has to
 * repeat the filter its jobs run under: the continued configuration is a pipeline of its own, and
 * CircleCI runs no job for a tag unless the job says so.
 */
export const CORE_TAG_FILTER = `/${CORE_TAG_PATTERN}/`;

const CORE_TAG = new RegExp(CORE_TAG_PATTERN);

/**
 * The version a core release tag carries, or undefined when the tag is not one.
 *
 * @param {string} tag the tag that started the pipeline, empty on a branch build
 * @return {string | undefined} the version to publish
 */
export function coreVersionFromTag(tag: string): string | undefined {
  return CORE_TAG.exec(tag)?.[1];
}
