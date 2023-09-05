import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { OpenJdkNodeExecutor } from '../executors';
import { orbs } from '../orbs';
import { config } from '../config';
import { ReusedCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { keeper } from '../orbs/keeper';
import { awsS3 } from '../orbs/aws-s3';
import { GraviteeioVersion, parse } from '../utils';

export class PackageBundleJob {
  private static readonly ARTIFACTORY_REPO_URL =
    'https://odbxikk7vo-artifactory.services.clever-cloud.com/external-dependencies-n-gravitee-all';

  public static create(dynamicConfig: Config, graviteeioVersion: string, isDryRun: boolean) {
    dynamicConfig.importOrb(keeper);
    dynamicConfig.importOrb(awsS3);

    const parsedGraviteeioVersion = parse(graviteeioVersion);

    return new Job('job-package-bundle', OpenJdkNodeExecutor.create(), [
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.artifactoryUser,
        'var-name': 'ARTIFACTORY_USERNAME',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.artifactoryApiKey,
        'var-name': 'ARTIFACTORY_PASSWORD',
      }),
      new commands.Checkout(),
      new commands.Run({
        name: `Checkout tag ${parsedGraviteeioVersion.version.full}`,
        command: `git checkout ${parsedGraviteeioVersion.version.full}`,
      }),
      new commands.Run({
        name: 'Install dependencies',
        command: 'npm install',
        working_directory: './release',
      }),
      new commands.Run({
        name: 'Building package bundle',
        command: `npm run zx -- --quiet --experimental ci-steps/package-bundles.mjs --version=${parsedGraviteeioVersion.version.full}`,
        working_directory: './release',
        environment: {
          ARTIFACTORY_REPO_URL: PackageBundleJob.ARTIFACTORY_REPO_URL,
        },
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.awsAccessKeyId,
        'var-name': 'AWS_ACCESS_KEY_ID',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.awsSecretAccessKey,
        'var-name': 'AWS_SECRET_ACCESS_KEY',
      }),
      PackageBundleJob.getSyncCommand(parsedGraviteeioVersion, isDryRun),
    ]);
  }

  private static getSyncCommand(graviteeioVersion: GraviteeioVersion, isDryRun: boolean): ReusedCommand {
    const targetFolder =
      graviteeioVersion.qualifier.full && graviteeioVersion.qualifier.full.length > 0
        ? '/pre-releases/graviteeio-apim'
        : '/graviteeio-apim';

    let to = '';
    if (isDryRun) {
      to = `s3://gravitee-dry-releases-downloads${targetFolder}`;
    } else {
      to = `s3://gravitee-releases-downloads${targetFolder}`;
    }
    return new reusable.ReusedCommand(orbs.awsS3.commands.sync, {
      arguments: '--endpoint-url https://cellar-c2.services.clever-cloud.com --acl public-read',
      from: `./release/.tmp/${graviteeioVersion.version.full}/dist`,
      to: `${to}`,
    });
  }
}
