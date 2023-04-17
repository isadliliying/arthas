package com.taobao.arthas.core.command.model;

public class RecorderModel extends ResultModel {

    private String content;

    public RecorderModel() {
    }

    public RecorderModel(String content) {
        this.content = content;
    }

    @Override
    public String getType() {
        return "recorder";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
