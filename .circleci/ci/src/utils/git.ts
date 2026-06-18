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
import { spawn, spawnSync } from 'node:child_process';

/**
 * Returns the files / directories changed between 2 commits
 * @param from sha of the commit where to start
 * @param to sha of the commit where to stop. Choose HEAD if undefined
 * @return {string[]} Files and directories changed. It will only contain 1st level items (element on the root of the repository)
 */
export const changedFiles = async (from: string, to = 'HEAD'): Promise<string[]> => {
  return new Promise((resolve, reject) => {
    const cmd = diffCommand(from, to);

    console.log(`Running "${cmd}"`);
    const [bin, ...args] = cmd.split(' ');
    const child = spawn(bin, args);

    child.stdout.on('data', (data: Buffer) => {
      const files = data
        .toString()
        .split('\n')
        .map(keepFirstPathItem)
        .filter(removeDuplicate)
        .filter((f) => f.length > 0);

      resolve(files);
    });

    child.stderr.on('data', (data: Buffer) => {
      reject(new Error(data.toString()));
    });

    child.on('error', (err) => {
      reject(err);
    });
  });
};

const diffCommand = (from: string, to: string) => `git --no-pager diff --name-only ${from} ${to}`;
const keepFirstPathItem = (path: string) => path.split('/')[0];
const removeDuplicate = (path: string, index: number, arr: string[]) => arr.indexOf(path) === index;

/**
 * Returns true when the only change in the root pom.xml is a version bump of one or more
 * `gravitee-gamma-module-*.version` properties. Those modules are external dependencies that are
 * not built nor tested by this repository, so such a bump must not trigger the full CI.
 */
export const isRootPomOnlyGammaModuleBump = (from: string, to = 'HEAD'): boolean => {
  try {
    const { stdout, status } = spawnSync('git', ['--no-pager', 'diff', '-U0', from, to, '--', 'pom.xml'], { encoding: 'utf-8' });
    return status === 0 && isOnlyGammaModuleVersionBump(stdout);
  } catch {
    return false;
  }
};

export const isOnlyGammaModuleVersionBump = (diff: string): boolean => {
  const changedLines = diff.split('\n').filter((line) => /^[+-][^+-]/.test(line));
  return changedLines.length > 0 && changedLines.every((line) => /<gravitee-gamma-module-[a-z0-9-]+\.version>/.test(line));
};
