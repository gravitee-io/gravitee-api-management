# Dictionary

A dictionary can be MANUAL or DYNAMIC.

## MANUAL
In the case of a _manual_ dictionary, it is the responsibility of the administrator (or any user with update right) to
create / update or remove properties.

In the same, it is the responsibility of the administrator to deploy the dictionary into API Gateways.
 
 
## DYNAMIC
In the case of a _dynamic_ dictionary, properties are updated automatically according to an underlying provider.

Each time a change has been detected between previous properties and new properties, the dictionary is automatically
deployed into API Gateways and are then available during the HTTP request process.
