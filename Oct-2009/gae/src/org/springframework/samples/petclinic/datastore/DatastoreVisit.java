package org.springframework.samples.petclinic.datastore;

import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;

public class DatastoreVisit extends Visit {
	public DatastoreVisit(Pet pet) {
		setPet(pet);
	}
}
