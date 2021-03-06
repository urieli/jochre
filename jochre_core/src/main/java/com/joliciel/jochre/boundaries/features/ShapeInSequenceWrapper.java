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
package com.joliciel.jochre.boundaries.features;

import com.joliciel.jochre.boundaries.ShapeInSequence;

/**
 * A simple wrapper for a shapeInSequence, useful to allow multiple shape
 * contexts to act all as a ShapeInSequenceWrapper for Shape features.
 * 
 * @author Assaf Urieli
 *
 */
public interface ShapeInSequenceWrapper {
  public ShapeInSequence getShapeInSequence();
}
