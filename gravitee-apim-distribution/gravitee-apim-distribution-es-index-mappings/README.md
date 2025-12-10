# gravitee-apim-distribution-es-index-mappings

## Description

This Maven module is responsible for generating all the Elasticsearch index templates and pipeline configuration files for the Gravitee API Management (APIM) platform. 

It automates the creation of JSON files required to configure Elasticsearch indices used by APIM components.

Each time a new version of APIM is released, a ZIP file containing these templates and configurations is generated. This ZIP file can be downloaded from the Maven Central repository following the versioning pattern: `gravitee-apim-distribution-es-index-mappings-{APIM_VERSION}.zip`