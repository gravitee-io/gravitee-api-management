# GraviteeApimPortalWebuiNext

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 17.2.3.

## Development server

Run `yarn serve` for a dev server. Navigate to `http://localhost:4101/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `yarn build` to build the project. The build artifacts will be stored in the `dist/` directory.

### Building gravitee-markdown library

This project contains the `@gravitee/gravitee-markdown` library. The library can be built with the command:

```bash
yarn build:gravitee-markdown
```

This builds the library using the default configuration and outputs to `dist/gravitee-markdown`.

The changes made to the library components will be immediately reflected in the portal application during development.

## Running unit tests

Run `yarn test` to execute the unit tests via [Jest](https://jestjs.io/).

## Translations

You can add your own translations to this project.

### Manually add your translations

1. Run `yarn extract` to extract the source language file.
   - You can specify the file extension with `yarn extract --format=<format>`. Find out more about [available formats](https://angular.io/guide/i18n-common-translation-files#extract-i18n---format-example).
2. Rename the translation file in `src/locale` to add the locale: `messages.xlf --> messages.{locale}.xlf`
3. Complete the file with the desired translations.
4. In the `angular.json` file, add the new local to `i18n.locales`. Example:

```json
{
  // ...
  "i18n": {
    "sourceLocale": "en-US",
    "locales": {
      "fr": {
        "translation": "src/locale/messages.fr.xlf"
      }
    }
  }
  // ...
}
```

5. Run `yarn build:i18n` to build all translations of your app.
6. Serve the application from the `gravitee-apim-portal-webui/dist/next/browser`.
   - You use a simple HTTP server to test the build with `npx http-server gravitee-apim-portal-webui/dist/next/browser`.
7. The translations will be available via `<host>/en-US/` and for each locale specified, example: `<host>/fr/`.

### Use a script to merge your translations

1. Run `yarn extract` to extract the source language file.

- You can specify the file extension with `yarn extract --format=<format>`. Find out more about [available formats](https://angular.io/guide/i18n-common-translation-files#extract-i18n---format-example).

2. Rename the translation file in `src/locale` to add the locale: `messages.xlf --> messages.{locale}.xlf`
3. Complete the file with the desired translations.
4. Run `yarn merge:i18n` to update the `angular.json` with the locale configuration and build the project.
5. Serve the application from the `gravitee-apim-portal-webui/dist/next/browser`.

Find out more about [@angular/localize](https://angular.io/guide/i18n-common-translation-files).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
