import { ReusableCommand, Run } from '../../sdk/index.mjs';

export function createInstallJdkCommand(version) {
  return new ReusableCommand(`install-jdk-${version}-for-machine`, [
    new Run({
      command: ['sudo apt update', 'sudo apt install -y openjdk-${version}-jdk'].join('\n'),
    }),
  ]);
}
