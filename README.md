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

The query can be any simple or complex SQL SELECT statement which can contain any 'where', 'join' or 'aggregate' statements. It does not support INSERT/UPDATE or DELETE statements. 
The select operation can be done only on the DB specified as parameter dbName. Cross DB select is not supported. The authentication will be done from config*.

The response is a JSON object and is explained as below:
````
'objects' = an array of records, each element of array being a row of the resultset
'status' = the boolean status of the operation i.e. true for success and false for failure
'message' = the message describing the status of operation
'time' = the time taken in the operation in milliseconds
````
Each element of the objects array is an row returned as a result of the sql statement. The SQL statement must be written in such a way that each output column has a unique name. So in case of joins where multiple tables have same column, the query can use an alias for each output column to uniquely identify a column. This is required because the implementation uses a map to represent a row object and only single entry can be done for same column name repeated in a row.