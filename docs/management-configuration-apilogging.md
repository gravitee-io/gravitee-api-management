# API Logging

Here you can enable to audit the consultation of log detail.
This can be useful to track who has accessed to a specific data from the audit view.

You will be able to display or not the end user in case of a OAuth2/JWT plan.
It will be extracted from the sub claim.

You can also limit the duration of API full logging (0 means no max duration).
This can be useful to avoid to API publishers to log headers / body payloads during too much time and avoid to consume too much CPU/memory.
By default, the calls are logged with the minimal information.
