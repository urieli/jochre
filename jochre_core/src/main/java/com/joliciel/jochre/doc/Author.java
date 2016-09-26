///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.doc;

import com.joliciel.jochre.Entity;

/**
 * The author (or co-author) of a document being scanned.
 * 
 * @author Assaf Urieli
 *
 */
public class Author implements Entity {
	private DocumentServiceInternal documentServiceInternal;
	private String firstName;
	private String lastName;
	private String firstNameLocal;
	private String lastNameLocal;
	private int id;

	@Override
	public void save() {
		this.documentServiceInternal.saveAuthor(this);
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstNameLocal() {
		return firstNameLocal;
	}

	public void setFirstNameLocal(String firstNameLocal) {
		this.firstNameLocal = firstNameLocal;
	}

	public String getLastNameLocal() {
		return lastNameLocal;
	}

	public void setLastNameLocal(String lastNameLocal) {
		this.lastNameLocal = lastNameLocal;
	}

	public DocumentServiceInternal getDocumentServiceInternal() {
		return documentServiceInternal;
	}

	public void setDocumentServiceInternal(DocumentServiceInternal documentServiceInternal) {
		this.documentServiceInternal = documentServiceInternal;
	}

	public String getFullName() {
		return this.firstName + " " + this.lastName;
	}

	public String getFullNameLocal() {
		return this.firstNameLocal + " " + this.lastNameLocal;
	}

	@Override
	public int hashCode() {
		if (this.id == 0)
			return super.hashCode();
		else
			return this.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (this.id == 0) {
			return super.equals(obj);
		} else {
			Author other = (Author) obj;
			return (this.getId() == other.getId());
		}
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

}
