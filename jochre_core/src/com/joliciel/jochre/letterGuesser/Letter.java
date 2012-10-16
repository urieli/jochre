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
package com.joliciel.jochre.letterGuesser;

import com.joliciel.talismane.machineLearning.Outcome;

/**
 * A letter that can be guessed by the letter guesser.
 * @author Assaf Urieli
 *
 */
public class Letter implements Outcome {
	private String string;
	
	public Letter(String string) {
		this.string = string;
	}
	
	public String getString() {
		return string;
	}
	
	@Override
	public String getCode() {
		return string;
	}

	@Override
	public String toString() {
		return string;
	}

}
