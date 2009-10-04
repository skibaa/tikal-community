package org.springframework.samples.petclinic.datastore;

import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;

public class DatastorePet extends Pet {
	public DatastorePet(Owner owner) {
		setOwner(owner);
	}
}
