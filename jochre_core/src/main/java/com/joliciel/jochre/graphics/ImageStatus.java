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
package com.joliciel.jochre.graphics;

/**
 * @author Assaf Urieli
 *
 * An image's validation status.
 */
public enum ImageStatus  {
    TRAINING_NEW("training_new",1),
    TRAINING_VALIDATED("training_validated",2),
    TRAINING_HELD_OUT("training_held_out",3),
    TRAINING_TEST("training_test",4),
    AUTO_NEW("auto_new",5),
    AUTO_VALIDATED ("auto_validated",6);
    

    private String code;
    private int id;
    private ImageStatus(String code, int id) {
        this.code = code;
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }

    public int getId() {
        return id;
    } 

    public static ImageStatus forId(int id) throws IllegalArgumentException  {
        for (ImageStatus imageStatus : ImageStatus.values()) {
            if (imageStatus.getId()==id)
                return imageStatus;
        }
        throw new IllegalArgumentException("No imageStatus found for id " + id);
    }
    
    public static ImageStatus forCode(String code) throws IllegalArgumentException {
        if (code==null) return null;
        for (ImageStatus imageStatus : ImageStatus.values()) {
            if (imageStatus.getCode().equals(code))
                return imageStatus;
        }
        throw new IllegalArgumentException("No imageStatus found for code " + code);
    }

	@Override
	public String toString() {
		if (this.equals(TRAINING_NEW))
			return "training - new";
		else if (this.equals(TRAINING_VALIDATED))
			return "training - validated";
		else if (this.equals(TRAINING_HELD_OUT))
			return "training - held-out";
		else if (this.equals(TRAINING_TEST))
			return "training - test";
		else if (this.equals(AUTO_NEW))
			return "auto - new";
		else if (this.equals(AUTO_VALIDATED))
			return "auto - validated";
		else
			return "";
	}
    
    
}