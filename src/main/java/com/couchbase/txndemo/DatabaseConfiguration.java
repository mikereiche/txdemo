/*
 * Copyright (c) 2021 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.txndemo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ConfigurationProperties("db.couchbase")
public class DatabaseConfiguration extends AbstractCouchbaseConfiguration {

  private String connectionString;

  private String userName;

  private String password;

  private String bucketName;

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

  public void setUserName(String username) {
    this.userName = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  @Override
  public String getConnectionString() {
    return connectionString;
  }

  @Override
  public String getUserName() {
    return userName;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getBucketName() {
    return bucketName;
  }

  /* for testing on single cb server instance
  @Override
  public TransactionConfig transactionConfig() {
    return TransactionConfigBuilder.create().logDirectly(Event.Severity.INFO).logOnFailure(true, Event.Severity.ERROR)
        .expirationTime(Duration.ofSeconds(10)).durabilityLevel(TransactionDurabilityLevel.NONE).build();
  }
   */
}
