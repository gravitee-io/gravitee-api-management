/**
 * Git utilities — changed-file detection between commits.
 */
import { spawn } from 'node:child_process';

/**
 * Returns the files / directories changed between 2 commits.
 * Only the first path segment is kept so that the result contains
 * top-level items (modules at the root of the repository).
 */
export async function changedFiles(from: string, to = 'HEAD'): Promise<string[]> {
  const cmd = `git --no-pager diff --name-only ${from} ${to}`;

  return new Promise((resolve, reject) => {
    console.log(`Running "${cmd}"`);
    const [bin, ...args] = cmd.split(' ');
    const child = spawn(bin, args);

    child.stdout.on('data', (data: Buffer) => {
      const files = data
        .toString()
        .split('\n')
        .map((p) => p.split('/')[0])
        .filter((v, i, a) => a.indexOf(v) === i)
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
}
