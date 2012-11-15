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

import java.util.Locale;

/**
 * A class storing session-wide reference data.
 * @author Assaf Urieli
 *
 */
public class JochreSession {
	private static ThreadLocal<Locale> localeHolder = new ThreadLocal<Locale>();
	private static ThreadLocal<Double> junkConfidenceThresholdHolder = new ThreadLocal<Double>();
	
	public static Locale getLocale() {
		return localeHolder.get();
	}
	
	public static void setLocale(Locale locale) {
		localeHolder.set(locale);
	}
	
	/**
	 * The average confidence below which a paragraph is considered to be junk,
	 * when considering all of its letters.
	 * @return
	 */
	public static double getJunkConfidenceThreshold() {
		Double junkConfidenceThreshold = junkConfidenceThresholdHolder.get();
		if (junkConfidenceThreshold==null)
			return 0.75;
		return junkConfidenceThreshold;
	}
	
	public static void setJunkConfidenceThreshold(double junkConfidenceThreshold) {
		junkConfidenceThresholdHolder.set(junkConfidenceThreshold);
	}
}
