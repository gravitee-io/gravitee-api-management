# Purge Storage Container

This simple script will purge the storage container older than a specified date.

First, be sure you have [`zx`](https://github.com/google/zx) CLI installed. 

To run this script you need to provide 2 env vars: 
 - CONNECTION_STRING: The connection string to the storage account
 - LIMIT_DATE: The limit date to purge the storage container (everything older than this date will be deleted), in the format `YYYY-MM-DDTHH:MM:SS+00:00`, for instance `2022-02-01T00:00:00+00:00`
 
Then, you can run the script with the following command:
`CONNECTION_STRING="" LIMIT_DATE="2022-02-01T00:00:00+00:00" zx ./purge-storage-container.md`

It will first get the env variables:

```javascript
const connectionString = process.env.CONNECTION_STRING;
const limitDate = process.env.LIMIT_DATE || '2022-02-01T00:00:00+00:00';
if(!connectionString) {
  console.error("You need to provide the following env vars: CONNECTION_STRING");
  process.exit(1);
}
```

Then call the Azure API to list all the containers matching the lastModified date: 
```javascript
const query = `[?properties.lastModified<="${limitDate}"].{name:name}`;
const containersToDeleteAsString = await $`az storage container list --connection-string=${connectionString} --query ${query}`;
const containersToDelete = JSON.parse(containersToDeleteAsString);
```

Then call the Azure API to delete all the containers: 
```javascript
console.log(`Deleting ${containersToDelete.length} containers`);
for(const container of containersToDelete) {
  console.log(`Deleting container ${container.name}`);  
  await $`az storage container delete --name ${container.name} --connection-string=${connectionString}`
  console.log(`🚮 Deleted container ${container.name}`);    
}
```

Et voila!