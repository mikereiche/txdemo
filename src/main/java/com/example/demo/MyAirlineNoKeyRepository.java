package com.example.demo;

import org.springframework.data.couchbase.repository.CouchbaseRepository;

public interface MyAirlineNoKeyRepository extends CouchbaseRepository<AirlineNoKey, String> {}
