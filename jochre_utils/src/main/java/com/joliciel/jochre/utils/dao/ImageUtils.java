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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.joliciel.jochre.utils.JochreException;

/**
 * Various static utility methods for handling images with a database using the
 * Spring Framework (only tested on PostGreSQL).
 * 
 * @author Assaf Urieli
 *
 */
public class ImageUtils {

  /**
   * Get the image from a previously retrieved ResultSet.
   */
  public static BufferedImage getImage(ResultSet rs, String column) throws SQLException {
    BufferedImage image = null;
    if (rs.getObject(column) != null) {
      byte[] imageBytes = rs.getBytes(column);
      ByteArrayInputStream is = new ByteArrayInputStream(imageBytes);
      try {
        image = ImageIO.read(is);
        is.close();
      } catch (IOException e) {
        throw new JochreException(e);
      }
    }
    return image;
  }

  /**
   * Get the image from a previously retrieved SqlRowSet.
   */
  public static BufferedImage getImage(SqlRowSet rs, String column) throws SQLException {
    BufferedImage image = null;
    if (rs.getObject(column) != null) {
      byte[] imageBytes = (byte[]) rs.getObject(column);
      ByteArrayInputStream is = new ByteArrayInputStream(imageBytes);
      try {
        image = ImageIO.read(is);
        is.close();
      } catch (IOException e) {
        throw new JochreException(e);
      }
    }
    return image;
  }

  /**
   * Get an image from a sql query which selects only the image itself.
   */
  public static BufferedImage getImage(NamedParameterJdbcTemplate jt, String sql, MapSqlParameterSource paramSource, String column) {
    BufferedImage image = null;
    byte[] pixels = jt.query(sql, paramSource, new ResultSetExtractor<byte[]>() {
      @Override
      public byte[] extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (rs.next()) {
          byte[] pixels = rs.getBytes("image_image");

          return pixels;
        } else {
          return null;
        }
      }

    });

    ByteArrayInputStream is = new ByteArrayInputStream(pixels);
    try {
      image = ImageIO.read(is);
      is.close();
    } catch (IOException e) {
      throw new JochreException(e);
    }

    return image;
  }

  /**
   * Store the image in a MapSqlParameterSource for usage in an insert/update
   * query.
   */
  public static void storeImage(MapSqlParameterSource paramSource, String varName, BufferedImage image) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "png", os);
      os.flush();
      paramSource.addValue(varName, os.toByteArray());
      os.close();
    } catch (IOException e) {
      throw new JochreException(e);
    }
  }
}
