/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import static com.bilibili.universal.process.consts.MachineConstant.DEFAULT_STATUS;

import java.util.*;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
import com.bilibili.universal.process.model.status.BriefProcess;
import com.bilibili.universal.process.util.TplUtil;
import com.bilibili.universal.util.common.AssertUtil;

/**
 * @author Tony Zhao
 * @version $Id: StatusMachine2ndServiceImpl.java, v 0.1 2022-02-09 5:50 PM Tony Zhao Exp $$
 */
@Service
public class StatusMachine2ndServiceImpl extends AbstractStatusMachineSmartStrategyService {

    @SuppressWarnings("rawtypes")
    @Override
    public long initProcess(int templateId, long refUniqueNo, DataContext dataContext) {
        return initProcess(templateId, refUniqueNo, dataContext, resp -> {
        });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public long initProcess(int templateId, long refUniqueNo, DataContext dataContext,
                            Consumer<ProcessContext> callback) {
        int dst = defaultDst(templateId);
        return initProcess(templateId, dst, refUniqueNo, dataContext, callback);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public long initProcess(int templateId, long refUniqueNo, long parentRefUniqueNo,
                            DataContext dataContext) {
        return initProcess(templateId, refUniqueNo, parentRefUniqueNo, dataContext, resp -> {
        });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public long initProcess(int templateId, long refUniqueNo, long parentRefUniqueNo,
                            DataContext dataContext, Consumer<ProcessContext> callback) {
        int dst = defaultDst(templateId);
        return initProcess(templateId, dst, refUniqueNo, parentRefUniqueNo, dataContext, callback);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            DataContext dataContext) {
        return initProcess(templateId, destination, refUniqueNo, dataContext, resp -> {
        });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            DataContext dataContext, Consumer<ProcessContext> callback) {
        return initProcess(templateId, destination, refUniqueNo, -1, dataContext, callback);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            long parentRefUniqueNo, DataContext dataContext) {
        return initProcess(templateId, destination, refUniqueNo, parentRefUniqueNo, dataContext,
            resp -> {
            });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            long parentRefUniqueNo, DataContext dataContext,
                            Consumer<ProcessContext> callback) {
        AssertUtil.largeThanValue(refUniqueNo, 0);

        TemplateCache template = getCache(templateId);
        Map<Integer, List<ActionHandler>> inits = template.getInitializers();
        if (CollectionUtils.isEmpty(inits) || !inits.containsKey(destination)) {
            return 0;
        }
        // if contains key but no handler could still initialize process..
        List<ActionHandler> handlers = inits.get(destination);

        // 4th: prepare proceed context
        ProcessContext context = buildContext(templateId, refUniqueNo, DEFAULT_STATUS, destination,
            dataContext);

        // fast query once to check if it's a new process
        notExistRefProcess(refUniqueNo);

        final long pno = nextId();

        Long processId = tx(status -> {
            execute(context, handlers);

            // secondly create new process
            long newProcessId = createProcess(templateId, pno, refUniqueNo, parentRefUniqueNo,
                DEFAULT_STATUS, destination, dataContext.getOperatorType(),
                dataContext.getOperatorId(), dataContext.getOperator(), dataContext.getRemark());

            // Warning: give callback a chance to execute outta business codes, callback must after execute!
            if (callback != null) {
                callback.accept(context);
            }

            return newProcessId;
        });

        AssertUtil.largeThanValue(processId, 0);

        runAsync(context, triggers(templateId, destination));

        return pno;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext) {
        return proceedProcess(actionId, refUniqueNo, dataContext, resp -> {
        });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                         Consumer<ProcessContext> callback) {
        // default proceed behavior is:
        // a) delegate proceed parent if is the slowest child and exists parent process...
        // b) delegate proceed children if proceed parent process...
        return proceedProcess(actionId, refUniqueNo, dataContext, callback, true, true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                         boolean proceedParent, boolean proceedChildren) {
        return proceedProcess(actionId, refUniqueNo, dataContext, resp -> {
        }, proceedParent, proceedChildren);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
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

        Integer r = tx(status -> {

            // check and lock
            UniversalProcess process = lockRefProcess(refUniqueNo);

            int current = process.getCurrentStatus();
            long pRefNo = process.getParentRefUniqueNo();
            long no = process.getProcessNo();
            context.setProcessNo(no);

            // check source
            checkSourceStatus(current, src);

            // no blocking in way..
            checkBlocking(no);

            // handlers in the action
            execute(context, handlers(actionId, true));

            // rotate embed status
            proceedProcessStatus(templateId, actionId, no, src, dst, dataContext.getOperatorType(),
                dataContext.getOperatorId(), dataContext.getOperator(), dataContext.getRemark());

            if (proceedParent && pRefNo > 0) {
                // slowest child will proceed parent whose dst follows with the newest slowest child

                UniversalProcess pProcess = queryRefProcess(pRefNo);
                int pid = pProcess.getTemplateId();
                int pSrc = pProcess.getCurrentStatus();

                BriefProcess slowest = slowestChildrenStatus(refChildren(pRefNo));
                // VIP: pls use slowest template id and its current status do ref mapping!!
                int pDst = statusC2P(pid, slowest.getTid(), slowest.getStatus());

                if (behind(pid, pSrc, pDst)) {
                    // really behind should be proceeded!

                    int aid = getActionId(pid, pSrc, pDst);
                    if (aid > 0) {
                        // Special Warning: cascade proceed parent, never proceed any children in reverse!
                        DataContext d = new DataContext(chooseParentParam(cache, context, pid));
                        ProcessContext pc = proceedProcess(aid, pRefNo, d, true, false);
                        context.parent(pc);
                    }
                }
            }

            if (proceedChildren && isParentTpl(templateId)) {
                // parent could proceed appropriate children

                refChildren(refUniqueNo).stream().filter(
                    c -> behindRefStatus(templateId, dst, c.getTemplateId(), c.getCurrentStatus()))
                    .forEach(c -> {
                        // loop to the end for searching nearest action id in status graph for proceeding
                        int aid = nearestAction(templateId, dst, c.getTemplateId(),
                            c.getCurrentStatus());
                        if (aid > 0) {
                            long cRefNo = c.getRefUniqueNo();
                            DataContext d = new DataContext(chooseChildParam(cache, context));

                            // Special Warning: cascade proceed in one orientation, never reverse!
                            ProcessContext pc = proceedProcess(aid, cRefNo, d, false, true);
                            context.children(pc);
                        }
                    });
            }

            // give a chance for business codes to execute outta logic, 
            // Special Warning: this callback must be after recursive proceed!
            if (callback != null) {
                callback.accept(context);
            }

            return 1;

        }); // tx

        AssertUtil.largeThanValue(r, 0);

        // At last, execute async handlers and triggers outta transaction!
        runAsync(context, handlers(actionId, false));
        runAsync(context, triggers(actionId));

        return context;
    }

    @Override
    public BatchInitResult batchInitProcess(BatchInitParam param) {
        return batchInitProcess(param, resp -> {
        });
    }

    @SuppressWarnings("rawtypes")
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
                long no;

                if (i.getDst() > 0) {
                    no = initProcess(id, i.getDst(), cRefNo, pno, cd);
                } else {
                    no = initProcess(id, cRefNo, pno, cd);
                }

                processNos.put(id, no);
            });

            BriefProcess slowest = slowestChildrenStatus(refChildren(pno));
            // VIP: pls use slowest template id and its current status do ref mapping!!
            int dst = statusC2P(ptId, slowest.getTid(), slowest.getStatus());

            return initProcess(ptId, dst, pno, pData);
        });

