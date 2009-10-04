package org.springframework.samples.petclinic.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.samples.petclinic.Clinic;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.PetType;
import org.springframework.samples.petclinic.Specialty;
import org.springframework.samples.petclinic.Vet;
import org.springframework.samples.petclinic.Visit;
import org.springframework.stereotype.Repository;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

@Repository
public class DatastoreClinic implements Clinic, InitializingBean {
	
	@Autowired
	DatastoreService datastore;
	
	public Collection<Owner> findOwners(String lastName)
			throws DataAccessException {
		
		
		ArrayList<Owner> res=new ArrayList<Owner>();

		Query q=new Query("Owner");
		q.addFilter("last_name", Query.FilterOperator.EQUAL, lastName);
		for (Entity e : datastore.prepare(q).asIterable()) {
			try {
				Owner o=ownerFromEntity(e);
				res.add(o);
			} catch (EntityNotFoundException ex) {
				throw new DataRetrievalFailureException("Cannot retrieve data", ex);
			}
		}
		return res;
	}

	private Owner ownerFromEntity(Entity e) throws EntityNotFoundException {
		Owner o=new Owner();
		o.setId(e.getKey().getId());
		o.setAddress((String)e.getProperty("address"));
		o.setCity((String)e.getProperty("city"));
		o.setFirstName((String)e.getProperty("first_name"));
		o.setLastName((String)e.getProperty("last_name"));
		o.setTelephone((String)e.getProperty("telephone"));

		Query pq=new Query("Pet");
		pq.addFilter("owner_key", Query.FilterOperator.EQUAL, e.getKey());
		
		for (Entity pe: datastore.prepare(pq).asIterable()) {
			Pet p=petFromEntity(pe, o);
			o.addPet(p);
		}
		
		return o;
	}

	private Pet petFromEntity(Entity e, Owner owner) throws EntityNotFoundException {
		if (owner==null) {
			Entity oe=datastore.get((Key) e.getProperty("owner_key"));
			owner=ownerFromEntity(oe);
		}
		Pet res=new DatastorePet(owner);
		res.setId(e.getKey().getId());
		res.setBirthDate((Date)e.getProperty("birth_date"));
		res.setName((String)e.getProperty("name"));
		
		Entity te;
		te = datastore.get((Key) e.getProperty("type_key"));
		PetType type=petTypeFromEntity(te);
		res.setType(type);
		
		Query q=new Query("Visit");
		q.addFilter("pet_key", Query.FilterOperator.EQUAL, e.getKey());
		
		for(Entity ve: datastore.prepare(q).asIterable()) {
			res.addVisit(visitFromEntity(ve, res));
		}
		
		return res;
	}

	private Visit visitFromEntity(Entity e, Pet pet) throws EntityNotFoundException {
		if(pet==null) {
			Entity pe=datastore.get((Key) e.getProperty("pet_key"));
			pet=petFromEntity(pe, null);
		}
		Visit res=new DatastoreVisit(pet);
		
		res.setId(e.getKey().getId());
		res.setDate((Date) e.getProperty("date"));
		res.setDescription((String) e.getProperty("description"));
		return res;
	}

	private PetType petTypeFromEntity(Entity e) {
		PetType res=new PetType();
		res.setId(e.getKey().getId());
		res.setName((String) e.getProperty("name"));
		
		return res;
	}

	public Collection<PetType> getPetTypes() throws DataAccessException {
		ArrayList<PetType> res=new ArrayList<PetType>();
		Query q=new Query("PetType");
		for(Entity e: datastore.prepare(q).asIterable()) {
			res.add(petTypeFromEntity(e));
		}
		return res;
	}

	public Collection<Vet> getVets() throws DataAccessException {
		ArrayList<Vet> res=new ArrayList<Vet>();
		
		Query q=new Query("Vet");
		for(Entity e: datastore.prepare(q).asIterable()) {
			Vet v;
			try {
				v = vetFromEntity(e);
			} catch (EntityNotFoundException ex) {
				throw new DataRetrievalFailureException("Cannot retrieve data", ex);
			}
			res.add(v);
		}
		
		return res;
	}

	private Vet vetFromEntity(Entity e) throws EntityNotFoundException {
		Vet res=new Vet();
		
		res.setId(e.getKey().getId());
		res.setFirstName((String) e.getProperty("first_name"));
		res.setLastName((String) e.getProperty("last_name"));
		
		Object specs=e.getProperty("specialty_keys");
		if (specs!=null) {
			for(Key sk: (List<Key>) specs) {
				Entity se=datastore.get(sk);
				res.addSpecialty(specialtyFromEntity(se));
			}
		}
		return res;
	}

