import { commands, parameters, reusable } from '@circleci/circleci-config-sdk';

export class WebuiInstallCommand {
  private static commandName = 'cmd-webui-install';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to build'),
  ]);

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(
      WebuiInstallCommand.commandName,
      [
        new commands.cache.Restore({
          name: 'Restore NPM cache',
          keys: [
            '<< parameters.apim-ui-project >>-cache-v1-{{ .Branch }}-{{ checksum "<< parameters.apim-ui-project >>/package-lock.json" }}',
            '<< parameters.apim-ui-project >>-cache-v1-{{ .Branch }}',
          ],
        }),
        new commands.Run({
          name: 'Install dependencies',
          command: 'npm install',
          working_directory: '<< parameters.apim-ui-project >>',
        }),
        new commands.cache.Save({
          name: 'Save NPM cache',
          key: '<< parameters.apim-ui-project >>-cache-v1-{{ .Branch }}-{{ checksum "<< parameters.apim-ui-project >>/package-lock.json" }}',
          paths: ['./<< parameters.apim-ui-project >>/node_modules'],
        }),
      ],
      WebuiInstallCommand.customParametersList,
    );
  }
}
