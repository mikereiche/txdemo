package com.example.demo;

import static com.couchbase.client.java.query.QueryScanConsistency.REQUEST_PLUS;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.transactions.error.TransactionFailed;
import com.example.demo.service.AirlineService;

@SpringBootApplication
@RestController
@EnableTransactionManagement
public class DemoApplication {

	private final CouchbaseTemplate template;

	@Autowired MyAirlineRepository airlineRepo;
	@Autowired MyAirlineNoKeyRepository airlineNoKeyRepo;
	@Autowired AirlineService airlineService;

	static GenericApplicationContext context;
	String airlineId;
	public static String myAirline = "My Airline";
	public static String yourAirline = "Your Airline";
	QueryOptions requestPlus = QueryOptions.queryOptions().scanConsistency(REQUEST_PLUS);
	StringBuffer sb = new StringBuffer();
	long t0;

	public void before() {
		System.err.println("before");
		airlineId = methodName();
		airlineService.setSb(sb);
		t0 = System.currentTimeMillis();
		removeAll(Airline.class);
		removeAll(AirlineNoKey.class);
		if ((System.currentTimeMillis() - t0) > 1000) {
			log("You may wish to create an index to speed things up: create index class_index on `travel-sample`(_class)");
		}
		System.err.println("starting " + (System.currentTimeMillis() - t0) + "ms");
		log(displayText(new StringBuffer()));
		log(methodName());
		t0 = System.currentTimeMillis();
	}

	public String after() {
		log("<br>et: ", System.currentTimeMillis() - t0, "ms");
		System.err.println("done");
		String output = sb.toString();
		sb.setLength(0);
		return output;
	}

	String methodName() { // called from before
		return Thread.currentThread().getStackTrace()[3].getMethodName();
	}

	public DemoApplication(@Autowired CouchbaseTemplate template) {
		this.template = template;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@GetMapping("/")
	public String index() {
		before();
		return after();
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "airlineId", defaultValue = "airline_10123") String airlineId) {
		before();
		airlineService.hello(airlineId);
		return after();
	}

