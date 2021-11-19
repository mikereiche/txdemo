package com.example.demo;

import com.couchbase.client.core.cnc.Event;
import com.couchbase.transactions.TransactionDurabilityLevel;
import com.couchbase.transactions.config.TransactionConfig;
import com.couchbase.transactions.config.TransactionConfigBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Duration;

@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration extends AbstractCouchbaseConfiguration {

  @Override
  public String getConnectionString() {
    return "10.144.220.101";
  }

  @Override
  public String getUserName() {
    return "Administrator";
  }

  @Override
  public String getPassword() {
    return "password";
  }

  @Override
  public String getBucketName() {
    return "travel-sample";
  }


  // non-practical settings for demo purposes
  @Bean
  @Override
  public TransactionConfig transactionConfig() {
    return TransactionConfigBuilder.create().logDirectly(Event.Severity.DEBUG).logOnFailure(true, Event.Severity.ERROR)
        .expirationTime(Duration.ofMinutes(10)).durabilityLevel(TransactionDurabilityLevel.NONE).build();
  }

}
