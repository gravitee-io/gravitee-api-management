# Microsoft SQL

Here is a docker-compose to run APIM with mssql as database.

---
> For more information, please read :
> https://documentation.gravitee.io/apim/getting-started/configuration/configure-repositories/jdbc
---
## Requirements

You need to provide a JDBC driver for mssql.
Put it in a `.driver` folder
You can download one here: https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server?view=sql-server-ver16

## How to run ?

⚠️ You need a license file to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

