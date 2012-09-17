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
package com.joliciel.jochre.text;

/**
 * @author Assaf Urieli
 *
 * A format for exporting text.
 */
public enum TextFormat  {
    PLAIN("plain",1),
    XHTML("xhtml",2);
    

    private String code;
    private int id;
    private TextFormat(String code, int id) {
        this.code = code;
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }

    public int getId() {
        return id;
    } 

    public static TextFormat forId(int id) throws IllegalArgumentException  {
        for (TextFormat textFormat : TextFormat.values()) {
            if (textFormat.getId()==id)
                return textFormat;
        }
        throw new IllegalArgumentException("No text format found for id " + id);
    }
    
    public static TextFormat forCode(String code) throws IllegalArgumentException {
        if (code==null) return null;
        for (TextFormat textFormat : TextFormat.values()) {
            if (textFormat.getCode().equals(code))
                return textFormat;
        }
        throw new IllegalArgumentException("No text format found for code " + code);
    }
}