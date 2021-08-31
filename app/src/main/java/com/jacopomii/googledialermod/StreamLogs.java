package com.jacopomii.googledialermod;

public class StreamLogs {
    private String mInputStreamLog;
    private String mErrorStreamLog;
    private String mOutputStreamLog;

    public StreamLogs() {}

    public String getInputStreamLog() {
        if (mInputStreamLog == null)
            return "";
        else
            return mInputStreamLog.trim();
    }

    public String getErrorStreamLog() {
        if (mErrorStreamLog == null)
            return "";
        else
            return mErrorStreamLog.trim();
    }

    public String getOutputStreamLog() {
        if (mOutputStreamLog == null)
            return "";
        else
            return mOutputStreamLog.trim();
    }

    public void setInputStreamLog(String inputStreamLog) {
        mInputStreamLog = inputStreamLog;
    }

    public void setErrorStreamLog(String errorStreamLog) {
        mErrorStreamLog = errorStreamLog;
    }

    public void setOutputStreamLog(String outputStreamLog) {
        mOutputStreamLog = outputStreamLog;
    }

    public String getInputStreamLogWithLabel() {
        return "\tInputStream:\n\t\t" + getInputStreamLog().replaceAll("\n", "\n\t\t");
    }

    public String getErrorStreamLogWithLabel() {
        return "\tErrorStream:\n\t\t" + getErrorStreamLog().replaceAll("\n", "\n\t\t");
    }

    public String getOutputStreamLogWithLabel() {
        return "\tOutputStream:\n\t\t" + getOutputStreamLog().replaceAll("\n", "\n\t\t");
    }

    public String getStreamLogsWithLabels() {
        String result = "\n" + getOutputStreamLogWithLabel();

        if (!getInputStreamLog().isEmpty()) {
            result += "\n" + getInputStreamLogWithLabel();
        }

        if (!getErrorStreamLog().isEmpty()) {
            result += "\n" + getErrorStreamLogWithLabel();
        }

        return result;
    }
}
