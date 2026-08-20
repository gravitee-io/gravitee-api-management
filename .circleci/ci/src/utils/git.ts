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
import { spawn } from 'node:child_process';

/**
 * Returns the files / directories changed between 2 commits
 * @param from sha of the commit where to start
 * @param to sha of the commit where to stop. Choose HEAD if undefined
 * @return {string[]} Files and directories changed. It will only contain 1st level items (element on the root of the repository)
 */
export const changedFiles = async (from: string, to = 'HEAD'): Promise<string[]> => {
  return new Promise((resolve, reject) => {
    const args = diffArgs(from, to);

    console.log(`Running "git ${args.join(' ')}"`);
    const child = spawn('git', args);

    let stdout = '';
    let stderr = '';

    child.stdout.on('data', (data: Buffer) => {
      stdout += data.toString();
    });

    child.stderr.on('data', (data: Buffer) => {
      stderr += data.toString();
    });

    child.on('error', (err) => {
      reject(err);
    });

    // Waiting for `close` rather than resolving on the first `data`: a pipe hands over 64 KB at a
    // time, and a wide diff arrives in several chunks. Resolving on the first one dropped every
    // path after it — silently, and always the same ones, since git sorts them.
    child.on('close', (code) => {
      if (code !== 0) {
        reject(new Error(stderr.trim().length > 0 ? stderr.trim() : `git exited with code ${code}`));
        return;
      }
      resolve(toChangedPaths(stdout));
    });
  });
};

export const toChangedPaths = (stdout: string): string[] =>
  stdout
    .split('\n')
    .map(keepFirstPathItem)
    .filter(removeDuplicate)
    .filter((f) => f.length > 0);

// Three dots: diff from the merge base, so what the base branch gained since this branch started
// is not reported as a change of this branch.
const diffArgs = (from: string, to: string) => ['--no-pager', 'diff', '--name-only', `${from}...${to}`];
const keepFirstPathItem = (path: string) => path.split('/')[0];
const removeDuplicate = (path: string, index: number, arr: string[]) => arr.indexOf(path) === index;
