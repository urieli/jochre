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
package com.joliciel.jochre.boundaries;

import java.util.HashMap;
import java.util.Map;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.GraphicsDao;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;
import com.joliciel.talismane.machineLearning.features.FeatureResult;

/**
 * A split is a split-point separating a shape into two sub-shapes. It is simply
 * defined by the parent shape and an x-coordinate indicating where the shape is
 * to be split.
 * 
 * @author Assaf Urieli
 *
 */
public class Split implements Entity, ShapeWrapper {
  private int shapeId;
  private Shape shape;
  private int position;
  private boolean dirty;
  private int id;

  private Map<String, FeatureResult<?>> featureResults = new HashMap<String, FeatureResult<?>>();

  private final JochreSession jochreSession;

  Split(JochreSession jochreSession) {
    this.jochreSession = jochreSession;
  }

  public Split(Shape shape, JochreSession jochreSession) {
    this.jochreSession = jochreSession;
    this.shape = shape;
    this.shapeId = shape.getId();
  }

  public int getShapeId() {
    return shapeId;
  }

  void setShapeId(int shapeId) {
    if (this.shapeId != shapeId) {
      this.shapeId = shapeId;
      this.setDirty(true);
    }
  }

  /**
   * X-coordinate of the split, where zero is the left-most coordinate of the
   * shape.
   */
  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    if (this.position != position) {
      this.position = position;
      this.dirty = true;
    }
  }

  @Override
  public Shape getShape() {
    if (shape == null && shapeId != 0) {
      GraphicsDao graphicsDao = GraphicsDao.getInstance(jochreSession);
      shape = graphicsDao.loadShape(shapeId);
    }
    return shape;
  }

  void setShape(Shape shape) {
    this.shape = shape;
    if (shape != null)
      this.setShapeId(shape.getId());
    else
      this.setShapeId(0);
  }

  @Override
  public void save() {
    if (this.dirty) {
      BoundaryDao boundaryDao = BoundaryDao.getInstance(jochreSession);
      boundaryDao.saveSplit(this);
    }
  }

  /**
   * Delete this split from persistent store.
   */
  public void delete() {
    if (this.id != 0) {
      BoundaryDao boundaryDao = BoundaryDao.getInstance(jochreSession);
      boundaryDao.deleteSplit(this);
    }
  }

  boolean isDirty() {
    return dirty;
  }

  void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  @Override
  public String toString() {
    return "Split [shape=" + this.getShape() + ", position=" + position + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + position;
    result = prime * result + ((shape == null) ? 0 : shape.hashCode());
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
    Split other = (Split) obj;
    if (position != other.position)
      return false;
    if (shape == null) {
      if (other.shape != null)
        return false;
    } else if (!shape.equals(other.shape))
      return false;
    return true;
  }

  @SuppressWarnings("unchecked")
  public <T> FeatureResult<T> getResultFromCache(SplitFeature<T> splitFeature) {
    FeatureResult<T> result = null;

    if (this.featureResults.containsKey(splitFeature.getName())) {
      result = (FeatureResult<T>) this.featureResults.get(splitFeature.getName());
    }

    return result;
  }

  public <T> void putResultInCache(SplitFeature<T> splitFeature, FeatureResult<T> featureResult) {
    this.featureResults.put(splitFeature.getName(), featureResult);
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
