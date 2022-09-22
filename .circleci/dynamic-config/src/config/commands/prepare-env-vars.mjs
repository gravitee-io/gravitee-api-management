import { ReusableCommand, Run } from '../../sdk/index.mjs';

export function createPrepareEnvVarsCommand(vars) {
  return new ReusableCommand(`prepare-env-var`, [
    new Run({
      command: Object.entries(vars)
        .map(([key, value]) => `echo "export ${key}=${value}" >> $BASH_ENV`)
        .join('\n'),
    }),
  ]);
}
