# Mysql Rest Service

---

## About

**Mysql Rest Service** is a Java & Jersey based REST service to support various CRUD operations for MySQL database. Presently it supports adding, updating and deleting records. More details can be seen in the wiki page. 

The service loads properties from classpath to connect to the target DB. Currently the name of the file is config.properties. 

Note: The current implementation does not support request based authentication and DB login is done via config.properties only. This feature will be available in next releases.
		This service will not support DDL operations, hence operations like create table, alter table etc cannot be done.

## Usage
### Query ( Fetch Data )

Query is supported over POST http request. Sample POST request as below:

```
POST http://localhost:8080/mrest/api/mdml/query
dbName=myDb1
query=Select * from MyTable1
```

Sample CURL request would be:

````
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' 'http://localhost:8080/mrest/api/mdml/query?dbName=myDb1&query=Select%20*%20from%20MyTable1%20where%20id%20%3D%2010001'
````

The response for above request will be like below:
````
 {
  "objects": [
    {      
      "id": 10001,
	  "dateOfCreation": 1429720003000,
      "address": "Pune, India",
      "dateUpdated": "2015-04-11",
      "idOtherTable": 20001
    }
   ],
   "status": true,
   "message": "Successful",
   "time": 493
 }
````
