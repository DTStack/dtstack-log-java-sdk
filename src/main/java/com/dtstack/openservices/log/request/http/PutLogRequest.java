package com.dtstack.openservices.log.request.http;

import java.util.List;

public class PutLogRequest {

    private List<String> logs;

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }
}
