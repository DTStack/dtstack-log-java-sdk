/*
 * Copyright (C) Alibaba Cloud Computing All rights reserved.
 */
package com.dtstack.openservices.log.response;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.dtstack.openservices.log.common.Consts;

/**
 * <p>
 *     响应模型
 * </p>
 * 
 * @author qingya@dtstack.com
 * 
 */
public class Response implements Serializable {

	private static final long serialVersionUID = 7331835262124313824L;
	private Map<String, String> mHeaders = new HashMap<String, String>();

	/**
	 * Construct the base response body with http headers
	 * 
	 * @param headers
	 *            http headers
	 */
	public Response(Map<String, String> headers) {
		setAllHeaders(headers);
	}

	/**
	 * Get the request id of the response
	 * 
	 * @return request id
	 */
	public String getRequestId() {
		return getHeader(Consts.CONST_X_SLS_REQUESTID);
	}

	/**
	 * Get the value of a key in the http response header, if the key is not
	 * found, it will return empty
	 * 
	 * @param key
	 *            key name
	 * @return the value of the key
	 */
	public String getHeader(String key) {
		if (mHeaders.containsKey(key)) {
			return mHeaders.get(key);
		} else {
			return new String();
		}
	}

	/**
	 * Set http headers
	 * 
	 * @param headers
	 *            http headers
	 */
	private void setAllHeaders(Map<String, String> headers) {
		mHeaders = new HashMap<String, String>(headers);
	}

	/**
	 * Get all http headers
	 * 
	 * @return http headers
	 */
	public Map<String, String> getAllHeaders() {
		return mHeaders;
	}

}
