# Couchbase Transactions Demo

This Spring Boot application showcases the `@Transactional` integration with Spring-Data-Couchbase and the underlying
SDK as well as the transactions library. Please note that this is meant to be consumed for demo/evaluation purposes
only and is not supported in production.

## Usage

 - To use this demo, you need to install the jar from the `jars` directory into your local maven repository so it can
be picked up as a dependency.
 - The `travel-sample` bucket needs to be loaded.
 - Also, the server needs to be configured so that transactions works as expected. So especially if you are using a single
node Couchbase server, make sure that the bucket has no replicas configured (or you'll see errors in the logs).
 - The Transaction config can be overridden in the `DatabaseConfiguration` config class if needed. 
 - When started, navigate to `localhost:8080`, which will provide a list of tasks to trigger that correspond to transactional
methods in the underlying `AirlineService`.