/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import static com.bilibili.universal.process.consts.XmlParseConstant.TEMPLATE_PATH;
import static com.bilibili.universal.process.consts.XmlParseConstant.UNDERSCORE;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.enums.TemplateTriggerType;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.cache.TemplateMetadata;
import com.bilibili.universal.process.model.initialization.XmlProcessAction;
import com.bilibili.universal.process.model.initialization.XmlProcessHandler;
import com.bilibili.universal.process.model.initialization.XmlProcessStatus;
import com.bilibili.universal.process.model.initialization.XmlProcessTemplate;
import com.bilibili.universal.process.model.status.StatusPair;
import com.bilibili.universal.process.model.status.StatusRefMapping;
import com.bilibili.universal.process.parser.XmlTemplateParser;
import com.bilibili.universal.process.service.ProcessAssemble2ndService;
import com.bilibili.universal.process.service.ProcessMetadataService;
import com.bilibili.universal.util.code.SystemResultCode;
import com.bilibili.universal.util.common.AssertUtil;
import com.bilibili.universal.util.exception.SystemException;
import com.bilibili.universal.util.log.LoggerUtil;

/**
 * Service which parse and cache template action into memory. 
 * 
 * @author Tony Zhao
 * @version $Id: ProcessAssemble2ndServiceImpl.java, v 0.1 2022-01-29 5:42 PM Tony Zhao Exp $$
 */
