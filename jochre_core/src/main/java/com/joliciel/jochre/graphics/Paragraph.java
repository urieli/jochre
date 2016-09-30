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

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.JochreSession;

/**
 * A single paragraph within a page, containing 1 to <i>n</i> rows.
 * 
 * @author Assaf Urieli
 *
 */
public class Paragraph implements Entity, Rectangle {
	private int id;
	List<RowOfShapes> rows;

	private int index;
	private int imageId;
	private JochreImage image = null;

	private boolean coordinatesFound = false;
	private int left;
	private int top;
	private int right;
	private int bottom;

	private Boolean junk = null;
	private final JochreSession jochreSession;
	private final GraphicsDao graphicsDao;

	public Paragraph(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.graphicsDao = GraphicsDao.getInstance(jochreSession);
	}

	/**
	 * The JochreImage containing this paragraph.
	 */
	public JochreImage getImage() {
		if (this.imageId != 0 && this.image == null) {
			this.image = this.graphicsDao.loadJochreImage(this.imageId);
		}
		return this.image;
	}

	void setImage(JochreImage image) {
		this.image = image;
		if (image != null)
			this.setImageId(image.getId());
		else
			this.setImageId(0);
	}

	@Override
	public void save() {
		if (this.image != null && this.imageId == 0)
			this.imageId = this.image.getId();

		this.graphicsDao.saveParagraph(this);
		if (this.rows != null) {
			int index = 0;
			for (RowOfShapes row : this.rows) {
				row.setParagraph(this);
				row.setIndex(index++);
				row.save();
			}
		}
	}

	/**
	 * The index of this paragraph, from 0 (top-most in right-to-left or
	 * left-to-right languages) to n.
	 */
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getImageId() {
		return imageId;
	}

	void setImageId(int imageId) {
		this.imageId = imageId;
	}

	public List<RowOfShapes> getRows() {
		if (rows == null) {
			if (this.id == 0)
				rows = new ArrayList<RowOfShapes>();
			else {
				rows = graphicsDao.findRows(this);
				for (RowOfShapes row : rows) {
					row.setParagraph(this);
				}
			}
		}
		return rows;
	}

	public RowOfShapes newRow() {
		RowOfShapes row = new RowOfShapes(jochreSession);
		row.setParagraph(this);
		this.getRows().add(row);
		return row;
	}

	/**
	 * The leftmost x coordinate of this paragraph (based on the rows it
	 * contains).
	 */
	@Override
	public int getLeft() {
		this.findCoordinates();
		return this.left;
	}

	/**
	 * The topmost y coordinate of this paragraph (based on the rows it contains).
	 */
	@Override
	public int getTop() {
		this.findCoordinates();
		return this.top;
	}

	/**
	 * The rightmost x coordinate of this paragraph (based on the rows it
	 * contains).
	 */
	@Override
	public int getRight() {
		this.findCoordinates();
		return this.right;
	}

	/**
	 * The bottom-most y coordinate of this paragraph (based on the rows it
	 * contains).
	 */
	@Override
	public int getBottom() {
		this.findCoordinates();
		return this.bottom;
	}

	private void findCoordinates() {
		if (!coordinatesFound) {
			RowOfShapes firstRow = this.getRows().iterator().next();
			left = firstRow.getLeft();
			top = firstRow.getTop();
			right = firstRow.getRight();
			bottom = firstRow.getBottom();

			for (RowOfShapes row : this.getRows()) {
				if (row.getLeft() < left)
					left = row.getLeft();
				if (row.getTop() < top)
					top = row.getTop();
				if (row.getRight() > right)
					right = row.getRight();
				if (row.getBottom() > bottom)
					bottom = row.getBottom();
			}
			coordinatesFound = true;
		}
	}

	/**
	 * Does this paragraph contain a vast majority of "junk" analyses (low
	 * confidence letters, inexistent words, etc. not to be exported)
	 */
	public boolean isJunk() {
		if (junk == null) {
			if (this.getRows().size() > 0) {
				double averageConfidence = 0;
				double shapeCount = 0;
				for (RowOfShapes row : this.getRows()) {
					for (GroupOfShapes group : row.getGroups()) {
						if (group.getShapes().size() > 0) {
							for (Shape shape : group.getShapes()) {
								averageConfidence += shape.getConfidence();
								shapeCount += 1;
							}
						}
					}
				}
				averageConfidence = averageConfidence / shapeCount;

				if (averageConfidence < jochreSession.getJunkConfidenceThreshold())
					junk = true;
				else
					junk = false;
			} else {
				junk = true;
			}
		}
		return junk;
	}

	@Override
	public String toString() {
		return "Par " + this.getIndex() + ", left(" + this.getLeft() + ")" + ", top(" + this.getTop() + ")" + ", right(" + this.getRight() + ")" + ", bot("
				+ this.getBottom() + ")" + ", width(" + this.getWidth() + ")" + ", height(" + this.getHeight() + ")";
	}

	@Override
	public int getWidth() {
		return right - left + 1;
	}

	@Override
	public int getHeight() {
		return bottom - top + 1;
	}

	public void addRow(RowOfShapes row) {
		this.getRows().add(row);
		row.setParagraph(this);
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
