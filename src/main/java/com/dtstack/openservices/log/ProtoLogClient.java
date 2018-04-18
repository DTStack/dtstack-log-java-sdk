/*
 * Copyright (C) Alibaba Cloud Computing All rights reserved.
 */
package com.dtstack.openservices.log;

import com.dtstack.openservices.log.common.*;
import com.dtstack.openservices.log.exception.LogException;
import com.dtstack.openservices.log.http.client.*;
import com.dtstack.openservices.log.http.comm.DefaultServiceClient;
import com.dtstack.openservices.log.http.comm.RequestMessage;
import com.dtstack.openservices.log.http.comm.ResponseMessage;
import com.dtstack.openservices.log.http.comm.ServiceClient;
import com.dtstack.openservices.log.http.utils.CodingUtils;
import com.dtstack.openservices.log.http.utils.DateUtil;
import com.dtstack.openservices.log.request.QueryLogsRequest;
import com.dtstack.openservices.log.request.PutLogsRequest;
import com.dtstack.openservices.log.response.QueryLogsResponse;
import com.dtstack.openservices.log.response.PutLogsResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.validator.routines.InetAddressValidator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

/**
 * <p>
 *     基于proto[B]uf数据协议实现的日志客户端，对于性能有更高要求的场景可以使用.
 * </p>
 * @author qingya@dtstack.com
 * 
 */
public class ProtoLogClient implements LogService {

	private String httpType;
	private String hostName;
	private String accessKey;
	private String sourceIp;
	private ServiceClient serviceClient;
	private String securityToken;
	private String realIpForConsole;
	private Boolean useSSLForConsole;
	private String userAgent = Consts.CONST_USER_AGENT_VALUE;
	private boolean mUUIDTag = false;

	/**
	 * Construct the sls client with accessId, accessKey and server address, all
	 * other parameters will be set to default value
	 *
	 * @param endpoint  the log service server address
	 * @param accessId  aliyun accessId
	 * @param accessKey aliyun accessKey
	 * @throws NullPointerException     if the input parameter is null
	 * @throws IllegalArgumentException if the input parameter is empty
	 */
	public ProtoLogClient(String endpoint, String accessId, String accessKey) {
		this(endpoint, accessId, accessKey, GetLocalMachineIp());
	}

	/**
	 * Construct the sls client with accessId, accessKey , server address and
	 * client ip address, all other parameters will be set to default value
	 *
	 * @param endpoint  the log service server address
	 * @param accessId  aliyun accessId
	 * @param accessKey aliyun accessKey
	 * @param SourceIp  client ip address
	 * @throws NullPointerException     if the input parameter is null
	 * @throws IllegalArgumentException if the input parameter is empty
	 */
	public ProtoLogClient(String endpoint, String accessId, String accessKey,
						  String SourceIp) {
		this(endpoint, accessId, accessKey, SourceIp,
				Consts.DEFAULT_SLS_COMPRESS_FLAG);
	}

	/**
	 * Construct sls client with full parameters
	 *
	 * @param endpoint        the log service server address
	 * @param accessId        aliyun accessId
	 * @param accessKey       aliyun accessKey
	 * @param sourceIp        client ip address
	 * @param connectMaxCount a flag to determine max count connection
	 * @param connectTimeout  a flag to determine max connect timeout
	 * @param sendTimeout     a flag to determine max request timeout
	 * @throws NullPointerException     if the input parameter is null
	 * @throws IllegalArgumentException if the input parameter is empty
	 */
	public ProtoLogClient(String endpoint, String accessId, String accessKey,
						  String sourceIp,
						  int connectMaxCount,
						  int connectTimeout,
						  int sendTimeout) {
		CodingUtils.assertStringNotNullOrEmpty(endpoint, "endpoint");
		CodingUtils.assertStringNotNullOrEmpty(accessId, "accessId");
		CodingUtils.assertStringNotNullOrEmpty(accessKey, "accessKey");

		if (endpoint.startsWith("http://")) {
			this.hostName = endpoint.substring(7);
			this.httpType = new String("http://");
		} else if (endpoint.startsWith("https://")) {
			this.hostName = endpoint.substring(8);
			this.httpType = new String("https://");
		} else {
			this.hostName = endpoint;
			this.httpType = new String("http://");
		}
		while (this.hostName.endsWith("/")) {
			this.hostName = this.hostName.substring(0,
					this.hostName.length() - 1);
		}
		if (IsIpAddress(this.hostName)) {
			throw new IllegalArgumentException("EndpontInvalid", new Exception(
					"The ip address is not supported"));
		}
		this.accessKey = accessKey;
		this.sourceIp = sourceIp;
		if (sourceIp == null || sourceIp.isEmpty()) {
			this.sourceIp = GetLocalMachineIp();
		}
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setMaxConnections(connectMaxCount);
		clientConfig.setConnectionTimeout(connectTimeout);
		clientConfig.setSocketTimeout(sendTimeout);
		this.serviceClient = new DefaultServiceClient(clientConfig);

	}