@Service
public class ProcessAssemble2ndServiceImpl implements InitializingBean, ApplicationContextAware,
                                           ProcessAssemble2ndService, ProcessMetadataService {

    private static final Logger           logger           = LoggerFactory
        .getLogger(ProcessAssemble2ndServiceImpl.class);

    /** spring context injected */
    private ApplicationContext            applicationContext;

    /** is it necessary? consider later => template id => template cache */
    private Map<Integer, TemplateCache>   templateIdCache  = new HashMap<>();

    /** action id should be unique. action id => template cache */
    private Map<Integer, TemplateCache>   actionIdCache    = new HashMap<>();

    /** parent child tpl cache */
    private Map<Integer, List<Integer>>   parentChildCache = new HashMap<>();

    /** parent_child template id => status mapping search */
    private Map<String, StatusRefMapping> tplStatusRef     = new HashMap<>();

    @Override
    public void initialize(InputStream stream) {
        // parse template from xml definition
        XmlProcessTemplate xp = XmlTemplateParser.parse(stream);

        // cache parent tpl id
        cacheParent(xp);

        // parse action handlers and cache status
        mappingCache(cacheTemplate(xp));
    }

    @Override
    public void initStatusRef() {
        for (Map.Entry<Integer, List<Integer>> entry : parentChildCache.entrySet()) {
            final int pid = entry.getKey();
            List<Integer> ids = entry.getValue();

            if (CollectionUtils.isEmpty(ids)) {
                continue;
            }

            ids.forEach(cid -> {
                String unionKey = unionKey(pid, cid);

                Map<Integer, List<StatusCache>> p2c = new HashMap<>();
                Map<Integer, Integer> c2p = new HashMap<>();
                StatusRefMapping refMapping = new StatusRefMapping(unionKey, p2c, c2p);

                TemplateCache child = getTemplateById(cid);
                StatusCache[] status = child.getStatusArray();

                // do ref mapping
                for (int i = 0; i < status.length; i++) {
                    StatusCache s = status[i];
                    int no = s.getNo();
                    int pno = s.getPs();

                    // for robust.. -1 represents non-initialized
                    if (no == -1 || pno == -1) {
                        continue;
                    }

                    // 1st. build child to parent
                    c2p.put(no, pno);

                    List<StatusCache> childStatus = new ArrayList<>();
                    if (p2c.containsKey(pno)) {
                        childStatus = p2c.get(pno);
                    } else {
                        // 2nd. build parent to children
                        p2c.put(pno, childStatus);
                    }

                    childStatus.add(s);
                }

                tplStatusRef.put(unionKey, refMapping);
            });
        }
    }

    /**
     * Build action id reverse search template mapping.
     * 
     * @param template 
     */
    private void mappingCache(TemplateCache template) {
        // 1. template id => template
        TemplateMetadata info = template.getMetadata();
        templateIdCache.put(info.getId(), template);

        // 2. then action id => template
        Map<Integer, ActionCache> actions = template.getActions();
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }

        for (Map.Entry<Integer, ActionCache> entry : actions.entrySet()) {
            int actionId = entry.getKey();
            if (actionIdCache.containsKey(actionId)) {
                throw new SystemException(SystemResultCode.SYSTEM_ERROR,
                    "Each action id should be unique, now is duplicate!");
            }

            actionIdCache.put(actionId, template);
        }
    }

    private TemplateCache cacheTemplate(XmlProcessTemplate xp) {
        TemplateCache template = new TemplateCache(cacheMetadata(xp));

        Map<Integer, ActionCache> actions = new HashMap<>();
        Map<Integer, Map<Integer, StatusPair>> dstTable = new HashMap<>();
        Map<Integer, Map<Integer, ActionCache>> actionTable = new HashMap<>();

        // step1: action and its reverse reflection
        for (XmlProcessAction a : xp.getActions()) {
            int id = a.getId();
            int src = a.getSource();
            int des = a.getDestination();
            String name = a.getName();
            String desc = a.getDesc();

            List<ActionHandler> sync = new ArrayList<>();
            List<ActionHandler> async = new ArrayList<>();
            Map<String, Integer> prepare = new HashMap<>();
            ActionCache cache = new ActionCache(id, name, desc, src, des, sync, async, prepare);

            XmlProcessAction xa = xp.getActionById(id);
            for (XmlProcessHandler h : xa.getHandlers()) {
                ActionHandler ah = fetchHandler(h);
                if (h.isTrans()) {
                    sync.add(ah);
                } else {
                    async.add(ah);
                }

                // prepare params for processes recursive call
                if (h.getPrepareId() > 0) {
                    String n = ah.getClass().getName();
                    prepare.put(n, h.getPrepareId());
                }
            }

            actions.put(id, cache);
            twoKeyReflect(id, src, new StatusPair(src, des), dstTable);
            // special warning: dst => src => action, reverse order.
            twoKeyReflect(des, src, cache, actionTable);
        }

        // step2: status check array
        List<StatusCache> statusCache = new ArrayList<>();
        for (XmlProcessStatus s : xp.getStatus()) {
            statusCache.add(new StatusCache(s.getNo(), s.getSequence(), s.getPs(), s.getAc(),
                s.isDefaultDst()));
        }

        if (!CollectionUtils.isEmpty(statusCache)) {
            // sort for not empty since should including pipeline service non-state action.
            Collections.sort(statusCache);
        }

        // step3: initializer
        Map<Integer, List<ActionHandler>> initializer = new HashMap<>();
        Map<Integer, List<XmlProcessHandler>> inits = xp.getInits();
        for (Map.Entry<Integer, List<XmlProcessHandler>> entry : inits.entrySet()) {
            initializer.put(entry.getKey(), fetchHandlers(entry.getValue()));
        }

        // step4: triggers
        Map<String, List<ActionHandler>> triggers = new HashMap<>();
        triggers.put(TemplateTriggerType.ACCEPT.name(), fetchHandlers(xp.getAccepts()));
        triggers.put(TemplateTriggerType.REJECT.name(), fetchHandlers(xp.getRejects()));
        triggers.put(TemplateTriggerType.CANCEL.name(), fetchHandlers(xp.getCancels()));

        // step5: assemble
        template.setStatusArray(statusCache.toArray(new StatusCache[statusCache.size()]));
        template.setInitializers(initializer);
        template.setActions(actions);
        template.setTriggers(triggers);
        template.setDstTable(dstTable);
        template.setActionTable(actionTable);

        return template;
    }

    private TemplateMetadata cacheMetadata(XmlProcessTemplate xp) {
        int id = xp.getId();
        int parentId = xp.getParent();
        String name = xp.getName();
        String desc = xp.getDesc();
        int reconcile = xp.getReconcile();
        int coordinate = xp.getCoordinate();

        return new TemplateMetadata(id, parentId, name, desc, reconcile, coordinate);
    }

    private void cacheParent(XmlProcessTemplate xp) {
        int parentId = xp.getParent();
        int childId = xp.getId();
        if (parentId > 0) {
            List<Integer> children = new ArrayList<>();
            if (parentChildCache.containsKey(parentId)) {
                children = parentChildCache.get(parentId);
            } else {
                parentChildCache.put(parentId, children);
            }

            children.add(childId);
        }
    }

    @SuppressWarnings("rawtypes")
    private List<ActionHandler> fetchHandlers(List<XmlProcessHandler> handlers) {
        return Optional.ofNullable(handlers)
            .map(h -> h.stream().map(this::fetchHandler).collect(Collectors.toList()))
            .orElse(new ArrayList<>());
    }

    @SuppressWarnings("rawtypes")
    private ActionHandler fetchHandler(XmlProcessHandler handler) {
        return fetchService(handler.getRefBeanId());
    }

    @SuppressWarnings("rawtypes")
    private ActionHandler fetchService(String serviceId) {
        ActionHandler handlerService = null;

        try {
            handlerService = (ActionHandler) applicationContext.getBean(serviceId);
            AssertUtil.isNotNull(handlerService, SystemResultCode.SYSTEM_ERROR,
                "Springboot context doesn't have a bean with id=" + serviceId);

        } catch (Exception e) {
            LoggerUtil.error(logger, e,
                "Springboot context doesn't have a bean with id=" + serviceId, e.getMessage());

            // determine whether throw ex later...
            throw new SystemException(SystemResultCode.SYSTEM_ERROR, e,
                "Springboot context doesn't have a bean with id=" + serviceId);
        }

        return handlerService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // search process template xml file
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            // search all template files under classpath
            Resource[] resources = resolver.getResources(TEMPLATE_PATH);
            for (Resource resource : resources) {
                // 1st. initialize xml parse and metadata assemble
                initialize(resource.getInputStream());
            }

            // 2nd. refactor all parent and child status mapping
            initStatusRef();
        } catch (Exception e) {
            LoggerUtil.error(logger, e, e.getMessage(),
                "mission system scan classpath process template failed.");
            throw e;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public TemplateCache getTemplateById(int templateId) {
        return templateIdCache.get(templateId);
    }

    @Override
    public TemplateCache getTemplateByActionId(int actionId) {
        return actionIdCache.get(actionId);
    }

    @Override
    public int getDstByTemplateId(int templateId) {
        TemplateCache cache = getTemplateById(templateId);
        StatusCache[] status = cache.getStatusArray();
        int size = status.length;
        for (int i = 0; i < size; i++) {
            if (status[i].isDefaultDst()) {
                return status[i].getNo();
            }
        }

        // random use one 
        Map<Integer, List<ActionHandler>> inits = cache.getInitializers();
        for (Map.Entry<Integer, List<ActionHandler>> entry : inits.entrySet()) {
            return entry.getKey();
        }

        // no inits
        return -1;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<ActionHandler> getExecutions(int actionId, boolean sync) {
        ActionCache action = getActionCache(actionId);
        if (sync) {
            return action.getSyncHandlers();
        } else {
            return action.getAsyncHandlers();
        }
    }

    @Override
    public boolean isFinalStatus(int templateId, int status) {
        TemplateCache cache = getTemplateById(templateId);
        if (cache == null || CollectionUtils.isEmpty(cache.getDstTable())) {
            return false;
        }

        Map<Integer, Map<Integer, StatusPair>> table = cache.getDstTable();
        for (Map.Entry<Integer, Map<Integer, StatusPair>> entry : table.entrySet()) {
            Map<Integer, StatusPair> dst = entry.getValue();
            if (dst.containsKey(status)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int getACStatus(int templateId) {
        TemplateCache cache = getTemplateById(templateId);
        if (cache == null || cache.getStatusArray().length == 0) {
            return -1;
        }

        StatusCache[] statusCaches = cache.getStatusArray();
        for (int i = 0; i < statusCaches.length; i++) {
            if (statusCaches[i].getAccomplish() == 1) {
                return statusCaches[i].getNo();
            }
        }

        return -1;
    }

    @Override
    public boolean isParentTpl(int templateId) {
        return parentChildCache.containsKey(templateId);
    }

    @Override
    public StatusRefMapping getRefStatusMapping(int parentTemplateId, int childTemplateId) {
        return tplStatusRef.get(unionKey(parentTemplateId, childTemplateId));
    }

    private ActionCache getActionCache(int actionId) {
        TemplateCache cache = actionIdCache.get(actionId);
        Map<Integer, ActionCache> actions = cache.getActions();
        return actions.get(actionId);
    }

    /**
     * Build hashmap with double key like Map<T1, Map<T2, value>>.
     *
     * @param key1
     * @param key2
     * @param value
     * @param map
     * @param <T>
     * @param <V>
     * @return
     */
    private static <T, V> Map<T, Map<T, V>> twoKeyReflect(T key1, T key2, V value,
                                                          Map<T, Map<T, V>> map) {
        if (key1 == null || key2 == null || value == null) {
            return map;
        }

        if (map == null) {
            map = new HashMap<>();
        }

        if (!map.containsKey(key1)) {
            Map<T, V> reflectMap = new HashMap<>();
            map.put(key1, reflectMap);
        }

        Map<T, V> reflection = map.get(key1);
        if (!reflection.containsKey(key2)) {
            // pipeline service action src and dst always be -1.
            reflection.put(key2, value);
        }

        return map;
    }

    private static String unionKey(int parentTemplateId, int childTemplateId) {
        return parentTemplateId + UNDERSCORE + childTemplateId;
    }

    private List<ActionHandler> getInitializer(int templateId, int destination) {

        TemplateCache template = getTemplateById(templateId);
        Map<Integer, List<ActionHandler>> inits = template.getInitializers();

        if (!inits.containsKey(destination)) {
            return new ArrayList<>();
        }

        return inits.get(destination);
    }

    private List<ActionHandler> getTriggers(int templateId, int triggerType) {

        String type = TemplateTriggerType.getNameByCode(triggerType);

        if (type == null) {
            return new ArrayList<>();
        }

        TemplateCache template = getTemplateById(templateId);
        Map<String, List<ActionHandler>> triggers = template.getTriggers();

        if (CollectionUtils.isEmpty(triggers) || !triggers.containsKey(type)) {
            // empty or no such triggers
            return new ArrayList<>();
        }

        return triggers.get(type);
    }

}