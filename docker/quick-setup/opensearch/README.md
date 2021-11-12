# OpenSearch

Here is a docker-compose to run APIM with OpenSearch.

---
> For more information, please read :
> https://docs.gravitee.io/apim/3.x/apim_installguide_repositories_elasticsearch.html
---

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`
