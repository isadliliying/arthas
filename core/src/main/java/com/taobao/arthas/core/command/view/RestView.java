package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.RestModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.HttpUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

import java.util.List;
import java.util.Map;

public class RestView extends ResultView<RestModel> {

    @Override
    public void draw(CommandProcess process, RestModel model) {
        process.write("ts=" + DateUtils.formatDate(model.getTs()) + "; [cost=" + model.getCostInMs() + "ms]" + "\n");
        process.write("method:[" + model.getMethod() + "]\n");
        process.write("uri:[" + model.getUri() + "]\n");
        for (Map.Entry<String, List<String>> stringListEntry : model.getRequestHeaders().entrySet()) {
            String headerValue = StringUtils.join(stringListEntry.getValue().toArray(new String[0]), ";");
            process.write("request-header:[" + stringListEntry.getKey() + ":" + headerValue + "]\n");
        }

        String requestBodyResult = StringUtils.objectToString(
                model.getRequestBody().needExpand() ? new ObjectView(model.getSizeLimit(), model.getRequestBody()).draw() : model.getRequestBody().getObject());
        process.write("-------------------request-body-------------------\n");
        process.write(requestBodyResult + "\n");
        process.write("-------------------request-body-------------------\n");

        process.write("\n");
        process.write("status:[" + model.getStatus() + "]\n");
        for (Map.Entry<String, List<String>> stringListEntry : model.getResponseHeaders().entrySet()) {
            String headerValue = StringUtils.join(stringListEntry.getValue().toArray(new String[0]), ";");
            process.write("response-header:[" + stringListEntry.getKey() + ":" + headerValue + "]\n");
        }

        String responseBodyResult = StringUtils.objectToString(
                model.getResponseBody().needExpand() ? new ObjectView(model.getSizeLimit(), model.getResponseBody()).draw() : model.getResponseBody().getObject());
        process.write("-------------------response-body-------------------\n");
        process.write(responseBodyResult + "\n");
        process.write("-------------------response-body-------------------\n");

        String curl = HttpUtils.buildCUrl(model);
        process.write("-------------------curl-------------------\n");
        process.write(curl + "\n");
        process.write("-------------------curl-------------------\n");

    }

}
