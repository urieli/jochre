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
package com.joliciel.jochre.security;

/**
 * @author Assaf Urieli
 *
 * A user's role in the system.
 */
public enum UserRole  {
    GUEST("guest",1),
    ADMIN("administrator",2);
    

    private String code;
    private int id;
    private UserRole(String code, int id) {
        this.code = code;
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }

    public int getId() {
        return id;
    } 

    public static UserRole forId(int id) throws IllegalArgumentException  {
        for (UserRole userRole : UserRole.values()) {
            if (userRole.getId()==id)
                return userRole;
        }
        throw new IllegalArgumentException("No userRole found for id " + id);
    }
    
    public static UserRole forCode(String code) throws IllegalArgumentException {
        if (code==null) return null;
        for (UserRole userRole : UserRole.values()) {
            if (userRole.getCode().equals(code))
                return userRole;
        }
        throw new IllegalArgumentException("No userRole found for code " + code);
    }
}