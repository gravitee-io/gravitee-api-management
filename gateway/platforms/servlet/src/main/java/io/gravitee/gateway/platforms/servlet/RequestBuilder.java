/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.platforms.servlet;

import io.gravitee.gateway.core.http.ContentRequest;
import io.gravitee.gateway.core.http.Request;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;

/**
 *
 * @author David BRASSEY (brasseld at gmail.com)
 */
public class RequestBuilder extends Request {

	public static Request from(HttpServletRequest servletRequest) throws IOException {
		Request request;

		if (hasContent(servletRequest)) {
			request = new ContentRequest(servletRequest.getInputStream());
		} else {
			request = new Request();
		}

		copyHeaders(request, servletRequest);
		copyQueryParameters(request, servletRequest);

		return request;
	}

	private static boolean hasContent(HttpServletRequest servletRequest) {
		return servletRequest.getContentLength() > 0 ||
				servletRequest.getContentType() != null ||
				// TODO: create an enum class for common HTTP headers
				servletRequest.getHeader("Transfer-Encoding") != null;
	}

	private static void copyHeaders(Request request, HttpServletRequest servletRequest) {
		Enumeration<String> headerNames = servletRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String hname = headerNames.nextElement();
			String hval = servletRequest.getHeader(hname);
			request.getHeaders().put(hname, hval);
		}
	}

	private static void copyQueryParameters(Request request, HttpServletRequest servletRequest) {
		String query = servletRequest.getQueryString();

		if (query != null) {
			try {
				query = URLDecoder.decode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}

			String[] pairSplit = query.split("&");
			for (String paramPair : pairSplit) {
				int idx = paramPair.indexOf("=");
				if (idx != -1) {
					String key = paramPair.substring(0, idx);
					String val = paramPair.substring(idx + 1);
					request.getQueryParameters().put(key, val);
				} else {
					request.getQueryParameters().put(paramPair, null);
				}
			}
		}
	}
}
