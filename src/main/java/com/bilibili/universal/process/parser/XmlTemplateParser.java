/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.parser;

import static com.bilibili.universal.process.consts.XmlParseConstant.*;

import java.io.InputStream;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bilibili.universal.process.model.initialization.XmlProcessAction;
import com.bilibili.universal.process.model.initialization.XmlProcessHandler;
import com.bilibili.universal.process.model.initialization.XmlProcessStatus;
import com.bilibili.universal.process.model.initialization.XmlProcessTemplate;
import com.bilibili.universal.util.log.LoggerUtil;

/**
 * A util class to parse template xml file for initialization.
 *
 * @author Tony Zhao
 * @version $Id: XmlTemplateParser.java, v 0.1 2021-07-21 3:37 PM Tony Zhao Exp $$
 */
public class XmlTemplateParser {

    private static final Logger logger = LoggerFactory.getLogger(XmlTemplateParser.class);

    public static XmlProcessTemplate parse(InputStream stream) {
        XmlProcessTemplate xp = new XmlProcessTemplate();

        try {
            Node root = StreamParser.parse(stream);

            parseAttrs(xp, parseAttrs(root));

            parseMetadata(root, xp);

            parseInit(root, xp);

            parseTriggers(root, xp);

            parseActions(root, xp);

        } catch (Exception e) {
            LoggerUtil.error(logger, e, "System could not initialize process templates, ex=",
                e.getMessage());
        }

        return xp;
    }

    private static void parseMetadata(Node template, XmlProcessTemplate xp) {
        List<Node> metadata = getNodes(template, SECTION_ROOT_METADATA);
        if (CollectionUtils.isEmpty(metadata)) {
            xp.setStatus(new ArrayList<>());
        } else {
            xp.setStatus(parseStatusList(metadata.get(0)));
        }
    }

    private static List<XmlProcessStatus> parseStatusList(Node statusParentNode) {
        List<XmlProcessStatus> statusList = new ArrayList<>();

        for (Node s : getNodes(statusParentNode, SECTION_INNER_STATUS)) {
            statusList.add(parseStatus(parseAttrs(s)));
        }

        // don't forget to sort status after read from xml file
        Collections.sort(statusList);

        return statusList;
    }

    private static XmlProcessStatus parseStatus(Map<String, String> attr) {
        // a single status node created
        XmlProcessStatus xs = new XmlProcessStatus();

        xs.setNo(getValue(attr, ATTR_NO, -1));
        xs.setSequence(getValue(attr, ATTR_SEQUENCE, -1));
        xs.setName(getValue(attr, ATTR_NAME, EMPTY));
        xs.setDesc(getValue(attr, ATTR_DESC, EMPTY));
        xs.setPs(getValue(attr, ATTR_PARENT_STATUS, -1));

        // indicate which status is normal accomplish
        xs.setAc(getValue(attr, ATTR_ACCOMPLISH, 0));

        // parse default dst
        xs.setDefaultDst(getValue(attr, ATTR_DEFAULT, false));

        return xs;
    }

    private static void parseInit(Node template, XmlProcessTemplate xp) {
        List<Node> initSection = getNodes(template, SECTION_ROOT_INIT);
        if (CollectionUtils.isEmpty(initSection)) {
            xp.setInits(new HashMap<>());
        } else {
            xp.setInits(scanInit(initSection.get(0)));
        }
    }

    private static Map<Integer, List<XmlProcessHandler>> scanInit(Node sectionNode) {
        List<XmlProcessHandler> preHandlers = innerHandlers(sectionNode, SECTION_INNER_PRE);
        List<XmlProcessHandler> postHandlers = innerHandlers(sectionNode, SECTION_INNER_POST);

        Map<Integer, List<XmlProcessHandler>> destinations = new HashMap<>();
        List<Node> dispatch = getNodes(sectionNode, SECTION_INNER_DISPATCH);
        if (!CollectionUtils.isEmpty(dispatch)) {
            for (Node des : getNodes(dispatch.get(0), SECTION_THIRD_DESTINATION)) {

                Map<String, String> attrs = parseAttrs(des);

                // destination should not be null, or else should throw ex.
                Integer destination = Integer.parseInt(attrs.get(ATTR_NO));

                destinations.put(destination, parseHandlers(des));
            }
        }

        // rearrange
        Map<Integer, List<XmlProcessHandler>> finalDispatch = new HashMap<>();
        for (Map.Entry<Integer, List<XmlProcessHandler>> entry : destinations.entrySet()) {
            List<XmlProcessHandler> ha = new ArrayList<>(preHandlers);
            ha.addAll(entry.getValue());
            ha.addAll(postHandlers);
            finalDispatch.put(entry.getKey(), ha);
        }

        return finalDispatch;
    }

    private static void parseTriggers(Node template, XmlProcessTemplate xp) {
        xp.setAccepts(innerHandlers(template, SECTION_ROOT_ACCEPT));
        xp.setRejects(innerHandlers(template, SECTION_ROOT_REJECT));
        xp.setCancels(innerHandlers(template, SECTION_ROOT_CANCEL));
    }

    private static void parseActions(Node template, XmlProcessTemplate xp) {
        List<XmlProcessAction> actions = new ArrayList<>();

        List<Node> actionSection = getNodes(template, SECTION_ROOT_ACTION);
        if (!CollectionUtils.isEmpty(actionSection)) {
            for (Node ac : actionSection) {
                List<XmlProcessHandler> handlers = new ArrayList<>(parseHandlers(ac));
                actions.add(parseAction(parseAttrs(ac), handlers));
            }
        }

        // status trans actions
        xp.setActions(actions);
    }

