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
package com.joliciel.jochre;

public abstract class EntityImpl implements EntityInternal {
    private int id;
 
    public boolean isNew() {
        return (id==0);
    }

    public final  int getId() {
        return id;
    }

    public final void setId(int id) {
        this.id = id;
    }

    public final void save() {
        this.saveInternal();
    }
    
    public abstract void saveInternal();

	@Override
	public int hashCode() {
		if (id==0)
			return super.hashCode();
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityImpl other = (EntityImpl) obj;
		if (id != other.id)
			return false;
		if (id==0)
			return super.equals(obj);
		return true;
	}
}
