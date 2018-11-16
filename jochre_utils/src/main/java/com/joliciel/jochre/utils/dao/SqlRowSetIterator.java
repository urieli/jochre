///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Assaf Urieli
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
package com.joliciel.jochre.utils.dao;

import java.util.Iterator;

import org.springframework.jdbc.support.rowset.SqlRowSet;

public class SqlRowSetIterator<T> implements Iterator<T> {
  SqlRowSet rowSet;
  SqlRowSetMapper<T> mapper;
  boolean checkedNext = false;
  boolean hasNext = false;
  
  public SqlRowSetIterator(SqlRowSet rowSet, SqlRowSetMapper<T> mapper) {
    this.rowSet = rowSet;
    this.mapper = mapper;
  }
  @Override
  public boolean hasNext() {
    if (!checkedNext) {
      hasNext = rowSet.next();
    }
    return hasNext;
  }

  @Override
  public T next() {
    return mapper.mapRow(rowSet);
  }

  @Override
  public void remove() {
    throw new RuntimeException("Unsupported opertion: Iterator.remove()");
  }

}
