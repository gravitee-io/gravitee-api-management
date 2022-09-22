import { ReusableCommand, Run } from '../../sdk/index.mjs';

export function createGetApimVersionCommand() {
  return new ReusableCommand('get-apim-version', [
    new Run({
      name: 'Read Version',
      command: ['echo "export APIM_VERSION=$(cat ./.apim-version.txt) >> $BASH_ENV"'].join('\n'),
    }),
  ]);
}
