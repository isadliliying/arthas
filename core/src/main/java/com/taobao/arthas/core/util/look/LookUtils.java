package com.taobao.arthas.core.util.look;

import com.alibaba.bytekit.utils.Pair;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.*;
import org.benf.cfr.reader.util.StringUtils;

import java.util.*;

/**
 * look命令工具
 */
public class LookUtils {


    /**
     * 解析转换 lookLoc
     */
    public static Pair<String, Integer> convertLookLoc(String lookLoc) {
        String[] arr = lookLoc.split("-");
        return Pair.of(arr[0], Integer.valueOf(arr[1]));
    }

    /**
     * 判断lookLoc格式是否合法
     */
    public static boolean validLookLocation(String lookLoc) {
        if (lookLoc == null || lookLoc.isEmpty()) {
            return false;
        }
        String[] arr = lookLoc.split("-");
        if (arr.length != 2) {
            return false;
        }
        return !com.taobao.arthas.core.util.StringUtils.isNumeric(lookLoc);
    }

    private static boolean match(AbstractInsnNode abstractInsnNode, Set<Integer> allowVariableSet) {
        if (abstractInsnNode instanceof VarInsnNode) {
            switch (abstractInsnNode.getOpcode()) {
                case Opcodes.ISTORE:
                case Opcodes.LSTORE:
                case Opcodes.FSTORE:
                case Opcodes.DSTORE:
                case Opcodes.ASTORE:
                    return allowVariableSet.contains(((VarInsnNode) abstractInsnNode).var);
            }
            return false;
        } else if (abstractInsnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
            if (methodInsnNode.owner.equals("java/lang/Byte") ||
                    methodInsnNode.owner.equals("java/lang/Short") ||
                    methodInsnNode.owner.equals("java/lang/Integer") ||
                    methodInsnNode.owner.equals("java/lang/Long") ||
                    methodInsnNode.owner.equals("java/lang/Boolean") ||
                    methodInsnNode.owner.equals("java/lang/Float")
            )
                return !methodInsnNode.name.equals("<init>");
            return true;
        }
        return false;
    }

    private static int findPreLineNumber(AbstractInsnNode insnNode) {
        while (insnNode != null) {
            if (insnNode instanceof LineNumberNode) {
                return ((LineNumberNode) insnNode).line;
            }
            insnNode = insnNode.getPrevious();
        }
        return 0;
    }

    public static String getLocalVarName(List<LocalVariableNode> varNodeList,VarInsnNode varInsnNode) {
        for (LocalVariableNode localVariable : varNodeList) {
            if (localVariable.index == varInsnNode.var){
                return localVariable.name;
            }
        }
        return null;
    }

    private static List<String> genContentList(List<AbstractInsnNode> nodeList, Map<Integer, String> varIdxMap) {
        List<String> contentList = new LinkedList<String>();
        for (AbstractInsnNode abstractInsnNode : nodeList) {
            String content = "";
            if (abstractInsnNode instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                content = "assign-variable: " + varIdxMap.get(varInsnNode.var);
            } else if (abstractInsnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                content = "invoke-method: " + methodInsnNode.owner + "#" + methodInsnNode.name + ":" + methodInsnNode.desc;
            }
            contentList.add(content);
        }
        return contentList;
    }

    private static List<Integer> genLineNumberList(List<AbstractInsnNode> nodeList) {
        List<Integer> preLineNumberList = new LinkedList<Integer>();
        for (AbstractInsnNode abstractInsnNode : nodeList) {
            int preLineNumber = findPreLineNumber(abstractInsnNode);
            preLineNumberList.add(preLineNumber);
        }
        return preLineNumberList;
    }

    private static List<AbstractInsnNode> filterNodeList(InsnList insnList, Map<Integer, String> varIdxMap) {
        List<AbstractInsnNode> noteList = new LinkedList<AbstractInsnNode>();
        for (AbstractInsnNode abstractInsnNode : insnList) {
            if (match(abstractInsnNode, varIdxMap.keySet())) {
                noteList.add(abstractInsnNode);
            }
        }
        return noteList;
    }

    private static List<String> renderMethod(MethodNode methodNode) {
        List<String> printLines = new LinkedList<String>();

        Map<Integer, String> varIdxMap = new HashMap<Integer, String>();
        for (LocalVariableNode localVariable : methodNode.localVariables) {
            varIdxMap.put(localVariable.index, localVariable.name);
        }

        List<AbstractInsnNode> noteList = filterNodeList(methodNode.instructions, varIdxMap);

        List<String> contentList = genContentList(noteList, varIdxMap);
        List<Integer> preLineNumberList = genLineNumberList(noteList);

        List<Pair<String, String>> contentUniqPairList = genContentUniqPair(contentList);

        for (int i = 0; i < contentUniqPairList.size(); i++) {
            Pair<String, String> contentUniqPair = contentUniqPairList.get(i);
            Integer preLineNumber = preLineNumberList.get(i);
            String printLine = "/*" + preLineNumber + "*/   " + contentUniqPair.second + "   " + contentUniqPair.first;
            printLines.add(printLine);
        }

        return printLines;
    }

