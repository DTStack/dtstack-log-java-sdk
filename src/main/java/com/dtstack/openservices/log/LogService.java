/*
 * Copyright (C) Alibaba Cloud Computing All rights reserved.
 */
package com.dtstack.openservices.log;

import com.dtstack.openservices.log.exception.LogException;
import com.dtstack.openservices.log.request.PutLogsRequest;
import com.dtstack.openservices.log.request.QueryLogsRequest;
import com.dtstack.openservices.log.response.QueryLogsResponse;
import com.dtstack.openservices.log.response.PutLogsResponse;


public interface LogService {


	public PutLogsResponse putLogs(PutLogsRequest request) throws LogException;


	public QueryLogsResponse queryLogs(QueryLogsRequest request) throws LogException;



}
