/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.batch.BatchInitParam;
import com.bilibili.universal.process.model.batch.BatchInitResult;
import com.bilibili.universal.process.model.batch.InitParam;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.cache.TemplateMetadata;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.util.TplUtil;
import com.bilibili.universal.util.common.AssertUtil;

/**
 * @author Tony Zhao
 * @version $Id: StatusMachine2ndServiceImpl.java, v 0.1 2022-02-09 5:50 PM Tony Zhao Exp $$
 */
@Service
public class StatusMachine2ndServiceImpl extends AbstractStatusMachineStrategyService {

    private static final Logger logger = LoggerFactory.getLogger(StatusMachine2ndServiceImpl.class);

    @Override
    public long initProcess(int templateId, long refUniqueNo, DataContext dataContext) {
        return initProcess(templateId, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long initProcess(int templateId, long refUniqueNo, DataContext dataContext,
                            Consumer<ProcessContext> callback) {
        int dst = getDst(templateId);
        return initProcess(templateId, dst, refUniqueNo, dataContext, callback);
    }

    @Override
    public long initProcess(int templateId, long refUniqueNo, long parentRefUniqueNo,
                            DataContext dataContext) {
        return initProcess(templateId, refUniqueNo, parentRefUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long initProcess(int templateId, long refUniqueNo, long parentRefUniqueNo,
                            DataContext dataContext, Consumer<ProcessContext> callback) {
        int dst = getDst(templateId);
        return initProcess(templateId, dst, refUniqueNo, parentRefUniqueNo, dataContext, callback);
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            DataContext dataContext) {
        return initProcess(templateId, destination, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            DataContext dataContext, Consumer<ProcessContext> callback) {
        return initProcess(templateId, destination, refUniqueNo, -1, dataContext, callback);
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            long parentRefUniqueNo, DataContext dataContext) {
        return initProcess(templateId, destination, refUniqueNo, parentRefUniqueNo, dataContext,
            resp -> {
            });
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            long parentRefUniqueNo, DataContext dataContext,
                            Consumer<ProcessContext> callback) {
        AssertUtil.largeThanValue(refUniqueNo, 0);
        final int source = -1;

        TemplateCache template = getCache(templateId);

        Map<Integer, List<ActionHandler>> inits = template.getInitializers();
        if (CollectionUtils.isEmpty(inits) || !inits.containsKey(destination)) {
            return 0;
        }

        List<ActionHandler> handlers = inits.get(destination);
        if (CollectionUtils.isEmpty(handlers)) {
            return 0;
        }

        // 4th: prepare proceed context
        ProcessContext context = buildContext(templateId, refUniqueNo, source, destination,
            dataContext);

        // fast query once to check if it's a new process
        notExistRefProcess(refUniqueNo);

        final long pno = nextId();

        Long processId = tx(status -> {
            execute(context, handlers);

            // secondly create new process
            long newProcessId = createProcess(templateId, -1, pno, refUniqueNo, parentRefUniqueNo,
                source, destination, dataContext.getOperator(), dataContext.getRemark());

            // Warning: give callback a chance to execute outta business codes, callback must after execute!
            if (callback != null) {
                callback.accept(context);
            }

            return newProcessId;
        });

        AssertUtil.largeThanValue(processId, 0);

        return pno;
    }

    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext) {
        return proceedProcess(actionId, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                         Consumer<ProcessContext> callback) {
        // default proceed behavior is:
        // a) delegate proceed parent if is the slowest child and exists parent process...
        // b) delegate proceed children if proceed parent process...
        return proceedProcess(actionId, refUniqueNo, dataContext, callback, true, true);
    }

    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                         boolean proceedParent, boolean proceedChildren) {
        return proceedProcess(actionId, refUniqueNo, dataContext, resp -> {
        }, proceedParent, proceedChildren);
    }

    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                         Consumer<ProcessContext> callback, boolean proceedParent,
                                         boolean proceedChildren) {
        // fast query once to check if it's a new process
        existRefProcess(refUniqueNo);

        TemplateCache template = getTpl(actionId);

        TemplateMetadata metadata = template.getMetadata();
        int templateId = metadata.getId();

        Map<Integer, ActionCache> actionMap = template.getActions();
        ActionCache cache = actionMap.get(actionId);
        final int src = cache.getSource();
        final int dst = cache.getDestination();

        // prepare proceed context
        final ProcessContext context = buildContext(templateId, actionId, refUniqueNo, src, dst,
            dataContext);

        // 6th: check current process info if process exists
        Integer r = tx(status -> {

            // need to lock current process
            UniversalProcess process = lockRefProcess(refUniqueNo);

            int current = process.getCurrentStatus();
            long pRefNo = process.getParentRefUniqueNo();
            long no = process.getProcessNo();
            context.setProcessNo(no);

            // use status flow reflection to validate state flow correct
            checkSourceStatus(current, src);

            // check no blocking in way..
            checkBlocking(no);

            // real execute action's handlers
            execute(context, handlers(actionId, true));

            // service which operates the combination of status
            proceedProcessStatus(templateId, actionId, no, src, dst, dataContext.getOperator(),
                dataContext.getRemark());

            if (pRefNo > 0 && proceedParent) {
                // child proceed parent

                UniversalProcess pProcess = queryRefProcess(pRefNo);
                int pid = pProcess.getTemplateId();
                int pSrc = pProcess.getCurrentStatus();

                int slowest = slowestChildrenStatus(childrenRef(pRefNo));
                int pDst = statusC2P(pid, templateId, slowest);

                if (behind(pid, pSrc, pDst)) {
                    // need proceed scenario

                    int pActionId = chooseParentAction(pid, pSrc, pDst);
                    if (pActionId > 0) {
                        // special warning: cascade proceed parent, never proceed any children!
                        DataContext d = new DataContext(chooseParentParam(cache, context, pid));
                        ProcessContext pc = proceedProcess(pActionId, pRefNo, d, true, false);
                        context.parent(pc);
                    }
                }
            }

            if (isParentTpl(templateId) && proceedChildren) {
                // parent proceed child

                List<UniversalProcess> children = childrenRef(refUniqueNo);
                children.stream().filter(c -> {
                    int cid = c.getTemplateId();
                    int cs = c.getCurrentStatus();

                    // search parent 2 child mapping
                    if (inParentRefStatus(templateId, src, cid, cs)) {
                        return true;
                    }
                    return false;
                }).forEach(c -> {
                    // search action id for proceeding
                    int cid = c.getTemplateId();
                    long cRefNo = c.getRefUniqueNo();
                    int cs = c.getCurrentStatus();

                    int cActionId = nearestChildAction(cid, cs, src, dst);
                    if (cActionId > 0) {
                        // special warning: cascade proceed appropriate children, never proceed any parent!
                        DataContext d = new DataContext(chooseChildParam(cache, context));
                        ProcessContext cc = proceedProcess(cActionId, cRefNo, d, false, true);
                        context.children(cc);
                    }
                });
            }

            // give a change for business codes to execute outta logic, 
            // pay special attention: this callback must be after recursive proceed!
            if (callback != null) {
                callback.accept(context);
            }

            return 1;

        }); // tx

        AssertUtil.largeThanValue(r, 0);

        // At last, execute async handlers outta transaction!
        runAsync(() -> execute(context, handlers(actionId, false)));

        return context;
    }

    @Override
    public BatchInitResult batchInitProcess(BatchInitParam param) {
        return batchInitProcess(param, resp -> {
        });
    }

    @Override
    public BatchInitResult batchInitProcess(BatchInitParam param,
                                            Consumer<ProcessContext> callback) {
        // add some validator here..

        List<InitParam> inits = param.getParams();
        InitParam init = inits.get(0);
        int ctId = init.getTemplateId();

        TemplateCache cache = getCache(ctId);
        int ptId = TplUtil.parentId(cache);

        DataContext pData = param.getParentDataContext();

        long pRefNo = param.getParentRefUniqueNo();
        if (pRefNo == -1) {
            pRefNo = nextId();
        }

        final long pno = pRefNo;
        final Map<Integer, Long> processNos = new HashMap<>();
        tx(status -> {

            inits.forEach(i -> {
                int id = i.getTemplateId();
                long cRefNo = i.getRefUniqueNo();
                DataContext cd = i.getDataContext();

                long no = initProcess(id, cRefNo, pno, cd);
                processNos.put(id, no);
            });

            return initProcess(ptId, pno, pData);
        });

        return new BatchInitResult(pno, processNos);
    }

    @Deprecated
    private void reconcileParent(long selfProcessNo, long parentRefUniqueNo, int needReconcile,
                                 int reconcileMode) {
        // if status reached to end, then check reconcile mode
        if (parentRefUniqueNo != -1 && needReconcile == 1) {
            // get parent process
            UniversalProcess parentProcess = lockNoProcess(parentRefUniqueNo);
            int pTemplateId = parentProcess.getTemplateId();
            long parentProcessNo = parentProcess.getProcessNo();
            int pActionId = -1;
            int pcStatus = parentProcess.getCurrentStatus();

            // check reconcile flag
            boolean needUpdateParent = true;
            if (reconcileMode == 1) {
                // cooperate mode: get other sibling process
                // loop to check each child process status with no lock and update parent status if ok
                for (UniversalProcess up : siblingsByRef(parentRefUniqueNo, selfProcessNo)) {
                    int uptId = up.getTemplateId();
                    int upStatus = up.getCurrentStatus();
                    boolean isFinal = isFinalStatus(uptId, upStatus);
                    if (!isFinal) {
                        needUpdateParent = false;
                    }
                }
            }

            // need reconcile parent
            if (needUpdateParent) {
                int pacStatus = getACStatus(pTemplateId);
                proceedProcessStatus(pTemplateId, pActionId, parentProcessNo, pcStatus, pacStatus,
                    MachineConstant.DEFAULT_OPERATOR, MachineConstant.DEFAULT_REMARK);
            }

        } // if need reconcile
    }

}