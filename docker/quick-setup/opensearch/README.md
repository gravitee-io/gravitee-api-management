# OpenSearch

Here is a docker-compose to run APIM with OpenSearch.

---
> For more information, please read :
> https://documentation.gravitee.io/apim/getting-started/configuration/repositories/elasticsearch
---

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`
