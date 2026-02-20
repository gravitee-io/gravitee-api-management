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
export function sanitizeBranch(branch: string) {
  return branch
    .replaceAll(/[~^]+/g, '')
    .replaceAll(/[^a-zA-Z0-9\\.]+/g, '-')
    .replaceAll(/^-+|-+$/g, '')
    .toLowerCase()
    .substring(0, 60);
}

export function isE2EBranch(branch: string): boolean {
  const regex = /.*-run-e2e.*/;
  return regex.test(branch);
}

export function isMasterBranch(branch: string): boolean {
  return branch === 'master';
}

export function isSupportBranch(branch: string): boolean {
  const regex = /^\d+\.\d+\.x$/;
  return regex.test(branch);
}

export function isAlphaVertx5Branch(branch: string) {
  return branch === 'alpha-vertx5';
}

export function isSupportBranchOrMasterOrAlphaVertx5(branch: string): boolean {
  return isMasterBranch(branch) || isSupportBranch(branch) || isAlphaVertx5Branch(branch);
}
