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
package com.joliciel.jochre.yiddish.lexicons;

public class YiddishTextUtils {

	public static String removeEndForm(String form) {
		String endForm = form;
		if (endForm.endsWith("ם")) {
			endForm = endForm.substring(0, endForm.length()-1) + "מ";
		} else if (endForm.endsWith("ן")) {
			endForm = endForm.substring(0, endForm.length()-1) + "נ";
		} else if (endForm.endsWith("ץ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "צ";
		} else if (endForm.endsWith("ף")) {
			endForm = endForm.substring(0, endForm.length()-1) + "פֿ";
		} else if (endForm.endsWith("ך")) {
			endForm = endForm.substring(0, endForm.length()-1) + "כ";
		}
		
		return endForm;
		
	}
	
	public static String getEndForm(String form) {
		String endForm = form;
		if (endForm.endsWith("מ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ם";
		} else if (endForm.endsWith("נ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ן";
		} else if (endForm.endsWith("צ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ץ";
		} else if (endForm.endsWith("פֿ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ף";
		} else if (endForm.endsWith("כ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ך";
		}
		
		return endForm;
		
	}

	public static boolean endsWithVowel(String form) {
		if (form.endsWith("י")||form.endsWith("ו")||form.endsWith("אַ")||form.endsWith("אָ")||form.endsWith("ע")
				||form.endsWith("ױ")||form.endsWith("ײ")||form.endsWith("ײַ")
				||form.endsWith("יִ")||form.endsWith("וּ"))
			return true;
		return false;
	}
}
