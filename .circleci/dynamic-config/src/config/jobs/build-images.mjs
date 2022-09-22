import { cache, Checkout, Job, ReusedCommand, Run, workspace } from '../../sdk/index.mjs';

export function createBuildImagesJob(executor) {
  return new Job('build-images', executor, [
    new Checkout(),
    new workspace.Attach({ at: '.' }),
    new ReusedCommand('prepare-env-var'),
    new ReusedCommand('get-apim-version'),
    new ReusedCommand('setup_remote_docker'),
    new ReusedCommand('build-backend-images'),
    new ReusedCommand('save-backend-images'),
    new workspace.Persist({
      root: './',
      paths: ['./docker-cache'],
    }),
  ]);
}