	/**
	 * Construct sls client with full parameters
	 *
	 * @param endpoint     the log service server address
	 * @param accessId     aliyun accessId
	 * @param accessKey    aliyun accessKey
	 * @param sourceIp     client ip address
	 * @param compressFlag a flag to determine if the send data will compressed , default
	 *                     is true ( data compressed)
	 * @throws NullPointerException     if the input parameter is null
	 * @throws IllegalArgumentException if the input parameter is empty
	 */
	public ProtoLogClient(String endpoint, String accessId, String accessKey,
						  String sourceIp, boolean compressFlag) {
		this(endpoint, accessId, accessKey, sourceIp,
				Consts.HTTP_CONNECT_MAX_COUNT,
				Consts.HTTP_CONNECT_TIME_OUT,
				Consts.HTTP_SEND_TIME_OUT);
	}


	@Override
	public QueryLogsResponse queryLogs(QueryLogsRequest request) throws LogException {
		return null;
	}

	private URI getHostURIByIp(String ipAddress) throws LogException {
		String endPointUrl = this.httpType + ipAddress;
		try {
			return new URI(endPointUrl);
		} catch (URISyntaxException e) {
			throw new LogException("EndpointInvalid",
					"Failed to get real server ip when direct mode in enabled", "");
		}
	}

