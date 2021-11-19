package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.DynamicProxyable;

public interface MyAirlineRepository
		extends CouchbaseRepository<Airline, String>, DynamicProxyable<MyAirlineRepository> {}
