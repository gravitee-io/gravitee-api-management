# PostgreSQL

Here is a docker-compose to run APIM with PostgreSQL as database.

---
> For more information, please read :
> https://documentation.gravitee.io/apim/getting-started/configuration/repositories/jdbc
---
## Requirements

You need to provide a JDBC driver for postgresql.
Put it in a `.driver` folder
You can download one here: https://jdbc.postgresql.org/download/

## How to run ?

⚠️ You need a license file to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

