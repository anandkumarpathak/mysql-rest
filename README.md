# Mysql Rest Service

---

## About

**Mysql Rest Service** is a Java & Jersey based REST service to support various CRUD operations for MySQL database. Presently it supports adding, updating and deleting records. More details can be seen in the wiki page. 

The service loads properties from classpath to connect to the target DB. Currently the name of the file is config.properties. 

Note: The current implementation does not support request based authentication and DB login is done via config.properties only. This feature will be available in next releases.
