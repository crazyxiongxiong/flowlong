/*
 * Copyright 2023-2025 Licensed under the AGPL License
 */
package com.flowlong.bpm.engine.impl;

import com.flowlong.bpm.engine.ProcessModelParser;
import com.flowlong.bpm.engine.assist.Assert;
import com.flowlong.bpm.engine.cache.FlowCache;
import com.flowlong.bpm.engine.cache.FlowSimpleCache;
import com.flowlong.bpm.engine.core.FlowLongContext;
import com.flowlong.bpm.engine.model.ProcessModel;
import lombok.Getter;
import lombok.Setter;

/**
 * FlowLong 默认流程模型解析器
 *
 * <p>
 * 尊重知识产权，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
@Getter
@Setter
public class DefaultProcessModelParser implements ProcessModelParser {

    /**
     * 流程缓存处理类，默认 ConcurrentHashMap 实现
     * 使用其它缓存框架可在初始化时赋值该静态属性
     */
    private FlowCache flowCache;

    public FlowCache getFlowCache() {
        if (null == flowCache) {
            synchronized (DefaultProcessModelParser.class) {
                flowCache = new FlowSimpleCache();
            }
        }
        return flowCache;
    }

    @Override
    public ProcessModel parse(String content, Long processId, boolean redeploy) {
        // 缓存解析逻辑
        if (null != processId) {
            final String cacheKey = "flwProcessModel#" + processId;
            FlowCache flowCache = this.getFlowCache();
            ProcessModel processModel = flowCache.get(cacheKey);
            if (null == processModel || redeploy) {
                processModel = parseProcessModel(content);
                flowCache.put(cacheKey, processModel);
            }
            return processModel;
        }

        // 未缓存解析逻辑
        return parseProcessModel(content);
    }

    private ProcessModel parseProcessModel(String content) {
        ProcessModel processModel = FlowLongContext.fromJson(content, ProcessModel.class);
        Assert.isNull(processModel, "process model json parser error");
        processModel.buildParentNode(processModel.getNodeConfig());
        return processModel;
    }
}