        return new BatchInitResult(pno, processNos);
    }

    @Override
    public ProcessContext smartProceedNext(int source, long refUniqueNo, DataContext dataContext) {
        return smartProceedNext(source, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public ProcessContext smartProceedNext(int source, long refUniqueNo, DataContext dataContext,
                                           Consumer<ProcessContext> callback) {
        UniversalProcess process = existRefProcess(refUniqueNo);
        int current = process.getCurrentStatus();

        // add source status check..
        checkSourceStatus(current, source);

        return continuousProceed(refUniqueNo, dataContext, callback);
    }

    @Override
    public ProcessContext continuousProceed(long refUniqueNo, DataContext dataContext) {
        return continuousProceed(refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public ProcessContext continuousProceed(long refUniqueNo, DataContext dataContext,
                                            Consumer<ProcessContext> callback) {
        UniversalProcess process = existRefProcess(refUniqueNo);

        int tid = process.getTemplateId();
        long refNo = process.getRefUniqueNo();
        int src = process.getCurrentStatus();

        int dst = nextActionDst(tid, src, false);
        if (dst < 0) {
            return reject(tid, refNo, dataContext);
        }

        // only deduce self is parent scenario
        if (isParentTpl(tid)) {

            List<BriefProcess> children = new ArrayList<>();
            List<UniversalProcess> refers = refChildren(refNo);
            for (UniversalProcess c : refers) {
                int cTid = c.getTemplateId();
                int cStatus = c.getCurrentStatus();
                int cDst = cStatus;

                int aid = nearestAction(tid, dst, cTid, cStatus);
                if (aid > 0) {
                    ActionCache action = getAction(aid);
                    cDst = action.getDestination();
                }

                children.add(new BriefProcess(cTid, cDst, getStatusSequence(cTid, cDst)));
            }

            Collections.sort(children);

            int min = Integer.MAX_VALUE;
            for (BriefProcess c : children) {
                int cTid = c.getTid();

                int pStatus = statusC2P(tid, cTid, c.getStatus());
                if (pStatus < min) {
                    min = pStatus;
                }
            }

            if (behind(tid, dst, min)) {
                dst = nextActionDst(tid, min, true);
                if (dst < 0) {
                    return reject(tid, refNo, dataContext);
                }
            }

        }

        // real selected appropriate action from source to destination
        int aid = getActionId(tid, src, dst);
        if (aid <= 0) {
            // missing appropriate action id here
            return reject(tid, refNo, dataContext);
        }

        // execute after deduce
        return proceedProcess(aid, refNo, dataContext, callback);
    }
}