	private Specialty specialtyFromEntity(Entity e) {
		Specialty res=new Specialty();
		res.setId(e.getKey().getId());
		res.setName((String) e.getProperty("name"));
		return res;
	}

	public Owner loadOwner(long id) throws DataAccessException {
		Key key=KeyFactory.createKey("Owner", id);
		try {
			return ownerFromEntity(datastore.get(key));
		} catch (EntityNotFoundException ex) {
			throw new DataRetrievalFailureException("Cannot retrieve data", ex);
		}
	}

	public Pet loadPet(long id) throws DataAccessException {
		Key key=KeyFactory.createKey("Pet", id);
		try {
			return petFromEntity(datastore.get(key), null);
		} catch (EntityNotFoundException ex) {
			throw new DataRetrievalFailureException("Cannot retrieve data", ex);
		}
	}

	public void storeOwner(Owner owner) throws DataAccessException {
		Entity e=new Entity("Owner");
		e.setProperty("address", owner.getAddress());
		e.setProperty("city", owner.getCity());
		e.setProperty("first_name", owner.getFirstName());
		e.setProperty("last_name", owner.getLastName());
		e.setProperty("telephone", owner.getTelephone());
		Key key=datastore.put(e);
		owner.setId(key.getId());
		
		for (Pet pet: owner.getPets()) {
			storePet(pet);
		}
	}

	public void storePet(Pet pet) throws DataAccessException {
		Entity e=new Entity("Pet");
		
		e.setProperty("name", pet.getName());
		e.setProperty("birth_date", pet.getBirthDate());
		e.setProperty("owner_key", KeyFactory.createKey("Owner", pet.getOwner().getId()));
		e.setProperty("type_key", KeyFactory.createKey("PetType", pet.getType().getId()));
		
		Key key=datastore.put(e);
		pet.setId(key.getId());
		
		for(Visit visit: pet.getVisits()) {
			storeVisit(visit);
		}
	}

	public void storeVisit(Visit visit) throws DataAccessException {
		Entity e=new Entity("Visit");
		
		e.setProperty("date", visit.getDate());
		e.setProperty("description", visit.getDescription());
		e.setProperty("pet_key", KeyFactory.createKey("Pet", visit.getPet().getId()));
		
		Key key=datastore.put(e);
		visit.setId(key.getId());
	}

	public void afterPropertiesSet() throws Exception {
		populateDataIfEmpty();
	}

	private void populateDataIfEmpty() {
		Query q=new Query("PetType");
		
		List l=datastore.prepare(q).asList(FetchOptions.Builder.withLimit(10));
		
		if(l.size()==0) {
			//database is empty, populate it
			Entity e;
			e=new Entity("PetType");
			e.setProperty("name", "cat");
			datastore.put(e);
			e=new Entity("PetType");
			e.setProperty("name", "dog");
			datastore.put(e);
			e=new Entity("PetType");
			e.setProperty("name", "lizard");
			datastore.put(e);
			e=new Entity("PetType");
			e.setProperty("name", "snake");
			datastore.put(e);
			e=new Entity("PetType");
			e.setProperty("name", "bird");
			datastore.put(e);
			e=new Entity("PetType");
			e.setProperty("name", "hamster");
			datastore.put(e);
			e=new Entity("Specialty");
			e.setProperty("name", "radiology");
			Key s1=datastore.put(e);
			e=new Entity("Specialty");
			e.setProperty("name", "surgery");
			Key s2=datastore.put(e);
			e=new Entity("Specialty");
			e.setProperty("name", "dentistry");
			Key s3=datastore.put(e);
			e=new Entity("Vet");
			e.setProperty("first_name", "James");
			e.setProperty("last_name", "Carter");
			datastore.put(e);
			ArrayList<Key> keyList;
			e=new Entity("Vet");
			e.setProperty("first_name", "Helen");
			e.setProperty("last_name", "Leary");
			keyList=new  ArrayList<Key>();
			keyList.add(s1);
			e.setProperty("specialty_keys", keyList);
			datastore.put(e);
			e=new Entity("Vet");
			e.setProperty("first_name", "Linda");
			e.setProperty("last_name", "Douglas");
			keyList=new  ArrayList<Key>();
			keyList.add(s2);
			keyList.add(s3);
			e.setProperty("specialty_keys", keyList);
			datastore.put(e);
		}
	}

}