	private static boolean IsIpAddress(String str) {
		Pattern pattern = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}");
		return pattern.matcher(str).matches();
	}


	public PutLogsResponse putLogs(String project, String logStore, byte[] logGroupBytes, String compressType) throws LogException {
		CodingUtils.assertStringNotNullOrEmpty(project, "project");
		CodingUtils.assertStringNotNullOrEmpty(logStore, "logStore");
		CodingUtils.assertParameterNotNull(logGroupBytes, "logGroupBytes");

		PutLogsRequest request = new PutLogsRequest(logStore, null, null, logGroupBytes);
		if (compressType.equals(Consts.CONST_LZ4)) {
			request.SetCompressType(Consts.CompressType.LZ4);
		} else if (compressType.equals(Consts.CONST_GZIP_ENCODING)) {
			request.SetCompressType(Consts.CompressType.GZIP);
		} else if (compressType.isEmpty()) {
			request.SetCompressType(Consts.CompressType.NONE);
		} else {
			throw new IllegalArgumentException("invalid CompressType: " + compressType + ", should be (" + Consts.CompressType.NONE + ", " + Consts.CompressType.GZIP + ", " + Consts.CompressType.LZ4 + ")");
		}
		return putLogs(request);

	}



	public PutLogsResponse putLogs(PutLogsRequest request) throws LogException {
		CodingUtils.assertParameterNotNull(request, "request");
		String logStore = request.GetLogStore();
		CodingUtils.assertStringNotNullOrEmpty(logStore, "logStore");
		String shardKey = request.GetRouteKey();
		Consts.CompressType compressType = request.GetCompressType();
		CodingUtils.assertParameterNotNull(compressType, "compressType");

		byte[] logBytes = request.GetLogGroupBytes();
		if (logBytes != null) {
		} else {
			List<LogItem> logItems = request.GetLogItems();
			if (logItems.size() > Consts.CONST_MAX_PUT_LINES) {
				throw new LogException("InvalidLogSize",
						"logItems' length exceeds maximum limitation : " + String.valueOf(Consts.CONST_MAX_PUT_LINES) + " lines", "");
			}
			String topic = request.GetTopic();
			CodingUtils.assertParameterNotNull(topic, "topic");
			String source = request.GetSource();
			if (request.getContentType() != Consts.CONST_SLS_JSON) { // 消息发送格式不是标准的JSON
				Logs.LogGroup.Builder logs = Logs.LogGroup.newBuilder();
				if (topic != null) {
					logs.setTopic(topic);
				}
				if (source == null || source.isEmpty()) {
					logs.setSource(this.sourceIp);
				} else {
					logs.setSource(source);
				}
				ArrayList<TagContent> tags = request.GetTags();
				if (tags != null && tags.size() > 0) {
					for (TagContent tag : tags) {
						Logs.LogTag.Builder tagBuilder = logs.addLogTagsBuilder();
						tagBuilder.setKey(tag.getKey());
						tagBuilder.setValue(tag.getValue());
					}
				}
				if (this.mUUIDTag) {
					Logs.LogTag.Builder tagBuilder = logs.addLogTagsBuilder();
					tagBuilder.setKey("__pack_unique_id__");
					tagBuilder.setValue(UUID.randomUUID().toString() + "-" + String.valueOf(Math.random()));
				}
				for (int i = 0; i < logItems.size(); i++) {
					LogItem item = logItems.get(i);
					Logs.Log.Builder log = logs.addLogsBuilder();
					log.setTime(item.mLogTime);
					for (LogContent content : item.mContents) {
						CodingUtils.assertStringNotNullOrEmpty(content.mKey, "key");
						Logs.Log.Content.Builder contentBuilder = log
								.addContentsBuilder();
						contentBuilder.setKey(content.mKey);
						if (content.mValue == null) {
							contentBuilder.setValue("");
						} else {
							contentBuilder.setValue(content.mValue);
						}
					}
				}
				logBytes = logs.build().toByteArray();
			} else {
				JSONObject jsonObj = new JSONObject();
				if (topic != null) {
					jsonObj.put("__topic__", topic);
				}
				if (source == null || source.isEmpty()) {
					jsonObj.put("__source__", this.sourceIp);
				} else {
					jsonObj.put("__source__", source);
				}
				JSONArray logsArray = new JSONArray();
				for (int i = 0; i < logItems.size(); i++) {
					LogItem item = logItems.get(i);
					JSONObject jsonObjInner = new JSONObject();
					jsonObjInner.put("__time__", item.mLogTime);
					for (LogContent content : item.mContents) {
						jsonObjInner.put(content.mKey, content.mValue);
					}
					logsArray.add(jsonObjInner);
				}
				jsonObj.put("__logs__", logsArray);
				JSONObject tagObj = new JSONObject();
				ArrayList<TagContent> tags = request.GetTags();
				if (tags != null && tags.size() > 0) {
					for (TagContent tag : tags) {
						tagObj.put(tag.getKey(), tag.getValue());
					}
				}
				if (this.mUUIDTag) {
					tagObj.put("__pack_unique_id__", UUID.randomUUID().toString() + "-" + String.valueOf(Math.random()));
				}
				if (tagObj.size() > 0) {
					jsonObj.put("__tags__", tagObj);
				}
				try {
					logBytes = jsonObj.toString().getBytes("utf-8");
				} catch (UnsupportedEncodingException e) {
					throw new LogException("UnsupportedEncoding", e.getMessage(), "");
				}
			}
		}
		if (logBytes.length > Consts.CONST_MAX_PUT_SIZE) {
			throw new LogException("InvalidLogSize",
					"logItems' size exceeds maximum limitation : "
							+ String.valueOf(Consts.CONST_MAX_PUT_SIZE)
							+ " bytes", "");
		}

		Map<String, String> headParameter = defineCommonHeader();
		headParameter.put(Consts.CONST_CONTENT_TYPE, request.getContentType());
		long originalSize = logBytes.length;

		//消息压缩方式
		if (compressType == Consts.CompressType.LZ4) {
			logBytes = LZ4Encoder.compressToLhLz4Chunk(logBytes.clone());
			headParameter.put(Consts.CONST_X_SLS_COMPRESSTYPE,
					compressType.toString());
		} else if (compressType == Consts.CompressType.GZIP) {
			ByteArrayOutputStream out = new ByteArrayOutputStream(
					logBytes.length);

			Deflater compresser = new Deflater();
			compresser.setInput(logBytes);
			compresser.finish();

			byte[] buf = new byte[10240];
			while (compresser.finished() == false) {
				int count = compresser.deflate(buf);
				out.write(buf, 0, count);
			}

			logBytes = out.toByteArray();
			headParameter.put(Consts.CONST_X_SLS_COMPRESSTYPE,
					compressType.toString());
		}

		headParameter.put(Consts.CONST_X_SLS_BODYRAWSIZE,
				String.valueOf(originalSize));

		String resourceUri = "/logstores/" + logStore;
		if (shardKey == null || shardKey.length() == 0) {
			resourceUri += "/shards/lb";
		} else
			resourceUri += "/shards/route?key=" + shardKey;
		Map<String, String> urlParameter = new HashMap<String, String>();
		urlParameter = request.GetAllParams();
		long cmp_size = logBytes.length;


		for (int i = 0; i < 2; i++) {
			String server_ip = null;
			ClientConnectionStatus connection_status = null;
			try {
				ResponseMessage response = sendData(HttpMethod.POST, urlParameter, headParameter,
						logBytes, null, server_ip);
				Map<String, String> resHeaders = response.getHeaders();
				PutLogsResponse putLogsResponse = new PutLogsResponse(resHeaders);
				if (connection_status != null) {
					connection_status.AddSendDataSize(cmp_size);
					connection_status.UpdateLastUsedTime(System.nanoTime());
				}
				return putLogsResponse;
			} catch (LogException e) {
				String request_id = e.GetRequestId();
				if (i == 1 || request_id != null && request_id.isEmpty() == false) {
					throw e;
				}
				if (connection_status != null) {
					connection_status.DisableConnection();
				}
			}
		}
		return null;
	}




	public QueryLogsResponse getLogs(QueryLogsRequest request) throws LogException {
		CodingUtils.assertParameterNotNull(request, "request");
		Map<String, String> urlParameter = request.GetAllParams();

		String logStore = request.GetLogStore();

		Map<String, String> headParameter = defineCommonHeader();

		ResponseMessage response = sendData(HttpMethod.GET, urlParameter, headParameter);

		Map<String, String> resHeaders = response.getHeaders();
		String requestId = GetRequestId(resHeaders);

		com.alibaba.fastjson.JSONArray object = parseResponseMessageToArrayWithFastJson(response, requestId);
		QueryLogsResponse getLogsResponse = new QueryLogsResponse(resHeaders);
		extractLogsWithFastJson(getLogsResponse, object);
		return getLogsResponse;

	}

	protected String GetRequestId(Map<String, String> headers) {
		if (headers.containsKey(Consts.CONST_X_SLS_REQUESTID)) {
			return headers.get(Consts.CONST_X_SLS_REQUESTID);
		} else {
			return "";
		}
	}


	@SuppressWarnings("unused")
	private String extractJsonString(String nodeKey, JSONObject object) {
		try {
			return object.getString(nodeKey);
		} catch (JSONException e) {
			// ignore
		}
		return "";
	}

	private int extractJsonInteger(String nodeKey, JSONObject object) {
		try {
			return object.getInt(nodeKey);
		} catch (JSONException e) {
			// ignore
		}
		return -1;
	}

	private List<String> extractJsonArray(String nodeKey, JSONObject object) {
		try {
			JSONArray items = object.getJSONArray(nodeKey);
			return ExtractJsonArray(items);
		} catch (JSONException e) {
			return new ArrayList<String>();
		}
	}

	private List<String> ExtractJsonArray(JSONArray ojbect) {
		ArrayList<String> result = new ArrayList<String>();
		try {

			for (int i = 0; i < ojbect.size(); i++) {
				result.add(ojbect.getString(i));
			}
		} catch (JSONException e) {
			// ignore
		}
		return result;
	}


	private void extractLogs(QueryLogsResponse response, JSONArray logs) {
		try {
			for (int i = 0; i < logs.size(); i++) {
				JSONObject log = logs.getJSONObject(i);
				String source = new String();
				LogItem logItem = new LogItem();
				@SuppressWarnings("unchecked")
				Iterator<String> it = (Iterator<String>) (log.keys());
				while (it.hasNext()) {
					String key = it.next();
					String value = log.getString(key);
					if (key.equals(Consts.CONST_RESULT_SOURCE)) {
						source = value;
					} else if (key.equals(Consts.CONST_RESULT_TIME)) {
						logItem.mLogTime = Integer.parseInt(value);
					} else {
						logItem.PushBack(key, value);
					}
				}
				response.AddLog(new QueriedLog(source, logItem));
			}
		} catch (JSONException e) {
			// ignore;
		}

	}

	protected void ErrorCheck(JSONObject object, String requestId)
			throws LogException {
		if (object.containsKey(Consts.CONST_ERROR_CODE)) {
			try {
				String errorCode = object.getString(Consts.CONST_ERROR_CODE);
				String errorMessage = object
						.getString(Consts.CONST_ERROR_MESSAGE);

				throw new LogException(errorCode, errorMessage, requestId);
			} catch (JSONException e) {
				throw new LogException("InvalidErrorResponse",
						"Error response is not a valid error json : \n"
								+ object.toString(), requestId);
			}
		} else {
			throw new LogException("InvalidErrorResponse",
					"Error response is not a valid error json : \n"
							+ object.toString(), requestId);
		}
	}

	/**
	 * <p>
	 *     解析响应报文
	 * </p>
	 * @param response
	 * @throws LogException
	 */
	private void extractResponseBody(ResponseMessage response) throws LogException {
		InputStream in = response.getContent();
		if (in == null) {
			return;
		}
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		String requestId = GetRequestId(response.getHeaders());
		int ch;
		try {
			byte[] cache = new byte[1024];
			while ((ch = in.read(cache, 0, 1024)) != -1) {
				byteStream.write(cache, 0, ch);
			}
		} catch (IOException e) {
			throw new LogException("BadResponse",
					"Io exception happened when parse the response data : ", e,
					requestId);
		}

		response.setBody(byteStream.toByteArray());

	}

	protected JSONObject parserResponseMessage(ResponseMessage response,
											   String requestId) throws LogException {
		byte[] body = response.getRawBody();

		if (body == null) {
			throw new LogException("BadResponse", "The response body is null",
					null, requestId);
		}
		String res;
		try {
			res = new String(body, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new LogException("BadResponse",
					"The response is not valid utf-8 string : ", e, requestId);
		}
		try {
			JSONObject object = JSONObject.fromObject(res);

			return object;
		} catch (JSONException e) {
			throw new LogException("BadResponse",
					"The response is not valid json string : " + res, e,
					requestId);
		}
	}

	protected com.alibaba.fastjson.JSONObject ParserResponseMessageWithFastJson(ResponseMessage response,
																				String requestId) throws LogException {
		byte[] body = response.getRawBody();

		if (body == null) {
			throw new LogException("BadResponse", "The response body is null",
					null, requestId);
		}
		String res;
		try {
			res = new String(body, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new LogException("BadResponse",
					"The response is not valid utf-8 string : ", e, requestId);
		}
		try {
			com.alibaba.fastjson.JSONObject object = com.alibaba.fastjson.JSONObject.parseObject(res);

			return object;
		} catch (com.alibaba.fastjson.JSONException e) {
			throw new LogException("BadResponse",
					"The response is not valid json string : " + res, e,
					requestId);
		}
	}

	private JSONArray parseResponseMessageToArray(ResponseMessage response,
												  String requestId) throws LogException {
		byte[] body = response.getRawBody();
		if (body == null) {
			throw new LogException("BadResponse", "The response body is null",
					null, requestId);
		}
		String returnStr;
		try {
			returnStr = new String(body, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new LogException("BadResponse",
					"The response is not valid utf-8 string : ", e, requestId);
		}

		try {
			JsonConfig jsonConfig = new JsonConfig();
			jsonConfig.setIgnoreDefaultExcludes(true);
			JSONArray array = JSONArray.fromObject(returnStr, jsonConfig);
			return array;
		} catch (JSONException e) {
			throw new LogException("BadResponse",
					"The response is not valid json string : " + returnStr, e,
					requestId);
		}
	}

	/**
	 * <p>
	 *     消息md5防篡改
	 * </p>
	 * @param bytes
	 * @return
	 */
	private String getMd5Value(byte[] bytes) {
		try {
			MessageDigest md;
			md = MessageDigest.getInstance(Consts.CONST_MD5);
			String res = new BigInteger(1, md.digest(bytes)).toString(16)
					.toUpperCase();

			StringBuilder zeros = new StringBuilder();
			for (int i = 0; i + res.length() < 32; i++) {
				zeros.append("0");
			}
			return zeros.toString() + res;
		} catch (NoSuchAlgorithmException e) {
			// never happen
			throw new RuntimeException("Not Supported signature method "
					+ Consts.CONST_MD5, e);
		}
	}


	private Map<String, String> defineCommonHeader() {

		HashMap<String, String> headParameter = new HashMap<String, String>();
		headParameter.put(Consts.CONST_USER_AGENT, userAgent);
		headParameter.put(Consts.CONST_CONTENT_LENGTH, "0");
		headParameter.put(Consts.CONST_X_SLS_BODYRAWSIZE, "0");
		headParameter.put(Consts.CONST_CONTENT_TYPE, Consts.CONST_PROTO_BUF);
		headParameter.put(Consts.CONST_DATE,
				DateUtil.formatRfc822Date(new Date()));


		headParameter.put(Consts.CONST_HOST, this.hostName);

		headParameter.put(Consts.CONST_X_SLS_APIVERSION,
				Consts.DEFAULT_API_VESION);
		headParameter.put(Consts.CONST_X_SLS_SIGNATUREMETHOD, Consts.HMAC_SHA1);
		if (securityToken != null && !securityToken.isEmpty()) {
			headParameter.put(Consts.CONST_X_ACS_SECURITY_TOKEN, securityToken);
		}
		if (realIpForConsole != null && !realIpForConsole.isEmpty()) {
			headParameter.put(Consts.CONST_X_SLS_IP, realIpForConsole);
		}
		if (useSSLForConsole != null) {
			headParameter.put(Consts.CONST_X_SLS_SSL, useSSLForConsole ? "true"
					: "false");
		}
		return headParameter;

	}

	private ResponseMessage sendData(HttpMethod method, Map<String, String> urlParams,
									 Map<String, String> headParams) throws LogException {
		return sendData(method, urlParams, headParams,
				new byte[0]);
	}

	protected ResponseMessage sendData(HttpMethod method, Map<String, String> parameters,
									   Map<String, String> headers, String requestBody) throws LogException {
		byte[] body;
		try {
			body = requestBody.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new LogException("EncodingException", e.getMessage(), "");
		}
		return sendData(method, parameters, headers, body);
	}

	protected ResponseMessage sendData(HttpMethod method,
									   Map<String, String> parameters, Map<String, String> headers, byte[] body) throws LogException {
		return sendData(method, parameters, headers, body, null, null);
	}

	/**
	 * <p>
	 *     发送数据
	 * </p>
	 * @param method
	 * @param parameters
	 * @param headers
	 * @param body
	 * @param output_header
	 * @param serverIp
	 * @return
	 * @throws LogException
	 */
	protected ResponseMessage sendData(HttpMethod method,
									   Map<String, String> parameters, Map<String, String> headers, byte[] body,
									   Map<String, String> output_header, String serverIp) throws LogException {
		if (body.length > 0) {
			headers.put(Consts.CONST_CONTENT_MD5, getMd5Value(body));
		}
		headers.put(Consts.CONST_CONTENT_LENGTH, String.valueOf(body.length));

		getSignature(this.accessKey, method.toString(), headers, parameters);
		URI uri =  getHostURIByIp(serverIp);

		RequestMessage request = buildRequest(uri, method, parameters, headers,
				new ByteArrayInputStream(body), body.length);
		ResponseMessage response = null;
		try {
			response = this.serviceClient.sendRequest(request, Consts.UTF_8_ENCODING);

			extractResponseBody(response);
			if (output_header != null) {
				output_header.putAll(response.getHeaders());
			}
			int statusCode = response.getStatusCode();
			if (statusCode != Consts.CONST_HTTP_OK) {
				String requestId = GetRequestId(response.getHeaders());
				JSONObject object = parserResponseMessage(response, requestId);
				ErrorCheck(object, requestId);
			}
		} catch (ServiceException e) {
			throw new LogException("RequestError", "Web request failed: "
					+ e.getMessage(), e, "");
		} catch (ClientException e) {
			throw new LogException("RequestError", "Web request failed: "
					+ e.getMessage(), e, "");
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
			}

		}
		return response;
	}

	private static RequestMessage buildRequest(URI endpoint,
											   HttpMethod httpMethod,
											   Map<String, String> parameters, Map<String, String> headers,
											   InputStream content, long size) {
		RequestMessage request = new RequestMessage();
		request.setMethod(httpMethod);
		request.setEndpoint(endpoint);
		request.setParameters(parameters);
		request.setHeaders(headers);
		request.setContent(content);
		request.setContentLength(size);

		return request;
	}

	private String BuildUrlParameter(Map<String, String> paras) {
		Map<String, String> treeMap = new TreeMap<String, String>(paras);
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for (Map.Entry<String, String> entry : treeMap.entrySet()) {
			if (isFirst == true) {
				isFirst = false;
			} else {
				builder.append("&");
			}
			builder.append(entry.getKey()).append("=").append(entry.getValue());
		}
		return builder.toString();
	}

	private String GetMapValue(Map<String, String> map, String key) {
		if (map.containsKey(key)) {
			return map.get(key);
		} else {
			return "";
		}
	}

	private String GetCanonicalizedHeaders(Map<String, String> headers) {
		Map<String, String> treeMap = new TreeMap<String, String>(headers);
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for (Map.Entry<String, String> entry : treeMap.entrySet()) {
			if (!entry.getKey().startsWith(Consts.CONST_X_SLS_PREFIX)
					&& !entry.getKey().startsWith(Consts.CONST_X_ACS_PREFIX)) {
				continue;
			}
			if (isFirst == true) {
				isFirst = false;
			} else {
				builder.append("\n");
			}
			builder.append(entry.getKey()).append(":").append(entry.getValue());
		}
		return builder.toString();
	}


	private void getSignature(String accesskey, String verb,
							  Map<String, String> headers,
							  Map<String, String> urlParams) {
		StringBuilder builder = new StringBuilder();
		builder.append(verb).append("\n");
		builder.append(GetMapValue(headers, Consts.CONST_CONTENT_MD5)).append(
				"\n");
		builder.append(GetMapValue(headers, Consts.CONST_CONTENT_TYPE)).append(
				"\n");
		builder.append(GetMapValue(headers, Consts.CONST_DATE)).append("\n");
		builder.append(GetCanonicalizedHeaders(headers)).append("\n");
		if (urlParams.isEmpty() == false) {
			builder.append("?");
			builder.append(BuildUrlParameter(urlParams));
		}
		String signature = getSignature(accesskey, builder.toString());
		headers.put(Consts.CONST_AUTHORIZATION,
				Consts.CONST_HEADSIGNATURE_PREFIX + ":" + signature);
	}

	private static String getSignature(String accesskey, String data) {
		try {
			byte[] keyBytes = accesskey.getBytes(Consts.UTF_8_ENCODING);
			byte[] dataBytes = data.getBytes(Consts.UTF_8_ENCODING);
			Mac mac = Mac.getInstance(Consts.HMAC_SHA1_JAVA);
			mac.init(new SecretKeySpec(keyBytes, Consts.HMAC_SHA1_JAVA));
			String sig = new String(Base64.encodeBase64(mac.doFinal(dataBytes)));
			return sig;
		} catch (UnsupportedEncodingException e) { // actually these exceptions
			// should never happened
			throw new RuntimeException("Not Supported encoding method "
					+ Consts.UTF_8_ENCODING, e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Not Supported signature method "
					+ Consts.HMAC_SHA1, e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException("Failed to calcuate the signature", e);
		}
	}

	private static String GetLocalMachineIp() {
		InetAddressValidator validator = new InetAddressValidator();
		String candidate = new String();
		try {
			for (Enumeration<NetworkInterface> ifaces = NetworkInterface
					.getNetworkInterfaces(); ifaces.hasMoreElements();) {
				NetworkInterface iface = ifaces.nextElement();

				if (iface.isUp()) {
					for (Enumeration<InetAddress> addresses = iface
							.getInetAddresses(); addresses.hasMoreElements();) {

						InetAddress address = addresses.nextElement();

						if (address.isLinkLocalAddress() == false
								&& address.getHostAddress() != null) {
							String ipAddress = address.getHostAddress();
							if (ipAddress.equals(Consts.CONST_LOCAL_IP)) {
								continue;
							}
							if (validator.isValidInet4Address(ipAddress)) {
								return ipAddress;
							}
							if (validator.isValid(ipAddress)) {
								candidate = ipAddress;
							}
						}
					}
				}
			}
		} catch (SocketException e) {

		}
		return candidate;
	}


	private com.alibaba.fastjson.JSONArray parseResponseMessageToArrayWithFastJson(ResponseMessage response,
																				   String requestId) throws LogException {
		byte[] body = response.getRawBody();
		if (body == null) {
			throw new LogException("BadResponse", "The response body is null",
					null, requestId);
		}
		String returnStr;
		try {
			returnStr = new String(body, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new LogException("BadResponse",
					"The response is not valid utf-8 string : ", e, requestId);
		}

		try {
			com.alibaba.fastjson.JSONArray array = com.alibaba.fastjson.JSONArray.parseArray(returnStr);
			return array;
		} catch (com.alibaba.fastjson.JSONException e) {
			throw new LogException("BadResponse",
					"The response is not valid json string : " + returnStr, e,
					requestId);
		}
	}

	private void extractLogsWithFastJson(QueryLogsResponse response, com.alibaba.fastjson.JSONArray logs) {
		try {
			for (int i = 0; i < logs.size(); i++) {
				com.alibaba.fastjson.JSONObject log = logs.getJSONObject(i);
				String source = new String();
				LogItem logItem = new LogItem();
				Set<String> keySet = log.keySet();
				for (String key:keySet) {
					String value = log.getString(key);
					if (key.equals(Consts.CONST_RESULT_SOURCE)) {
						source = value;
					} else if (key.equals(Consts.CONST_RESULT_TIME)) {
						logItem.mLogTime = Integer.parseInt(value);
					} else {
						logItem.PushBack(key, value);
					}
				}
				response.AddLog(new QueriedLog(source, logItem));
			}
		} catch (JSONException e) {
			// ignore;
		}

	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getRealIpForConsole() {
		return realIpForConsole;
	}

	public void setRealIpForConsole(String realIpForConsole) {
		this.realIpForConsole = realIpForConsole;
	}

	public boolean isUseSSLForConsole() {
		return useSSLForConsole;
	}

	public void setUseSSLForConsole(boolean useSSLForConsole) {
		this.useSSLForConsole = useSSLForConsole;
	}

	public void ClearConsoleResources() {
		realIpForConsole = null;
		useSSLForConsole = null;
	}

	public void EnableUUIDTag() {
		mUUIDTag = true;
	}

	public void DisableUUIDTag() {
		mUUIDTag = false;
	}

	public String GetSecurityToken() {
		return securityToken;
	}

	public void SetSecurityToken(String securityToken) {
		this.securityToken = securityToken;
	}

	public void RemoveSecurityToken() {
		securityToken = null;
	}



}
