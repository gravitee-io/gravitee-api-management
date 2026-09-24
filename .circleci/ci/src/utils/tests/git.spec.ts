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
import { EventEmitter } from 'node:events';
import { spawn } from 'node:child_process';
import { changedFiles, diffRef, refsUnder, toChangedPaths } from '../git';

jest.mock('node:child_process', () => ({ spawn: jest.fn() }));

class FakeChild extends EventEmitter {
  stdout = new EventEmitter();
  stderr = new EventEmitter();
}

const runWithChunks = (chunks: string[], code = 0): Promise<string[]> => {
  const child = new FakeChild();
  (spawn as jest.Mock).mockReturnValue(child);

  const promise = changedFiles('a-sha');

  chunks.forEach((chunk) => child.stdout.emit('data', Buffer.from(chunk)));
  child.emit('close', code);

  return promise;
};

describe('changedFiles', () => {
  it('should keep the paths of every chunk, not only the first one', async () => {
    // A pipe hands over 64 KB at a time, so a wide diff arrives in several chunks.
    const files = await runWithChunks(['gravitee-apim-console-webui/src/a.ts\ngravitee-apim-gate', 'way/src/b.java\nhelm/values.yaml\n']);

    expect(files).toEqual(['gravitee-apim-console-webui', 'gravitee-apim-gateway', 'helm']);
  });

  it('should diff from the merge base', async () => {
    const child = new FakeChild();
    (spawn as jest.Mock).mockReturnValue(child);

    const promise = changedFiles('a-sha', 'a-ref');
    child.emit('close', 0);
    await promise;

    expect(spawn).toHaveBeenCalledWith('git', ['--no-pager', 'diff', '--name-only', 'a-sha...a-ref']);
  });

  it('should reject when git fails', async () => {
    const child = new FakeChild();
    (spawn as jest.Mock).mockReturnValue(child);

    const promise = changedFiles('a-sha');
    child.stderr.emit('data', Buffer.from('fatal: bad revision'));
    child.emit('close', 128);

    await expect(promise).rejects.toThrow('fatal: bad revision');
  });
});

describe('toChangedPaths', () => {
  it('should keep the first path item, once', () => {
    expect(toChangedPaths('a/b/c.ts\na/d.ts\nb/e.ts\n')).toEqual(['a', 'b']);
  });

  it('should drop empty lines', () => {
    expect(toChangedPaths('\n\na/b.ts\n\n')).toEqual(['a']);
  });
});

describe('diffRef', () => {
  it('should use the common commit when there is one', () => {
    expect(diffRef('a-sha', 'origin/master')).toEqual('a-sha');
  });

  it('should fall back to the base branch when the common commit is empty', () => {
    // `jq -r '.base.sha // ""'` yields an empty string when the GitHub API answers without it,
    // so the fallback has to treat empty as absent, not just undefined.
    expect(diffRef('', 'origin/master')).toEqual('origin/master');
  });
});

describe('refsUnder', () => {
  // `git ls-remote --tags --refs origin`, as it comes: one `<sha>\t<ref>` per line.
  const TAGS = [
    '6e63f853294a0c66a671bf9d24c4c1e7ae7c1cab\trefs/tags/0.1.0',
    '6e1fa64e00aefc17fd2ff24c17c4646791cd9aef\trefs/tags/4.12.0',
    '11937c094dbaee4fb094fa4d5cd190ed8c9e66e3\trefs/tags/core_4.13.0',
  ].join('\n');

  it('should name the refs under the prefix', () => {
    expect(refsUnder(TAGS, 'refs/tags/')).toStrictEqual(['0.1.0', '4.12.0', 'core_4.13.0']);
  });

  it('should read heads the same way', () => {
    const heads =
      'eae3a8ab7687f7aa543539757a39ae6f2b7bb315\trefs/heads/3.10.x\ndf67ea7ace37b20f03b38ed67751197708b5805c\trefs/heads/4.12.x';

    expect(refsUnder(heads, 'refs/heads/')).toStrictEqual(['3.10.x', '4.12.x']);
  });

  // The callers ask which versions exist: a line they cannot name is a line they must not count.
  it('should drop what does not sit under the prefix', () => {
    expect(refsUnder(TAGS, 'refs/heads/')).toStrictEqual([]);
  });

  it('should answer nothing on an empty output', () => {
    expect(refsUnder('', 'refs/tags/')).toStrictEqual([]);
  });
});
