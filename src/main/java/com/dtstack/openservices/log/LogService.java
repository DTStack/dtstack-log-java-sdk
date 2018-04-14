/*
 * Copyright (C) Alibaba Cloud Computing All rights reserved.
 */
package com.dtstack.openservices.log;

import com.dtstack.openservices.log.common.LogItem;
import com.dtstack.openservices.log.exception.LogException;
import com.dtstack.openservices.log.request.PutLogsRequest;
import com.dtstack.openservices.log.response.PutLogsResponse;

import java.util.List;


public interface LogService {



	/**
	 * Send Data to log service server
	 * 
	 * @param project
	 *            the project name
	 * @param logStore
	 *            the log store where the source data should be put
	 * @param topic
	 *            source data topic
	 * @param logItems
	 *            the log data to send
	 * 
	 * @param source
	 *            the source of the data, if the source is empty, it will be
	 *            reset to the host ip
	 * 
	 * @return The put logs response
	 * 
	 * @throws LogException
	 *             if any error happen when send data to the server
	 * @throws NullPointerException
	 *             if any parameter is null
	 * @throws IllegalArgumentException
	 *             if project or logstore is empty, or the logGroup log count
	 *             exceed 4096, or the total data size exceed 5MB
	 */
	public PutLogsResponse putLogs(String project, String logStore,
								   String topic, List<LogItem> logItems, String source)
			throws LogException;

	/**
	 * Send Data to log service server
	 * 
	 * @param project
	 *            the project name
	 * @param logStore
	 *            the log store where the source data should be put
	 * @param topic
	 *            source data topic
	 * @param logItems
	 *            the log data to send
	 * @param source
	 *            the source of the data, if the source is empty, it will be
	 *            reset to the host ip
	 * @param shardHash
	 *            the hash key md5value (00000000000000000000000000000000 ~
	 *            ffffffffffffffffffffffffffffffff)
	 * @return The put logs response
	 * @throws LogException
	 *             if any error happen when send data to the server
	 * @throws NullPointerException
	 *             if any parameter is null
	 * @throws IllegalArgumentException
	 *             if project or logstore is empty, or the logGroup log count
	 *             exceed 4096, or the total data size exceed 5MB
	 */
	public PutLogsResponse putLogs(String project, String logStore,
                                   String topic, List<LogItem> logItems, String source,
                                   String shardHash) throws LogException;
	/**
	 * Send Data to log service server
	 * 
	 * @param request
	 *            the put log request
	 * 
	 * @return The put logs response
	 * 
	 * @throws LogException
	 *             if any error happen when send data to the server
	 * @throws NullPointerException
	 *             if any parameter is null
	 * @throws IllegalArgumentException
	 *             if project or logstore is empty, or the logGroup log count
	 *             exceed 4096, or the total data size exceed 5MB
	 */
	public PutLogsResponse putLogs(PutLogsRequest request) throws LogException;



}
