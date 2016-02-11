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
import com.joliciel.jochre.utils.JochreException;

/**
 * Various static utility methods for handling images with a database using the Spring Framework
 * (only tested on PostGreSQL).
 * @author Assaf Urieli
 *
 */
public class ImageUtils {

	/**
	 * Get the image from a previously retrieved ResultSet.
	 * @param rs
	 * @param column
	 * @return
	 * @throws SQLException
	 */
	public static BufferedImage getImage(ResultSet rs, String column) throws SQLException {
		BufferedImage image = null;
		if (rs.getObject(column)!=null) {        	   
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
	 * Get an image from a sql query which selects only the image itself.
	 * @param jt
	 * @param sql
	 * @param paramSource
	 * @param column
	 * @return
	 */
	public static BufferedImage getImage(NamedParameterJdbcTemplate jt, String sql, MapSqlParameterSource paramSource, String column)  {
		BufferedImage image = null;
		byte[] pixels = (byte[]) jt.query(sql, paramSource,
				new ResultSetExtractor() {
			@Override
			public Object extractData(ResultSet rs)
			throws SQLException, DataAccessException {
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
	 * Store the image in a MapSqlParameterSource for usage in an insert/update query.
	 * @param paramSource
	 * @param varName
	 * @param image
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
