package com.example.demo.service;

import static com.couchbase.client.java.query.QueryScanConsistency.REQUEST_PLUS;

import java.util.Optional;

import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Airline;
import com.example.demo.AirlineNoKey;
import com.example.demo.DemoApplication;
import com.example.demo.MyAirlineNoKeyRepository;
import com.example.demo.MyAirlineRepository;

@Service
public class AirlineService {

	CouchbaseTemplate template;
	MyAirlineRepository airlineRepo;
	MyAirlineNoKeyRepository airlineNoKeyRepo;

	String myAirline = DemoApplication.myAirline;
	String yourAirline = DemoApplication.yourAirline;
	StringBuffer sb;

	public AirlineService(CouchbaseTemplate template, MyAirlineRepository airlineRepo,
			MyAirlineNoKeyRepository airlineNoKeyRepository) {
		this.template = template;
		this.airlineRepo = airlineRepo;
		this.airlineNoKeyRepo = airlineNoKeyRepository;
	}

	public StringBuffer setSb(StringBuffer sb){
		return this.sb = sb;
	}

	private void throwPoofException() {
		log("threw: PoofException");
		throw new DemoApplication.PoofException();
	}

	private void assertEquals(Object a, Object b, String s) {
		try {
			log("found: ", b);
			org.junit.jupiter.api.Assertions.assertEquals(a, b, s);
		} catch (AssertionError e) {
			log("<H2>", e.getMessage(), "</H2>");
		}
	}

	public Object log(Object... args) {
			sb.append("<br>tx: ");
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

	@Transactional
	public void hello(String airlineId) {
		log("found:", template.findById(Airline.class).one(airlineId));
	}

	@Transactional
	public void queryAndCountCommitted(String airlineId) {
		log("insert:", template.insertById(Airline.class).one(new Airline(airlineId, "My Airline")));
		log("insert:",  template.insertById(Airline.class).one(new Airline(airlineId + "_your", "Your Airline")));
	}

	@Transactional
	public void queryAndCountCommittedRepo(String airlineId) {
		log("insert:",  airlineRepo.save(new Airline(airlineId, myAirline)));
		log("insert:", airlineRepo.save(new Airline(airlineId + "_your", yourAirline)));
	}

	@Transactional
	public void queryAndCountUncommitted(String airlineId) {
		log("insert for rollback:", template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
		log("insert for rollback:",template.insertById(Airline.class).one(new Airline(airlineId + "_your", yourAirline)));
		assertEquals(2, template.findByQuery(Airline.class).withConsistency(REQUEST_PLUS).all().size(),
				"should have found uncommitted documents");
		assertEquals(2l, template.findByQuery(Airline.class).count(), "should have found uncommitted documents");
		throwPoofException();
	}

	@Transactional
	public void queryAndCountUncommittedRepo(String airlineId) {
		log("insert for rollback:", airlineRepo.save(new Airline(airlineId, myAirline)));
		log("insert for rollback:", airlineRepo.save(new Airline(airlineId + "_your", yourAirline)));
		assertEquals(2, airlineRepo.findAll().size(), "should have found uncommitted documents");
		assertEquals(2l, airlineRepo.count(), "should have found uncommitted documents");
		throwPoofException();
	}

	@Transactional
	public void insertCommit(String airlineId) {
		log("insert:",  template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
	}

	@Transactional
	public void insertCommitRepo(String airlineId) {
		log("insert:", airlineRepo.save(new Airline(airlineId, myAirline)));
	}

	@Transactional
	public void replaceCommit(String airlineId) {
		Airline b;
		log("found:", b=template.findById(Airline.class).one(airlineId));
		b.name = yourAirline;
		log("replace:",template.replaceById(Airline.class).one(b));
	}

	@Transactional
	public void replaceCommitRepo(String airlineId) {
		Optional<Airline> b;
		log("found:", (b=airlineRepo.findById(airlineId)).get());
		b.get().name = yourAirline;
		log("replace:",airlineRepo.save(b.get()));
	}

	@Transactional
	public void replaceCommitNoKey(String airlineId) {
		AirlineNoKey b;
		log("found: ", b=template.findById(AirlineNoKey.class).one(airlineId));
		b.name = yourAirline;
		log("replace: ",template.replaceById(AirlineNoKey.class).one(b));
	}

	@Transactional
	public void replaceCommitNoKeyRepo(String airlineId) {
		AirlineNoKey b =(AirlineNoKey)log("found", (b=airlineNoKeyRepo.findById(airlineId).get()));
		b.name = yourAirline;
		log("replace: ",airlineNoKeyRepo.save(b));
	}

	@Transactional
	public void replaceCommitImmutable(String airlineId) {
		Airline b = (Airline)log("found: ",template.findById(Airline.class).one(airlineId));
		Airline c = (Airline)log("replace: ",template.replaceById(Airline.class).one(b.withName(yourAirline)));
	}

	@Transactional
	public void replaceCommitImmutableRepo(String airlineId) {
		Airline b = (Airline) log("found: ",airlineRepo.findById(airlineId).get());
		Airline c = (Airline) log("replace: ",airlineRepo.save(b.withName(yourAirline)));
	}

	@Transactional
	public void removeCommit(String airlineId) {
		Airline b = (Airline)log("found: ",template.findById(Airline.class).one(airlineId));
		template.removeById(Airline.class).one((Airline)log("delete: ",b));
	}

	@Transactional
	public void removeCommitRepo(String airlineId) {
		Airline b = (Airline) log("found: ",airlineRepo.findById(airlineId).get());
		airlineRepo.delete((Airline) log("delete: ",b));
	}

	@Transactional
	public void insertRollback(String airlineId) {
		Airline a = (Airline)log("insert: ",template.insertById(Airline.class).one(new Airline(airlineId, myAirline)));
		throwPoofException();
	}

	@Transactional
	public void insertRollbackRepo(String airlineId) {
		Airline a =(Airline)log("found: ", airlineRepo.save(new Airline(airlineId, myAirline)));
		throwPoofException();
	}

	@Transactional
	public void replaceRollback(String airlineId) {
		Airline b = (Airline)log("found: ", template.findById(Airline.class).one(airlineId));
		b.name = yourAirline;
		Airline c = (Airline)log("replace: ", template.replaceById(Airline.class).one(b));
		throwPoofException();
	}

	@Transactional
	public void replaceRollbackRepo(String airlineId) {
		Airline b = (Airline)log("found: ",airlineRepo.findById(airlineId).get());
		b.name = yourAirline;
		Airline c = (Airline)log("replace: ",airlineRepo.save(b));
		throwPoofException();
	}

	@Transactional
	public void removeRollback(String airlineId) {
		Airline b = (Airline)log("found: ",template.findById(Airline.class).one(airlineId));
		template.removeById(Airline.class).one((Airline)log("delete: ",b));
		throwPoofException();
	}

	@Transactional
	public void removeRollbackRepo(String airlineId) {
		Airline b = (Airline)log("found: ",airlineRepo.findById(airlineId).get());
		airlineRepo.delete((Airline) log("delete: ",b));
		throwPoofException();
	}
}
