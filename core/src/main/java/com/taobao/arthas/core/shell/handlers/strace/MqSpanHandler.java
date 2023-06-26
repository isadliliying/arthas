package com.taobao.arthas.core.shell.handlers.strace;

import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MqSpanHandler extends AbstractSpanHandler {

    public static final Map<String, Pair<String, String>> signatureMapOgnlExpress = new HashMap<String, Pair<String, String>>();

    static {
        signatureMapOgnlExpress.put("(Lcom/seewo/framework/mq/client/model/Message;)Lcom/seewo/framework/mq/client/model/SendingResult;", Pair.make("#msgList={params[0]},#msgListArray=#msgList.{#msg=#this,#{\"msgId\":#this.getMsgId(),\"topic\":#this.getTopic(),\"tags\":#this.getTags(),\"keys\":#this.getKeys(),\"body\":@com.alibaba.fastjson.JSON@parse(new java.lang.String(#msg.getBody()))}},#{\"msgList\":#msgListArray}", "#{\"sendResult\":returnObj}"));
        signatureMapOgnlExpress.put("(Ljava/util/List;)Lcom/seewo/framework/mq/client/model/SendingResult;", Pair.make("#msgList=params[0],#msgListArray=#msgList.{#msg=#this,#{\"msgId\":#this.getMsgId(),\"topic\":#this.getTopic(),\"tags\":#this.getTags(),\"keys\":#this.getKeys(),\"body\":@com.alibaba.fastjson.JSON@parse(new java.lang.String(#msg.getBody()))}},#{\"msgList\":#msgListArray}", "#{\"sendResult\":returnObj}"));
        signatureMapOgnlExpress.put("(Lcom/seewo/framework/mq/client/model/Message;Lcom/seewo/framework/mq/client/SendingBack;)V", Pair.make("#msgList={params[0]},#msgListArray=#msgList.{#msg=#this,#{\"msgId\":#this.getMsgId(),\"topic\":#this.getTopic(),\"tags\":#this.getTags(),\"keys\":#this.getKeys(),\"body\":@com.alibaba.fastjson.JSON@parse(new java.lang.String(#msg.getBody()))}},#{\"msgList\":#msgListArray}", "#{\"sendResult\":\"using call back\"}"));
        signatureMapOgnlExpress.put("(Lcom/seewo/framework/mq/client/model/Message;Ljava/lang/Object;)Lcom/seewo/framework/mq/client/model/SendingResult;", Pair.make("#msgList={params[0]},#msgListArray=#msgList.{#msg=#this,#{\"msgId\":#this.getMsgId(),\"topic\":#this.getTopic(),\"tags\":#this.getTags(),\"keys\":#this.getKeys(),\"body\":@com.alibaba.fastjson.JSON@parse(new java.lang.String(#msg.getBody()))}},#{\"msgList\":#msgListArray}", "#{\"sendResult\":returnObj}"));
    }

    @Override
    public boolean hasMatch(Line line) {
        return line.getMark().equals("ProducerTemplate#sendMsg") || line.getMark().equals("ProducerTemplate#sendMsgOrderly");
    }

    @Override
    public Pair<String, String> getEnterMark(Line line) {
        try {
            JSONObject msg = (JSONObject) line.getObject().getJSONArray("msgList").get(0);
            String info = msg.getString("topic") + "#" + msg.getString("tags");
            return Pair.make("[mq]", "[" + info + "]");
        } catch (Exception e) {
            return Pair.make("[mq]", "unknown");
        }
    }

    @Override
    public boolean hasMatch(Advice advice) {
        String methodDesc = advice.getMethod().getMethodDesc();
        return signatureMapOgnlExpress.containsKey(methodDesc);
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        String methodDesc = advice.getMethod().getMethodDesc();
        return signatureMapOgnlExpress.get(methodDesc).getFirst();
    }

    @Override
    public String exitOgnlExpress(Advice advice) {
        String methodDesc = advice.getMethod().getMethodDesc();
        return signatureMapOgnlExpress.get(methodDesc).getSecond();
    }

}
