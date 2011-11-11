/*
 * Copyright (c) 2011 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.github.kevinsawicki.http.github;

import static org.eclipse.egit.github.core.client.IGitHubConstants.CHARSET_UTF8;
import static org.eclipse.egit.github.core.client.IGitHubConstants.CONTENT_TYPE_JSON;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.HttpClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.client.SizedInputStreamPart;

/**
 * Client that uses {@link HttpRequest} instances to make requests
 */
public class HttpRequestClient extends HttpClient<HttpRequestClient> {

	private String user;

	private String password;

	private String userAgent;

	/**
	 * Create client
	 */
	public HttpRequestClient() {
		super();
	}

	/**
	 * @param hostname
	 * @param port
	 * @param scheme
	 */
	public HttpRequestClient(String hostname, int port, String scheme) {
		super(hostname, port, scheme);
	}

	/**
	 * @param hostname
	 */
	public HttpRequestClient(final String hostname) {
		super(hostname);
	}

	@Override
	public HttpRequestClient setUserAgent(final String agent) {
		this.userAgent = agent;
		return this;
	}

	@Override
	public HttpRequestClient setCredentials(final String user,
			final String password) {
		this.user = user;
		this.password = password;
		return this;
	}

	@Override
	public String getUser() {
		return user;
	}

	private <V> V sendJson(final HttpRequest request, final Object params,
			final Type type) throws IOException {
		if (params != null)
			request.contentType(CONTENT_TYPE_JSON, CHARSET_UTF8).send(
					toJson(params));
		final int code = request.code();
		if (isOk(code))
			if (type != null)
				return parseJson(request.stream(), type);
			else
				return null;
		if (isEmpty(code))
			return null;
		throw createException(request.stream(), code, request.message());
	}

	private String createUri(final String uri) {
		return baseUri + configureUri(uri);
	}

	private HttpRequest configure(final HttpRequest request) {
		if (user != null && password != null)
			request.basic(user, password);
		if (userAgent != null && userAgent.length() > 0)
			request.userAgent(userAgent);
		return request;
	}

	@Override
	public InputStream getStream(GitHubRequest request) throws IOException {
		try {
			return configure(HttpRequest.get(createUri(request.generateUri())))
					.stream();
		} catch (HttpRequestException e) {
			throw (IOException) e.getCause();
		}
	}

	@Override
	public HttpResponse get(GitHubRequest request) throws IOException {
		try {
			final HttpRequest httpRequest = configure(HttpRequest
					.get(createUri(request.generateUri())));
			final int code = httpRequest.code();
			if (isOk(code))
				return new HttpResponse(httpRequest, getBody(request,
						httpRequest.stream()));
			if (isEmpty(code))
				return new HttpResponse(httpRequest, null);
			throw createException(httpRequest.stream(), code,
					httpRequest.message());
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	@Override
	public <V> V post(final String uri, final Object params, final Type type)
			throws IOException {
		try {
			final HttpRequest request = configure(HttpRequest
					.post(createUri(uri)));
			return sendJson(request, params, type);
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	@Override
	public <V> V put(final String uri, final Object params, final Type type)
			throws IOException {
		try {
			final HttpRequest request = configure(HttpRequest
					.put(createUri(uri)));
			return sendJson(request, params, type);
		} catch (HttpRequestException e) {
			throw (IOException) e.getCause();
		}
	}

	@Override
	public void delete(final String uri, final Object params)
			throws IOException {
		try {
			final HttpRequest request = configure(HttpRequest
					.delete(createUri(uri)));
			if (params != null)
				request.contentType(CONTENT_TYPE_JSON, CHARSET_UTF8).send(
						toJson(params));
			final int code = request.code();
			if (!isEmpty(code))
				throw new RequestException(parseError(request.stream()), code);
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	@Override
	public int postMultipart(final String uri, final Map<String, Object> parts)
			throws IOException {
		try {
			final HttpRequest post = HttpRequest.post(uri);
			for (Entry<String, Object> part : parts.entrySet()) {
				final Object value = part.getValue();
				if (value instanceof SizedInputStreamPart)
					post.part(part.getKey(),
							((SizedInputStreamPart) value).getStream());
				else
					post.part(part.getKey(), part.getValue().toString());
			}
			return post.code();
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}
}
