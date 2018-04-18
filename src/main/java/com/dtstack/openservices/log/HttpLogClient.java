package com.dtstack.openservices.log;

import com.dtstack.openservices.log.common.Consts;
import com.dtstack.openservices.log.exception.LogException;
import com.dtstack.openservices.log.http.client.ClientConfiguration;
import com.dtstack.openservices.log.http.client.ClientException;
import com.dtstack.openservices.log.http.client.HttpMethod;
import com.dtstack.openservices.log.http.client.ServiceException;
import com.dtstack.openservices.log.http.comm.DefaultServiceClient;
import com.dtstack.openservices.log.http.comm.RequestMessage;
import com.dtstack.openservices.log.http.comm.ResponseMessage;
import com.dtstack.openservices.log.http.comm.ServiceClient;
import com.dtstack.openservices.log.request.http.PutLogRequest;
import com.dtstack.openservices.log.request.http.QueryLogRequest;
import com.dtstack.openservices.log.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     基于json数据协议实现的日志客户端，对于一般场景下使用更简单轻量级.
 *     当前支持最基本的日志上传，日志查询。
 * </p>
 * @author qingya@dtstack.com
 */
public class HttpLogClient {

    private String endpoint;

    private String accessKey;

    private String hostName;

    private String httpType;

    private ServiceClient serviceClient;

    private Map<String,String> majorParameters = new HashMap<>();

    private static class SingletonHolder {
        private static final HttpLogClient INSTANCE = new HttpLogClient();
    }

    public HttpLogClient() {
    }

    public static final HttpLogClient getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public HttpLogClient(String endpoint, String accessKey) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;

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

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setMaxConnections(Consts.HTTP_CONNECT_MAX_COUNT);
        clientConfig.setConnectionTimeout(Consts.HTTP_CONNECT_TIME_OUT);
        clientConfig.setSocketTimeout(Consts.HTTP_SEND_TIME_OUT);
        serviceClient = new DefaultServiceClient(clientConfig);
    }

    public HttpLogClient(String endpoint, String accessKey, Map<String, String> majorParameters) {
        this(endpoint, accessKey);
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.majorParameters = majorParameters;
    }

    /**
     * <p>
     *     上传日志
     * </p>
     * @param request
     */
    public void putLog(PutLogRequest request){
        validateMajor();



    }

    /**
     * <p>
     *     查询日志
     * </p>
     * @param request
     */
    public void queryLog(QueryLogRequest request){


    }

    private void validateMajor(){

        Assert.assertTrue(majorParameters.containsKey("appname"), "请设置app_name参数");
        Assert.assertTrue(majorParameters.containsKey("userToken"), "请设置userToken参数");
        Assert.assertTrue(majorParameters.containsKey("keeptype"), "请设置keep_type参数");

    }

    private void sendData(HttpMethod method,
                          Map<String, String> parameters, Map<String, String> headers, byte[] body) throws LogException{

        URI uri = getHostURIByIp(endpoint);
        RequestMessage request = buildRequest(uri, method, parameters, headers,
                new ByteArrayInputStream(body), body.length);
        ResponseMessage response = null;
        try {
            response = this.serviceClient.sendRequest(request, Consts.UTF_8_ENCODING);


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

    private URI getHostURIByIp(String ipAddress) throws LogException
    {
        String endPointUrl = this.httpType + ipAddress;
        try {
            return new URI(endPointUrl);
        } catch (URISyntaxException e) {
            throw new LogException("EndpointInvalid",
                    "Failed to get real server ip when direct mode in enabled", "");
        }
    }


    private static RequestMessage buildRequest(URI endpoint,
                                               HttpMethod httpMethod,
                                               Map<String, String> parameters, Map<String, String> headers,
                                               InputStream content, long size) {
        RequestMessage request = new RequestMessage();
        request.setMethod(httpMethod);
        request.setEndpoint(endpoint);
        request.setParameters(parameters);
        request.setContent(content);
        request.setContentLength(size);
        request.setHeaders(headers);
        return request;
    }



}
