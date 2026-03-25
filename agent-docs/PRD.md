# Existing code:

I have 2 developer portals in APIM: Classic portal and next gen portal.

Next gen portal is currently in developer preview mode and is not generally available but it can be enabled by a setting in the APIM console in this file gravitee-apim-console-webui/src/management/settings/portal-settings/portal-settings.component.html. By default, the classic portal is enabled.

By default, the classic portal frontend is accessible at https://portal.customer-example.com/ and the next gen portal frontend is accessible at https://portal.customer-example.com/next (if it's enabled via the setting in the APIM console).

There is an environment variable DEFAULT_PORTAL=next as per this file docker/quick-setup/mongodb/docker-compose.yml which allows to set the next gen portal as the default portal. In this case, the next gen portal frontend is accessible at https://portal.customer-example.com/ and the classic portal frontend becomes accessible at https://portal.customer-example.com/classic.


I have customer that deploys APIM on premises and other customers that use Gravitee cloud.

For Cloud customers, the DEFAULT_PORTAL=next is shared across all environments.

# Feature request:

Default Portal for Cloud Users

As a Cloud Administrator, I want the ability to toggle the Next-Gen Developer Portal as the "default" experience at the environment level, So that my developers can access the modern portal via the base URL without needing to append /next

Acceptance Criteria:

- A new/existing configuration setting/toggle must be added to the **Environment** settings within the APIM Console.
- For **Cloud Customers**, this setting must override the shared global configuration file, allowing for customer-by-customer activation.
- When the "Set as Default" toggle is **Enabled**:
    - Navigating to the Base URL (e.g., `https://portal.customer-example.com`) must automatically render the Next-Gen Developer Portal.
    - The legacy portal remains accessible (if required) via a fallback path or is fully superseded based on the implementation design.
- When the "Set as Default" toggle is **Disabled**:
    - Navigating to the Base URL must continue to render the legacy Developer Portal.
    - The Next-Gen portal remains accessible via the `/next` suffix
- For self-hosted installations, the UI toggle should reflect
- The system must ensure that the UI toggle and the startup configuration file remain in sync or follow a clear "order of precedence" (e.g., UI settings override file settings if persisted in the DB).\
- The upgrade must default this toggle to **Disabled** for existing environments to prevent unexpected UI changes for current users.
- New environments created after this change should have the option to set the Next-Gen portal as the default from the start.
- The setting must be accessible via the Management API

# Developer comments

Here are comments from a developer that worked on the portal:

- Determine how to pass classic/next portals from docker-compose to portal UI (currently docker-compose writes urls in baseHref in index.html)
- Console: How can we discover how portal(s) is (are) actually deployed?
- What should be the behavior of ‘toggle’ when docker-compose is built with default=next?
- Portal FE - implement /next and /classic route handling in portal next and classic