	@GetMapping("/queryAndCountCommitted")
	public String queryAndCountCommitted() {
		before();
		airlineService.queryAndCountCommitted(airlineId);
		assertEquals(2, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).all().size(),
				"should have found committed documents");
		assertEquals(2l, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).count(),
				"should have found committed documents");
		return after();
	}

	@GetMapping("/queryAndCountCommittedRepo")
	public String queryAndCountCommittedRepo() {
		before();
		airlineService.queryAndCountCommittedRepo(airlineId);
		assertEquals(2, airlineRepo.withOptions(requestPlus).findAll().size(), "should have found committed documents");
		assertEquals(2l, airlineRepo.withOptions(requestPlus).count(), "should have found committed documents");
		return after();
	}

	@GetMapping("/queryAndCountUncommitted")
	public String queryAndCountUncommitted() {
		before();
		try {
			airlineService.queryAndCountUncommitted(airlineId);
		} catch (TransactionFailed e) {
			System.err.println(e);
			log("caught: ", e);
			if (!(e.getCause() instanceof PoofException)) {
				throw new RuntimeException(e.getCause());
			}
		}
		assertEquals(0, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).all().size(),
				"should not have found uncommitted documents");
		assertEquals(0l, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).count(),
				"should not have found uncommitted documents");
		return after();
	}

	@GetMapping("/queryAndCountUncommittedRepo")
	public String queryAndCountUncommittedRepo() {
		before();
		try {
			airlineService.queryAndCountUncommittedRepo(airlineId);
		} catch (TransactionFailed e) {
			System.err.println(e);
			log("caught: ", e);
			if (!(e.getCause() instanceof PoofException)) {
				throw new RuntimeException(e.getCause());
			}
		}
		assertEquals(0, airlineRepo.withOptions(requestPlus).findAll().size(), "should not have found committed documents");
		assertEquals(0l, airlineRepo.withOptions(requestPlus).count(), "should not have found committed documents");
		return after();
	}

	@GetMapping("/insertCommit")
	public String insertCommit() {
		before();
		airlineService.insertCommit(airlineId);
		assertNotNull(template.findById(Airline.class).one(airlineId), "not committed");
		return after();
	}

	@GetMapping("/insertCommitRepo")
	public String insertCommitRepo() {
		before();
		airlineService.insertCommitRepo(airlineId);
		assertTrue( airlineRepo.findById(airlineId).isPresent(), "not committed");
		assertNotNull(airlineRepo.findById(airlineId).get(), "not committed");
		return after();
	}

	@GetMapping("/replaceCommit")
	public String replaceCommit() {
		before();
		Airline a = (Airline)log("insert: "+template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
		airlineService.replaceCommit(airlineId);
		assertEquals(yourAirline, template.findById(Airline.class).one(airlineId).name, "not commited");
		return after();
	}

	@GetMapping("/replaceCommitRepo")
	public String replaceCommitRepo() {
		before();
		Airline a = (Airline)log("insert: ",airlineRepo.save(new Airline(airlineId, myAirline)));
		airlineService.replaceCommitRepo(airlineId);
		assertEquals(yourAirline, airlineRepo.findById(airlineId).get().name, "not commited");
		return after();
	}

	@GetMapping("/replaceCommitNoKey")
	public String replaceCommitNoKey() {
		before();
		AirlineNoKey a = (AirlineNoKey)log("insert: ",template.insertById(AirlineNoKey.class).one(new AirlineNoKey(airlineId, myAirline)));
		airlineService.replaceCommitNoKey(airlineId);
		assertEquals(yourAirline, template.findById(AirlineNoKey.class).one(airlineId).name, "not commited");
		return after();
	}

	@GetMapping("/replaceCommitNoKeyRepo")
	public String replaceCommitNoKeyRepo() {
		before();
		AirlineNoKey a = (AirlineNoKey)log("insert: ", airlineNoKeyRepo.save(new AirlineNoKey(airlineId, myAirline)));
		airlineService.replaceCommitNoKeyRepo(airlineId);
		assertEquals(yourAirline, airlineRepo.findById(airlineId).get().name, "not committed");
		return after();
	}

	@GetMapping("/replaceCommitImmutable")
	public String replaceCommitImmutable() {
		before();
		Airline a = (Airline)log("insert: ", template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
		airlineService.replaceCommitImmutable(airlineId);
		assertEquals(yourAirline, template.findById(Airline.class).one(airlineId).name, "not committed");
		return after();
	}

	@GetMapping("/replaceCommitImmutableRepo")
	public String replaceCommitImmutableRepo() {
		before();
		Airline a = (Airline)log("insert: ", airlineRepo.save(new Airline(airlineId, myAirline)));
		airlineService.replaceCommitImmutableRepo(airlineId);
		assertEquals(yourAirline, airlineRepo.findById(airlineId).get().name, "not commited");
		return after();
	}

	@GetMapping("/removeCommit")
	public String removeCommit() {
		before();
		Airline a = (Airline)log("insert: ", template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
		airlineService.removeCommit(airlineId);
		assertNull(template.findById(Airline.class).one(airlineId), "not committed");
		return after();
	}

	@GetMapping("/removeCommitRepo")
	public String removeCommitRepo() {
		before();
		Airline a = (Airline)log("insert: ", airlineRepo.save(new Airline(airlineId, myAirline)));
		airlineService.removeCommitRepo(airlineId);
		assertFalse((Boolean) log("isPresent: ",airlineRepo.findById(airlineId).isPresent()), "not committed");
		return after();
	}

	@GetMapping("/insertRollback")
	public String insertRollback() {
		before();
		try {
			airlineService.insertRollback(airlineId);
		} catch (TransactionFailed e) {
			System.err.println(e);
			log("caught: ", e);
			if (!(e.getCause() instanceof PoofException)) {
				log(e.getCause());
			}
		}
		assertNull(template.findById(Airline.class).one(airlineId), "not rolled back");
		return after();
	}

	@GetMapping("/insertRollbackRepo")
	public String insertRollbackRepo() {
		before();
		try {
			airlineService.insertRollbackRepo(airlineId);
		} catch (TransactionFailed e) {
			System.err.println(e);
			log("caught: ", e);
			if (!(e.getCause() instanceof PoofException)) {
				log(e.getCause());
			}
		}
		assertFalse((Boolean)log("isPresent: ",airlineRepo.findById(airlineId).isPresent()), "not rolled back");
		return after();
	}

	@GetMapping("/replaceRollback")
	public String replaceRollback() {
		before();
		Airline a = template.insertById(Airline.class).one(new Airline(airlineId, myAirline));
		try {
			airlineService.replaceRollback(airlineId);
		} catch (TransactionFailed e) {
			System.err.println(e);
			log("caught: ", e);
			if (!(e.getCause() instanceof PoofException)) {
				log(e.getCause());
			}
		}
		assertEquals(myAirline, template.findById(Airline.class).one(airlineId).name, "not rolled back");
		return after();
	}

	@GetMapping("/replaceRollbackRepo")
	public String replaceRollbackRepo() {
		before();
		Airline a = airlineRepo.save(new Airline(airlineId, myAirline));
		try {
			airlineService.replaceRollbackRepo(airlineId);
		} catch (TransactionFailed e) {
			System.err.println(e);
			log("caught: ", e);
			if (!(e.getCause() instanceof PoofException)) {
				log(e.getCause());
			}
		}
		assertEquals(myAirline, airlineRepo.findById(airlineId).get().name, "not rolled back");
		return after();
	}

	@GetMapping("/removeRollback")
	public String removeRollback() {
		before();
		Airline a = template.insertById(Airline.class).one(new Airline(airlineId, myAirline));
		try {
			airlineService.removeRollback(airlineId);
		} catch (TransactionFailed e) {
			System.err.println(e);
			log("caught: ", e);
			if (!(e.getCause() instanceof PoofException)) {
				log(e.getCause());
			}
		}
		assertNotNull(template.findById(Airline.class).one(airlineId), "not rolled back");
		return after();
	}

	@GetMapping("/removeRollbackRepo")
	public String removeRollbackRepo() {
		before();
		Airline a = airlineRepo.save(new Airline(airlineId, myAirline));
		try {
			airlineService.removeRollbackRepo(airlineId);
		} catch (TransactionFailed e) {
			System.err.println(e);
			log("caught: ", e);
			if (!(e.getCause() instanceof PoofException)) {
				log(e.getCause());
			}
		}
		assertTrue((Boolean)log("isPresent: ",airlineRepo.findById(airlineId).isPresent()), "not rolled back");
		return after();
	}

	private <T> void removeAll(Class<T> clazz) {
		template.removeByQuery(clazz).withConsistency(REQUEST_PLUS).all();
		List<T> list = template.findByQuery(clazz).withConsistency(REQUEST_PLUS).all();
		if (!list.isEmpty()) {
			throw new RuntimeException("there are some " + clazz + " left over");
		}

	}

	public static class PoofException extends RuntimeException {}

	StringBuffer displayText(StringBuffer sb) {
		String urlPrefix = "http://localhost:8080";
		sb.append("<table>");
		for (Method m : this.getClass().getMethods()) {

			GetMapping a = m.getAnnotation(GetMapping.class);
			if (a != null) {
				sb.append("<tr>");
				sb.append("<td>");
				sb.append("<a href=\"");
				sb.append(urlPrefix + a.value()[0]);
				sb.append("\">");
				sb.append(a.value()[0]);
				sb.append("</a>");
				sb.append("</td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table>");
		return sb;
	}

	public Object log(Object... args) {
		if(args.length > 0 && args[0] instanceof String && !((String)args[0]).startsWith("<")){
			sb.append("<br>__: ");
		}
		for (Object s : args) {
			sb.append(s);
		}
		sb.append("\n");
		for(Object a:args){
			if(! (a instanceof String)){
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

	private void assertNotEquals(Object a, Object b, String s) {
		try {
			log("found: ",b);
			org.junit.jupiter.api.Assertions.assertNotEquals(a, b, s);
		} catch (AssertionError e) {
			log("<br><H2>", e.getMessage());
		}
	}

	private void assertNull(Object a, String s) {
		try {
			log("found: ",a);
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
			log("found: ",a);
			org.junit.jupiter.api.Assertions.assertNotNull(a, s);
		} catch (AssertionError e) {
			log("<br><H2>", e.getMessage());
		}
	}

	private void assertEquals(Object a, Object b, String s) {
		try {
			log("found: ",b);
			org.junit.jupiter.api.Assertions.assertEquals(a, b, s);
		} catch (AssertionError e) {
			log("<br><H2>", e.getMessage());
		}
	}

}