    private static XmlProcessAction parseAction(Map<String, String> attr,
                                                List<XmlProcessHandler> handlers) {
        XmlProcessAction xa = new XmlProcessAction();

        // id should not duplicate!!
        xa.setId(Integer.parseInt(attr.get(ATTR_ID)));

        xa.setName(getValue(attr, ATTR_NAME, EMPTY));
        xa.setDesc(getValue(attr, ATTR_DESC, EMPTY));
        xa.setEntrance(getValue(attr, ATTR_ENTRANCE, false));

        xa.setSource(getValue(attr, ATTR_SOURCE, -1));
        xa.setDestination(getValue(attr, ATTR_DESTINATION, -1));

        xa.setHandlers(handlers);

        return xa;
    }

    private static List<XmlProcessHandler> innerHandlers(Node current, String handlerParentTag) {
        List<XmlProcessHandler> handlers = new ArrayList<>();

        List<Node> nodes = getNodes(current, handlerParentTag);
        if (!CollectionUtils.isEmpty(nodes)) {
            handlers.addAll(parseHandlers(nodes.get(0)));
        }

        return handlers;
    }

    private static List<XmlProcessHandler> parseHandlers(Node handlerParentNode) {
        List<XmlProcessHandler> handlers = new ArrayList<>();
        for (Node ha : getNodes(handlerParentNode, SECTION_INNER_HANDLER)) {
            handlers.add(parseHandler(parseAttrs(ha)));
        }

        // don't forget to sort handlers after read from xml file
        Collections.sort(handlers);

        return handlers;
    }

    private static XmlProcessHandler parseHandler(Map<String, String> attr) {
        // a new handler created...
        XmlProcessHandler xh = new XmlProcessHandler();

        xh.setSequence(getValue(attr, ATTR_SEQUENCE, 1));
        xh.setRefBeanId(getValue(attr, ATTR_REFERENCE, EMPTY));
        xh.setDesc(getValue(attr, ATTR_DESC, EMPTY));

        // parse handler transaction
        xh.setTrans(getValue(attr, ATTR_TRANS, true));

        // parse prepare parameter for next process's template id
        xh.setPrepareId(getValue(attr, ATTR_PREPARE, -1));

        return xh;
    }

    private static boolean matchNode(Node node, String search) {
        return node != null && search.equals(node.getNodeName());
    }

    private static List<Node> getNodes(Node root, String childName) {
        return getNodes(root, childName, 0);
    }

    @Deprecated
    private static List<Node> getNodes(Node root, String childName, int depth) {

        List<Node> result = new ArrayList<>();

        if (childName.equals(root.getNodeName())) {
            result.add(root);
            return result;
        }

        NodeList children = root.getChildNodes();
        int len = children.getLength();

        if (len == 0 || depth > 5) {
            // skip recursive exceed five cascades
            return result;
        }

        for (int i = 0; i < len; i++) {
            Node node = children.item(i);
            if (matchNode(node, childName)) {
                result.add(node);
            }
        }

        if (CollectionUtils.isEmpty(result)) {
            // not found in current level
            // deep dive into second level
            int j = 0;
            while (j < len) {
                Node node = children.item(j++);
                List<Node> onceResult = getNodes(node, childName, depth + 1);
                if (!CollectionUtils.isEmpty(onceResult)) {
                    result.addAll(onceResult);
                }
            }
        }

        return result;
    }

    private static Map<String, String> parseAttrs(Node node) {
        Map<String, String> attrMap = new HashMap<>();

        NamedNodeMap nodeAttrs = node.getAttributes();
        int len = nodeAttrs.getLength();

        if (len <= 0) {
            return attrMap;
        }

        for (int i = 0; i < len; i++) {
            String attrName = nodeAttrs.item(i).getNodeName();
            String attrValue = nodeAttrs.item(i).getNodeValue();
            attrMap.put(attrName, attrValue);
        }

        return attrMap;
    }

    private static void parseAttrs(XmlProcessTemplate xp, Map<String, String> attr) {
        xp.setId(Integer.parseInt(attr.get(ATTR_ID)));
        xp.setName(attr.get(ATTR_NAME));
        xp.setDesc(attr.get(ATTR_DESC));

        // parentId
        xp.setParent(getValue(attr, ATTR_PARENT, -1));

        // reconcile 0 represents no reconcile
        xp.setReconcile(getValue(attr, ATTR_RECONCILE, 0));

        // coordinate 1 represents and &&
        xp.setCoordinate(getValue(attr, ATTR_COORDINATE, 1));
    }

    private static boolean getValue(Map<String, String> attr, String key, boolean defaultValue) {
        if (attr.containsKey(key)) {
            String value = attr.get(key);
            return value == null ? defaultValue : Boolean.parseBoolean(value);
        }

        return defaultValue;
    }

    private static int getValue(Map<String, String> attr, String key, int defaultValue) {
        if (attr.containsKey(key)) {
            String value = attr.get(key);
            return value == null ? defaultValue : Integer.parseInt(value);
        }

        return defaultValue;
    }

    private static String getValue(Map<String, String> attr, String key, String defaultValue) {
        if (attr.containsKey(key)) {
            String value = attr.get(key);
            return value == null ? defaultValue : value;
        }

        return defaultValue;
    }

}