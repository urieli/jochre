///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreSearch;
import com.joliciel.jochre.search.JochreSearch.Command;
import com.joliciel.jochre.utils.Either;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Restful web service for Jochre search.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreSearchServlet extends HttpServlet {
	private static final Logger LOG = LoggerFactory.getLogger(JochreSearchServlet.class);
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		this.doGet(req, response);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		long startTime = System.currentTimeMillis();
		String user = null;
		try {
			if (LOG.isDebugEnabled())
				LOG.debug(getURI(req));

			req.setCharacterEncoding("UTF-8");
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json;charset=UTF-8");

			Command command = Command.valueOf(req.getParameter("command"));
			user = req.getParameter("user");

			if (command.equals("logConfig")) {
				response.setContentType("application/json;charset=UTF-8");
				Slf4jListener.reloadLogger(this.getServletContext());
				PrintWriter out = response.getWriter();
				out.write("{\"response\":\"logger reloaded\"}\n");
				out.flush();
				return;
			}

			Map<String, String> argMap = new HashMap<>();
			@SuppressWarnings("rawtypes")
			Enumeration params = req.getParameterNames();
			while (params.hasMoreElements()) {
				String paramName = (String) params.nextElement();
				String value = req.getParameter(paramName);
				if (LOG.isDebugEnabled())
					LOG.debug(paramName + ": " + value);
				argMap.put(paramName, value);
			}

			Either<PrintWriter, OutputStream> out;

			response.setContentType(command.getContentType());
			if (command.getContentType().startsWith("image")) {
				out = Either.ofRight(response.getOutputStream());
			} else {
				out = Either.ofLeft(response.getWriter());
			}

			Config config = ConfigFactory.load();
			String configId = config.getString("jochre.search.webapp.config-id");

			JochreSearch main = new JochreSearch(configId);
			main.execute(argMap, out);

			if (out.isLeft())
				out.getLeft().flush();
			else
				out.getRight().flush();

			response.setStatus(HttpServletResponse.SC_OK);
		} catch (RuntimeException e) {
			LOG.error("Failed to run " + getURI(req), e);
			throw e;
		} finally {
			long duration = System.currentTimeMillis() - startTime;
			LOG.info("User:" + user + " " + getURI(req) + " Duration:" + duration);
		}
	}

	public static String getURI(HttpServletRequest request) {
		String uri = request.getScheme() + "://" + request.getServerName()
				+ ("http".equals(request.getScheme()) && request.getServerPort() == 80 || "https".equals(request.getScheme()) && request.getServerPort() == 443
						? ""
						: ":" + request.getServerPort())
				+ request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

		return uri;
	}
}
