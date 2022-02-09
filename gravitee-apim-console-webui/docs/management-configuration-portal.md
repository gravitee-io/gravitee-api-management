# Portal Settings

Be careful before updating values, they will be automatically available on every new user session.

For example, you are able to select the Theme to apply on the portal or toggle features like Support or Rating

Some settings are read-only because they are set with environment variables.

## API key configuration

The custom API key mode allows you to set arbitrary values as API keys. This way, API publishers are able to silently migrate to Gravitee.io without changing consumers API key.

The shared API key mode will make consumers reuse the same API key across all API subscriptions of this application.
With this mode enabled, consumers will be asked on their second subscription to choose between reusing their key
across all subscriptions or generate one different API key for each subscription (which is the default mode).

This choice is permanent and consumers will not be able to switch back to one key per subscription for their application.

When disabling the shared API key mode, applications that have already been configured to use a shared key will continue
to work this way, but consumers will stop being asked to choose between one mode or the other on their second subscription.