    public static String render(MethodNode methodNode) {
        List<String> lineList = renderMethod(methodNode);
        return StringUtils.join(lineList, "\n");
    }

    private static Map<String, AbstractInsnNode> genUniqMapNode(MethodNode methodNode, int uniqLength) {
        //本地变量表
        Map<Integer, String> varIdxMap = new HashMap<Integer, String>();
        for (LocalVariableNode localVariable : methodNode.localVariables) {
            varIdxMap.put(localVariable.index, localVariable.name);
        }
        //filter node
        List<AbstractInsnNode> nodeList = new LinkedList<AbstractInsnNode>();
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (match(abstractInsnNode, varIdxMap.keySet())) {
                nodeList.add(abstractInsnNode);
            }
        }
        //拼凑出content
        List<String> contentList = new LinkedList<String>();
        for (AbstractInsnNode abstractInsnNode : nodeList) {
            String content = "";
            if (abstractInsnNode instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                content = "assign-variable: " + varIdxMap.get(varInsnNode.var);
            } else if (abstractInsnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                content = "invoke-method: " + methodInsnNode.owner + "#" + methodInsnNode.name + ":" + methodInsnNode.desc;
            }
            contentList.add(content);
        }
        //构建拼凑map
        Map<String, AbstractInsnNode> uniqMapNode = new HashMap<String, AbstractInsnNode>();
        Map<String, Integer> contentMapIdx = new HashMap<String, Integer>();
        for (int i = 0; i < contentList.size(); i++) {
            String c = contentList.get(i);
            Integer lastIdx = contentMapIdx.get(c);
            int curIdx = lastIdx == null ? 1 : ++lastIdx;
            contentMapIdx.put(c, curIdx);

            String md5 = Md5Utils.md5DigestAsHex(c.getBytes()).substring(0, uniqLength);

            String uniq = md5 + "-" + curIdx;
            uniqMapNode.put(uniq, nodeList.get(i));
        }
        return uniqMapNode;
    }

    public static AbstractInsnNode findInsnNode(MethodNode methodNode, String uniq) {
        String[] arr = uniq.trim().split("-");
        String mark = arr[0];
        Map<String, AbstractInsnNode> uniqMap = genUniqMapNode(methodNode, mark.length());
        return uniqMap.get(uniq);
    }

    /**
     * 生成content映射的
     */
    private static List<Pair<String, String>> genContentUniqPair(List<String> contentList) {
        int preSize = contentList.size();
        List<Pair<String, String>> contentUniqPairList = new LinkedList<Pair<String, String>>();
        Set<String> contentSet = new HashSet<String>(contentList);
        //采集md5
        Map<String, String> contentMapMd5 = new HashMap<String, String>(preSize);
        for (String content : contentSet) {
            String project = Md5Utils.md5DigestAsHex(content.getBytes());
            contentMapMd5.put(content, project);
        }
        //寻找合适长度
        int length = findUniqLength(contentMapMd5.values());
        //生成map
        Map<String, Integer> contentMapIdx = new HashMap<String, Integer>();
        for (String c : contentList) {
            //维护idx
            Integer lastIdx = contentMapIdx.get(c);
            int curIdx = lastIdx == null ? 1 : ++lastIdx;
            contentMapIdx.put(c, curIdx);

            String uniq = c + "-" + curIdx;
            if (length != -1) {
                String md5 = contentMapMd5.get(c);
                uniq = md5.substring(0, length) + "-" + curIdx;
            }
            contentUniqPairList.add(Pair.of(c, uniq));
        }
        return contentUniqPairList;
    }

    /**
     * 暂时使用md5，初始给4位
     */
    private static int findUniqLength(Collection<String> md5List) {
        Set<String> md5Set = new HashSet<String>(md5List);
        if (md5Set.size() != md5List.size()) {
            return -1;
        }
        for (int i = 4; i < 32; i++) {
            Set<String> uniqSet = new HashSet<String>(md5Set.size());
            for (String md5 : md5Set) {
                String uniq = md5.substring(0, i);
                if (!uniqSet.add(uniq)) {
                    break;
                }
            }
            if (uniqSet.size() == md5Set.size()) {
                return i;
            }
        }
        return -1;
    }

}
