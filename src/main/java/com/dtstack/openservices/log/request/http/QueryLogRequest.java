package com.dtstack.openservices.log.request.http;


/**
 * <p>
 *     具体实现参照cf=>http://confluence.dev.dtstack.cn/pages/viewpage.action?pageId=5603929
 *
 *     接口定义如下:
 *     <pre>
 *         {
 *            "userToken": "",
 *            "appname": "",
 *            "query": "",
 *            "startTime": "yyyy-MM-dd HH:mm:ss",
 *            "endTime": "yyyy-MM-dd HH:mm:ss"
 *          }
 *     </pre>
 *
 * </p>
 */
public class QueryLogRequest {

    private String startTime;

    private String endTime;

    private String query;

    // 可选字段
    private String appname;

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }
}
