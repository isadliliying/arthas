package com.taobao.arthas.core.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.command.model.Line;
import com.taobao.arthas.core.command.model.MethodNode;
import com.taobao.arthas.core.command.model.ThreadNode;
import com.taobao.arthas.core.command.model.TraceTree;
import com.taobao.arthas.core.shell.handlers.strace.SpanHandler;
import com.taobao.arthas.core.shell.handlers.strace.SpanHandlerHolder;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class StraceUtils {
    public static TraceTree transformFile(String inputFileName, String outputFileName, ThreadNode threadNode) {
        File logFile = new File(inputFileName);
        if (!logFile.exists()) {
            return null;
        }

        try {
            TraceTree tree = new TraceTree(threadNode);

            Node root = new Node();
            root.setMark("root");
            LinkedList<Line> queue = new LinkedList<Line>();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
            try {
                Node currentNode = root;
                String tmp = bufferedReader.readLine();
                while (tmp != null) {
                    Line line = parseLine(tmp);
                    try {
                        if (line == null) {
                            continue;
                        }

                        //只有enter才会生成mark
                        Pair<String, String> mark = buildMark(line);
                        if (line.getPhrase().equals("enter")) {
                            tree.begin(mark.getFirst(), mark.getSecond(), line.getTimestamp());
                        } else {
                            Object obj = line.getObject().get("exception");
                            tree.end(obj != null, line.getTimestamp());
                        }

                        Line queueLine = queue.isEmpty() ? null : queue.getLast();

                        if (line.getPhrase().equals("enter")) {
                            Node node = new Node();
                            node.setMark(mark.getFirst() + "#" + mark.getSecond());
                            queue.addLast(line);

                            node.setParent(currentNode);
                            currentNode.getSubCallList().add(node);
                            currentNode = node;

                        } else if (queueLine != null && line.getPhrase().equals("exit") && queueLine.getPhrase().equals("enter")) {
                            Item item = combine(queueLine, line);
                            queue.removeLast();
                            currentNode.setType(item.type);
                            currentNode.setTakeTime(item.takeTime);
                            currentNode.setMark(item.mark);
                            currentNode.setData(item.object);
                            currentNode = currentNode.getParent();
                        }
                    } finally {
                        tmp = bufferedReader.readLine();
                    }
                }
            } finally {
                bufferedReader.close();
            }

            Node node = root.getSubCallList().get(0);

            File file = new File(outputFileName);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(JSON.toJSONString(node, SerializerFeature.PrettyFormat));
            fileWriter.flush();
            fileWriter.close();

            return tree;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Pair<String, String> buildMark(Line line) {
        if (line.getPhrase().equals("enter")) {
            SpanHandler spanHandler = SpanHandlerHolder.matchHandler(line,true);
            return spanHandler.getEnterMark(line);
        }
        return null;
    }

    public static Pair<String, Integer> findNextBlock(String text, int begin) {
        int fromIdx = text.indexOf('[', begin);
        if (fromIdx < 0) return null;
        int endIdx = text.indexOf(']', fromIdx + 1);
        if (endIdx < 0) return null;
        return Pair.make(text.substring(fromIdx + 1, endIdx), endIdx);
    }

    public static Pair<String, Integer> findLastBlock(String text, int begin) {
        int fromIdx = text.indexOf('[', begin);
        if (fromIdx < 0) return null;
        int endIdx = text.lastIndexOf(']');
        if (endIdx < 0) return null;
        return Pair.make(text.substring(fromIdx + 1, endIdx), endIdx);
    }

    public static Line parseLine(String tmp) {
        Line lineObj = new Line();
        Pair<String, Integer> pair = findNextBlock(tmp, 0);
        if (pair == null) return null;
        lineObj.setType(pair.getFirst());
        pair = findNextBlock(tmp, pair.getSecond());
        if (pair == null) return null;
        lineObj.setPhrase(pair.getFirst());
        pair = findNextBlock(tmp, pair.getSecond());
        if (pair == null) return null;
        lineObj.setTimestamp(Long.parseLong(pair.getFirst()));
        pair = findNextBlock(tmp, pair.getSecond());
        if (pair == null) return null;
        lineObj.setMark(pair.getFirst());


        pair = findLastBlock(tmp, pair.getSecond());
        if (pair == null) return null;
        lineObj.setObject(JSON.parseObject(pair.getFirst()));

        return lineObj;
    }

    public static Item combine(Line enter, Line exit) {
        Pair<String, String> mark = buildMark(enter);

        Item item = new Item();
        item.setMark(mark.getFirst() + "#" + mark.getSecond());
        item.setType(enter.getType());
        item.setTakeTime(exit.getTimestamp() - enter.getTimestamp());

        JSONObject jo = new JSONObject();
        jo.putAll(enter.getObject());
        jo.putAll(exit.getObject());
        item.setObject(jo);

        return item;
    }


    static class Item {

        private String type;

        private Long takeTime;

        private String mark;

        private JSONObject object;

        public Item() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getTakeTime() {
            return takeTime;
        }

        public void setTakeTime(Long takeTime) {
            this.takeTime = takeTime;
        }

        public String getMark() {
            return mark;
        }

        public void setMark(String mark) {
            this.mark = mark;
        }

        public JSONObject getObject() {
            return object;
        }

        public void setObject(JSONObject object) {
            this.object = object;
        }
    }

    static class Node {

        @JSONField(ordinal = 1)
        private String mark;
        @JSONField(ordinal = 2)
        private String type;
        @JSONField(ordinal = 3)
        private Long takeTime;
        @JSONField(ordinal = 4)
        private JSONObject data;
        @JSONField(serialize = false)
        private transient Node parent;
        @JSONField(ordinal = 5)
        private List<Node> subCallList = new LinkedList<Node>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getTakeTime() {
            return takeTime;
        }

        public void setTakeTime(Long takeTime) {
            this.takeTime = takeTime;
        }

        public String getMark() {
            return mark;
        }

        public void setMark(String mark) {
            this.mark = mark;
        }

        public JSONObject getData() {
            return data;
        }

        public void setData(JSONObject data) {
            this.data = data;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public List<Node> getSubCallList() {
            return subCallList;
        }

        public void setSubCallList(List<Node> subCallList) {
            this.subCallList = subCallList;
        }
    }
}
