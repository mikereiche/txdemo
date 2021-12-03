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

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

public class AirlineNoKey {

  @Id
  public String id;

  public String name;

  @Version
  public long version; // repository.save() needs version to distinguish insert/replace/upsert for tx/no-tx

  public AirlineNoKey(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String toString() {
    return "{ id: " + id + ", name: \"" + name +"\" }";
  }

}

