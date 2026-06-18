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
 * Returns true when the given commit only bumps one or more `gravitee-gamma-module-*.version`
 * properties in the root pom.xml and changes nothing else. Those modules are external dependencies
 * that are not built nor tested by this repository, so such a commit must not trigger the full CI.
 *
 * The check runs against the commit and its parent (`${commit}~1..${commit}`), not the PR base.
 * The base detection can resolve too far back when the branch sits on commits not yet on the target
 * branch ref, which would pull unrelated changes into the diff and defeat the skip. The commit is
 * passed explicitly (CIRCLE_SHA1) because CircleCI checks out a PR merge commit, so HEAD would be the
 * merge and HEAD~1 the base branch, not the actual bump commit.
 */
export const isCommitOnlyGammaModuleBump = (commit: string): boolean => {
  const parent = `${commit}~1`;
  try {
    const files = runGit(['--no-pager', 'diff', '--name-only', parent, commit])
      .split('\n')
      .filter((file) => file.length > 0);
    if (files.length !== 1 || files[0] !== 'pom.xml') {
      return false;
    }
    // No pathspec: it would resolve relative to the current working directory (the generator runs from
    // .circleci/ci). Only pom.xml changed, so the full commit diff is the pom.xml diff.
    return isOnlyGammaModuleVersionBump(runGit(['--no-pager', 'diff', '-U0', parent, commit]));
  } catch {
    return false;
  }
};

export const isOnlyGammaModuleVersionBump = (diff: string): boolean => {
  const changedLines = diff.split('\n').filter((line) => /^[+-][^+-]/.test(line));
  return changedLines.length > 0 && changedLines.every((line) => /<gravitee-gamma-module-[a-z0-9-]+\.version>/.test(line));
};

const runGit = (args: string[]): string => {
  const { stdout, status, stderr } = spawnSync('git', args, { encoding: 'utf-8' });
  if (status !== 0) {
    throw new Error(stderr || `git ${args.join(' ')} exited with ${status}`);
  }
  return stdout;
};
