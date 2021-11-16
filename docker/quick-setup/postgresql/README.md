# PostgreSQL

Here is a docker-compose to run APIM with PostgreSQL as database.

---
> For more information, please read :
> https://docs.gravitee.io/apim/3.x/apim_installguide_repositories_jdbc.html
---
## Requirements

You need to provide a JDBC driver for postgresql.
Put it in a `.driver` folder
You can download one here: https://jdbc.postgresql.org/download.html

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

