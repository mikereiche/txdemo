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

import static com.couchbase.client.java.query.QueryScanConsistency.REQUEST_PLUS;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.couchbase.client.java.manager.query.CreateQueryIndexOptions;
import com.couchbase.client.java.manager.query.QueryIndexManager;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.transactions.error.TransactionFailed;

@SpringBootApplication
@RestController
@EnableTransactionManagement
public class DemoApplication {

	private final CouchbaseTemplate template;

	@Autowired MyAirlineRepository airlineRepo;

	@Autowired MyAirlineNoKeyRepository airlineNoKeyRepo;

	@Autowired AirlineService airlineService;

	String airlineId;
	public static String myAirline = "My Airline";
	public static String yourAirline = "Your Airline";
	QueryOptions requestPlus = QueryOptions.queryOptions().scanConsistency(REQUEST_PLUS);
	public String displayString = displayText();
	StringBuffer sb = new StringBuffer();
	long t0;

	public DemoApplication(@Autowired CouchbaseTemplate template) {
		this.template = template;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@GetMapping("/")
	public String aaa_index() {
		return doWorkAndValidate(() -> {}, () -> {});
	}

	@GetMapping("/find")
	public String find(@RequestParam(value = "airlineId", defaultValue = "airline_10123") String airlineId) {
		// the existing airline documents in travel-sample do not have the _class attribute and therefore will
		// not be accessible by query, but they will be accessible ById.
		return doWorkAndValidate(() -> airlineService.hello(airlineId), () -> {});
	}

	@GetMapping("/queryAndCountCommitted")
	public String queryAndCountCommitted() {
		return doWorkAndValidate(() -> airlineService.queryAndCountCommitted(airlineId), () -> {
			assertEquals(2, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).all().size(),
					"did not find documents");
			assertEquals(2l, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).count(),
					"did not find documents");
		});
	}

	@GetMapping("/queryAndCountCommittedRepo")
	public String queryAndCountCommittedRepo() {
		return doWorkAndValidate(() -> airlineService.queryAndCountCommittedRepo(airlineId), //
				() -> {
					assertEquals(2, airlineRepo.withOptions(requestPlus).findAll().size(), "did not find documents");
					assertEquals(2l, airlineRepo.withOptions(requestPlus).count(), "did not find documents");
				});
	}

	@GetMapping("/queryAndCountUncommitted")
	public String queryAndCountUncommitted() {
		return doWorkAndValidate(() -> airlineService.queryAndCountUncommitted(airlineId), //
				() -> {
					assertEquals(0, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).all().size(),
							"should not have found documents");
					assertEquals(0l, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).count(),
							"should not have found documents");
				});
	}

	@GetMapping("/queryAndCountUncommittedRepo")
	public String queryAndCountUncommittedRepo() {
		return doWorkAndValidate(() -> airlineService.queryAndCountUncommittedRepo(airlineId), //
				() -> {
					assertEquals(0, airlineRepo.withOptions(requestPlus).findAll().size(), "should not have found documents");
					assertEquals(0l, airlineRepo.withOptions(requestPlus).count(), "should not have found documents");
				});

	}

	@GetMapping("/insertCommit")
	public String insertCommit() {
		return doWorkAndValidate(() -> airlineService.insertCommit(airlineId),
				() -> assertNotNull(template.findById(Airline.class).one(airlineId), "not committed"));

	}

	@GetMapping("/insertCommitRepo")
	public String insertCommitRepo() {
		return doWorkAndValidate(() -> airlineService.insertCommitRepo(airlineId), () -> {
			assertTrue(airlineRepo.findById(airlineId).isPresent(), "not committed");
			assertNotNull(airlineRepo.findById(airlineId).get(), "not committed");
		});
	}

	@GetMapping("/replaceCommit")
	public String replaceCommit() {
		return doWorkAndValidate(() -> {
			log("insert: " + template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
			airlineService.replaceCommit(airlineId);
		}, () -> assertEquals(yourAirline, template.findById(Airline.class).one(airlineId).name, "not commited"));
	}

	@GetMapping("/replaceCommitRepo")
	public String replaceCommitRepo() {
		return doWorkAndValidate(() -> {
			log("insert: ", airlineRepo.save(new Airline(airlineId, myAirline)));
			airlineService.replaceCommitRepo(airlineId);
		}, () -> assertEquals(yourAirline, airlineRepo.findById(airlineId).get().name, "not commited"));
	}

	@GetMapping("/replaceCommitNoKey")
	public String replaceCommitNoKey() {
		return doWorkAndValidate(() -> {
			log("insert: ", template.insertById(AirlineNoKey.class).one(new AirlineNoKey(airlineId, myAirline)));
			airlineService.replaceCommitNoKey(airlineId);
		}, () -> assertEquals(yourAirline, template.findById(AirlineNoKey.class).one(airlineId).name, "not commited"));
	}

	@GetMapping("/replaceCommitNoKeyRepo")
	public String replaceCommitNoKeyRepo() {
		return doWorkAndValidate(() -> {
			log("insert: ", airlineNoKeyRepo.save(new AirlineNoKey(airlineId, myAirline)));
			airlineService.replaceCommitNoKeyRepo(airlineId);
		}, () -> assertEquals(yourAirline, airlineRepo.findById(airlineId).get().name, "not committed"));
	}

	@GetMapping("/replaceCommitImmutable")
	public String replaceCommitImmutable() {
		return doWorkAndValidate(() -> {
			log("insert: ", template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
			airlineService.replaceCommitImmutable(airlineId);
		}, () -> assertEquals(yourAirline, template.findById(Airline.class).one(airlineId).name, "not committed"));
	}

	@GetMapping("/replaceCommitImmutableRepo")
	public String replaceCommitImmutableRepo() {
		return doWorkAndValidate(() -> {
			log("insert: ", airlineRepo.save(new Airline(airlineId, myAirline)));
			airlineService.replaceCommitImmutableRepo(airlineId);
		}, () -> assertEquals(yourAirline, airlineRepo.findById(airlineId).get().name, "not commited"));
	}

	@GetMapping("/removeCommit")
	public String removeCommit() {
		return doWorkAndValidate(() -> {
			log("insert: ", template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
			airlineService.removeCommit(airlineId);
		}, () -> assertNull(template.findById(Airline.class).one(airlineId), "not committed"));
	}

	@GetMapping("/removeCommitRepo")
	public String removeCommitRepo() {
		return doWorkAndValidate(() -> {
			log("insert: ", airlineRepo.save(new Airline(airlineId, myAirline)));
			airlineService.removeCommitRepo(airlineId);
		}, () -> assertFalse((Boolean) log("isPresent: ", airlineRepo.findById(airlineId).isPresent()), "not committed"));
	}

	@GetMapping("/insertRollback")
	public String insertRollback() {
		return doWorkAndValidate(() -> airlineService.insertRollback(airlineId),
				() -> assertNull(template.findById(Airline.class).one(airlineId), "not rolled back"));
	}

	@GetMapping("/insertRollbackRepo")
	public String insertRollbackRepo() {
		return doWorkAndValidate(() -> airlineService.insertRollbackRepo(airlineId),
				() -> assertFalse((Boolean) log("isPresent: ", airlineRepo.findById(airlineId).isPresent()),
						"not rolled back"));
	}

	@GetMapping("/replaceRollback")
	public String replaceRollback() {
		return doWorkAndValidate(() -> {
			log("insert: ", template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
			airlineService.replaceRollback(airlineId);
		}, () -> assertEquals(myAirline, template.findById(Airline.class).one(airlineId).name, "not rolled back"));
	}

	@GetMapping("/replaceRollbackRepo")
	public String replaceRollbackRepo() {
		return doWorkAndValidate(() -> {
			log("insert: ", airlineRepo.save(new Airline(airlineId, myAirline)));
			airlineService.replaceRollbackRepo(airlineId);
		}, () -> assertEquals(myAirline, airlineRepo.findById(airlineId).get().name, "not rolled back"));
	}

	@GetMapping("/removeRollback")
	public String removeRollback() {
		return doWorkAndValidate(() -> {
			log("insert: ", template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
			airlineService.removeRollback(airlineId);
		}, () -> assertNotNull(template.findById(Airline.class).one(airlineId), "not rolled back"));
	}

	@GetMapping("/removeRollbackRepo")
	public String removeRollbackRepo() {
		return doWorkAndValidate(() -> {
			log("insert: ", airlineRepo.save(new Airline(airlineId, myAirline)));
			airlineService.removeRollbackRepo(airlineId);
		}, () -> assertTrue((Boolean) log("isPresent: ", airlineRepo.findById(airlineId).isPresent()), "not rolled back"));
	}

	// --- Utilities ---

	void before() {
		airlineId = methodName();
		sb.setLength(0);
		airlineService.setSb(sb);
		t0 = System.currentTimeMillis();
		removeAll(Airline.class);
		removeAll(AirlineNoKey.class);
		if ((System.currentTimeMillis() - t0) > 1000) {
			log("creating index on _class");
			QueryIndexManager indexManager = template.getCouchbaseClientFactory().getCluster().queryIndexes();
			Collection<String> fields = Arrays.asList("_class");
			indexManager.createIndex(template.getCouchbaseClientFactory().getBucket().name(), "class_index", fields,
					CreateQueryIndexOptions.createQueryIndexOptions().ignoreIfExists(true));
		}
		log(displayString);
		log(methodName());
		t0 = System.currentTimeMillis();
	}

	String after() {
		log("<br>Ran in: ", System.currentTimeMillis() - t0, "ms");
		String output = sb.toString();
		sb.setLength(0);
		return output;
	}

	private <T> void removeAll(Class<T> clazz) {
		template.removeByQuery(clazz).withConsistency(REQUEST_PLUS).all();
		List<T> list = template.findByQuery(clazz).withConsistency(REQUEST_PLUS).all();
		if (!list.isEmpty()) {
			throw new RuntimeException("there are some " + clazz + " left over");
		}

	}

	String doWorkAndValidate(Runnable work, Runnable validate) {
		before();
		try {
			work.run();
		} catch (TransactionFailed e) {
			log("caught: ", e);
			if (e.getCause() == null || !(e.getCause() instanceof SimulatedFailureException)) {
				return after(); // something went wrong
			}
		}
		validate.run();
		return after();
	}

	String methodName() { // called from before(), called from doWork()
		return Thread.currentThread().getStackTrace()[4].getMethodName();
	}

	/**
	 * Thrown to simulate certain kinds of failures for example.
	 */
	public static class SimulatedFailureException extends RuntimeException {}

	String displayText() {
		StringBuffer s = new StringBuffer();
		String urlPrefix = "http://localhost:8080";
		s.append("<table>");
		List<Method> methods = Arrays.asList(this.getClass().getMethods());
		Collections.sort(methods, new Comparator<Method>() {
			@Override
			public int compare(Method o1, Method o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for (Method m : methods) {

			GetMapping a = m.getAnnotation(GetMapping.class);
			if (a != null) {
				s.append("<tr>");
				s.append("<td>");
				s.append("<a href=\"");
				s.append(urlPrefix + a.value()[0]);
				s.append("\">");
				s.append(a.value()[0]);
				s.append("</a>");
				s.append("</td>");
				s.append("</tr>");
			}
		}
		s.append("</table>");
		displayString = s.toString();
		return displayString;
	}

	public Object log(Object... args) {
		if (args.length > 0 && args[0] instanceof String && !((String) args[0]).startsWith("<")) {
			sb.append("<br>__: ");
		}
		for (Object s : args) {
			if (s instanceof Exception) {
				sb.append("<b>");
			}
			sb.append(s);
			if (s instanceof Exception) {
				sb.append("</b>");
			}
		}
		sb.append("\n");
		for (Object a : args) {
			if (!(a instanceof String)) {
				return a;
			}
		}
		return null;
	}

	private void assertFalse(boolean b, String s) {
		try {
			org.junit.jupiter.api.Assertions.assertFalse(b, s);
		} catch (AssertionError e) {
			log("<br><H2>", e.getMessage());
		}
	}

	private void assertNull(Object a, String s) {
		try {
			log("found: ", a);
			org.junit.jupiter.api.Assertions.assertNull(a, s);
		} catch (AssertionError e) {
			log("<br><H2>", e.getMessage());
		}
	}

	private void assertTrue(boolean b, String s) {
		try {
			org.junit.jupiter.api.Assertions.assertTrue(b, s);
		} catch (AssertionError e) {
			log("<br><H2>", e.getMessage());
		}
	}

	private void assertNotNull(Object a, String s) {
		try {
			log("found: ", a);
			org.junit.jupiter.api.Assertions.assertNotNull(a, s);
		} catch (AssertionError e) {
			log("<br><H2>", e.getMessage());
		}
	}

	private void assertEquals(Object a, Object b, String s) {
		try {
			log("found: ", b);
			org.junit.jupiter.api.Assertions.assertEquals(a, b, s);
		} catch (AssertionError e) {
			log("<br><H2>", e.getMessage());
		}
	}

}
