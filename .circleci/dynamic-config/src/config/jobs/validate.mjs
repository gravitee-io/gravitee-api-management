import { Checkout, Job, ReusedCommand, Run, workspace } from '../../sdk/index.mjs';

export function createValidateJob(executor) {
  return new Job('validate', executor, [
    new Checkout(),
    new workspace.Attach({ at: '.' }),
    new ReusedCommand('restore-maven-job-cache', {
      jobName: 'validate',
    }),
    new Run({
      name: 'validate project',
      command: 'mvn -s .gravitee.settings.xml validate --no-transfer-progress -T 2C',
    }),
    new ReusedCommand('save-maven-job-cache', {
      jobName: 'validate',
    }),
    new ReusedCommand('notify-on-failure'),
  ]);
}
