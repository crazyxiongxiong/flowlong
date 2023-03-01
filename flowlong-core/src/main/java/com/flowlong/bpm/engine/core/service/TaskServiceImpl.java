/* Copyright 2023-2025 www.flowlong.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowlong.bpm.engine.core.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.flowlong.bpm.engine.Assignment;
import com.flowlong.bpm.engine.FlowLongEngine;
import com.flowlong.bpm.engine.TaskAccessStrategy;
import com.flowlong.bpm.engine.TaskService;
import com.flowlong.bpm.engine.assist.Assert;
import com.flowlong.bpm.engine.assist.DateUtils;
import com.flowlong.bpm.engine.assist.JsonUtils;
import com.flowlong.bpm.engine.assist.StringUtils;
import com.flowlong.bpm.engine.core.Execution;
import com.flowlong.bpm.engine.core.FlowLongEngineImpl;
import com.flowlong.bpm.engine.core.FlowState;
import com.flowlong.bpm.engine.core.mapper.*;
import com.flowlong.bpm.engine.entity.Process;
import com.flowlong.bpm.engine.entity.*;
import com.flowlong.bpm.engine.exception.FlowLongException;
import com.flowlong.bpm.engine.listener.TaskListener;
import com.flowlong.bpm.engine.model.CustomModel;
import com.flowlong.bpm.engine.model.NodeModel;
import com.flowlong.bpm.engine.model.ProcessModel;
import com.flowlong.bpm.engine.model.TaskModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 任务执行业务类
 *
 * @author hubin
 * @since 1.0
 */
@Service
public class TaskServiceImpl implements TaskService {
    private static final String START = "start";
    private TaskAccessStrategy taskAccessStrategy;
    private ProcessMapper processMapper;
    private TaskListener taskListener;
    private InstanceMapper instanceMapper;
    private TaskMapper taskMapper;
    private TaskActorMapper taskActorMapper;
    private HisTaskMapper hisTaskMapper;

    public TaskServiceImpl(@Autowired(required = false) TaskAccessStrategy taskAccessStrategy,
                           @Autowired(required = false) TaskListener taskListener, ProcessMapper processMapper, InstanceMapper instanceMapper,
                           TaskMapper taskMapper, TaskActorMapper taskActorMapper, HisTaskMapper hisTaskMapper) {
        this.taskAccessStrategy = taskAccessStrategy;
        this.processMapper = processMapper;
        this.taskListener = taskListener;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.taskActorMapper = taskActorMapper;
        this.hisTaskMapper = hisTaskMapper;
    }

    /**
     * 完成指定任务
     */
    @Override
    public Task complete(String taskId) {
        return complete(taskId, null, null);
    }

    /**
     * 完成指定任务
     */
    @Override
    public Task complete(String taskId, String operator) {
        return complete(taskId, operator, null);
    }

    /**
     * 完成指定任务
     * 该方法仅仅结束活动任务，并不能驱动流程继续执行
     *
     * @see FlowLongEngineImpl#executeTask(String, String, java.util.Map)
     */
    @Override
    public Task complete(String taskId, String operator, Map<String, Object> args) {
        Task task = taskMapper.selectById(taskId);
        Assert.notNull(task, "指定的任务[id=" + taskId + "]不存在");
        task.setVariable(JsonUtils.toJson(args));
        if (!isAllowed(task, operator)) {
            throw new FlowLongException("当前参与者[" + operator + "]不允许执行任务[taskId=" + taskId + "]");
        }
        HisTask history = new HisTask(task);
        history.setFinishTime(DateUtils.getTime());
        history.setTaskState(FlowState.finish);
        history.setOperator(operator);
        if (history.getActorIds() == null) {
            List<TaskActor> actors = taskActorMapper.selectList(Wrappers.<TaskActor>lambdaQuery().eq(TaskActor::getTaskId, taskId));
            String[] actorIds = new String[actors.size()];
            for (int i = 0; i < actors.size(); i++) {
                actorIds[i] = actors.get(i).getActorId();
            }
            history.setActorIds(actorIds);
        }
        hisTaskMapper.insert(history);
        taskMapper.deleteById(task);
        if (null != taskListener) {
            taskListener.notify(TaskListener.EVENT_COMPLETE, history);
        }
        return task;
    }

    /**
     * 更新任务对象的finish_Time、operator、expire_Time、version、variable
     *
     * @param task 任务对象
     */
    @Override
    public void updateTask(Task task) {
        taskMapper.updateById(task);
    }

    /**
     * 任务历史记录方法
     *
     * @param execution 执行对象
     * @param model     自定义节点模型
     * @return 历史任务对象
     */
    @Override
    public HisTask history(Execution execution, CustomModel model) {
        HisTask hisTask = new HisTask();
        hisTask.setId(StringUtils.getPrimaryKey());
        hisTask.setInstanceId(execution.getInstance().getId());
        String currentTime = DateUtils.getTime();
        hisTask.setCreateTime(currentTime);
        hisTask.setFinishTime(currentTime);
        hisTask.setDisplayName(model.getDisplayName());
        hisTask.setTaskName(model.getName());
        hisTask.setTaskState(FlowState.finish);
        hisTask.setTaskType(TaskModel.TaskType.Record.ordinal());
        hisTask.setParentTaskId(execution.getTask() == null ? START : execution.getTask().getId());
        hisTask.setVariable(JsonUtils.toJson(execution.getArgs()));
        hisTaskMapper.insert(hisTask);
        return hisTask;
    }

    /**
     * 提取指定任务，设置完成时间及操作人，状态不改变
     */
    @Override
    public Task take(String taskId, String operator) {
        Task task = taskMapper.selectById(taskId);
        Assert.notNull(task, "指定的任务[id=" + taskId + "]不存在");
        if (!isAllowed(task, operator)) {
            throw new FlowLongException("当前参与者[" + operator + "]不允许提取任务[taskId=" + taskId + "]");
        }
        Task newTask = new Task();
        newTask.setId(taskId);
        newTask.setOperator(operator);
        newTask.setFinishTime(DateUtils.getTime());
        taskMapper.updateById(newTask);
        return task;
    }

    /**
     * 唤醒指定的历史任务
     */
    @Override
    public Task resume(String taskId, String operator) {
        HisTask histTask = hisTaskMapper.selectById(taskId);
        Assert.notNull(histTask, "指定的历史任务[id=" + taskId + "]不存在");
        boolean isAllowed = true;
        if (StringUtils.isNotEmpty(histTask.getOperator())) {
            isAllowed = histTask.getOperator().equals(operator);
        }
        if (isAllowed) {
            Task task = histTask.undoTask();
            task.setId(StringUtils.getPrimaryKey());
            task.setCreateTime(DateUtils.getTime());
            taskMapper.insert(task);
            assignTask(task.getId(), task.getOperator());
            return task;
        } else {
            throw new FlowLongException("当前参与者[" + operator + "]不允许唤醒历史任务[taskId=" + taskId + "]");
        }
    }

    /**
     * 向指定任务添加参与者
     */
    @Override
    public void addTaskActor(String taskId, String... actors) {
        addTaskActor(taskId, null, actors);
    }

    /**
     * 向指定任务添加参与者
     * 该方法根据performType类型判断是否需要创建新的活动任务
     */
    @Override
    public void addTaskActor(String taskId, Integer performType, String... actors) {
        Task task = taskMapper.selectById(taskId);
        Assert.notNull(task, "指定的任务[id=" + taskId + "]不存在");
        if (!task.isMajor()) {
            return;
        }
        if (performType == null) {
            performType = task.getPerformType();
        }
        if (performType == null) {
            performType = 0;
        }
        switch (performType) {
            case 0:
                assignTask(task.getId(), actors);
                Map<String, Object> data = task.getVariableMap();
                String oldActor = (String) data.get(Task.KEY_ACTOR);
                data.put(Task.KEY_ACTOR, oldActor + "," + StringUtils.getStringByArray(actors));
                task.setVariable(JsonUtils.toJson(data));
                taskMapper.updateById(task);
                break;
            case 1:
                try {
                    for (String actor : actors) {
                        Task newTask = (Task) task.clone();
                        newTask.setId(StringUtils.getPrimaryKey());
                        newTask.setCreateTime(DateUtils.getTime());
                        newTask.setOperator(actor);
                        Map<String, Object> taskData = task.getVariableMap();
                        taskData.put(Task.KEY_ACTOR, actor);
                        task.setVariable(JsonUtils.toJson(taskData));
                        taskMapper.insert(newTask);
                        assignTask(newTask.getId(), actor);
                    }
                } catch (CloneNotSupportedException ex) {
                    throw new FlowLongException("任务对象不支持复制", ex.getCause());
                }
                break;
            default:
                break;
        }
    }

    /**
     * 撤回指定的任务
     */
    @Override
    public Task withdrawTask(String taskId, String operator) {
        HisTask hist = hisTaskMapper.selectById(taskId);
        Assert.notNull(hist, "指定的历史任务[id=" + taskId + "]不存在");
//        List<Task> tasks;
//        if (hist.isPerformAny()) {
//            tasks = access().getNextActiveTasks(hist.getId());
//        } else {
//            tasks = access().getNextActiveTasks(hist.getInstanceId(),
//                    hist.getTaskName(), hist.getParentTaskId());
//        }
//        if (tasks == null || tasks.isEmpty()) {
//            throw new FlowLongException("后续活动任务已完成或不存在，无法撤回.");
//        }
//        hisTaskMapper.deleteBatchIds(tasks.stream().map(t -> t.getId()).collect(Collectors.toList()));

        Task task = hist.undoTask();
        task.setId(StringUtils.getPrimaryKey());
        task.setCreateTime(DateUtils.getTime());
        taskMapper.insert(task);
        assignTask(task.getId(), task.getOperator());
        return task;
    }

    /**
     * 驳回任务
     */
    @Override
    public Task rejectTask(ProcessModel model, Task currentTask) {
        String parentTaskId = currentTask.getParentTaskId();
        if (StringUtils.isEmpty(parentTaskId) || parentTaskId.equals(START)) {
            throw new FlowLongException("上一步任务ID为空，无法驳回至上一步处理");
        }
        NodeModel current = model.getNode(currentTask.getTaskName());
        HisTask history = hisTaskMapper.selectById(parentTaskId);
        NodeModel parent = model.getNode(history.getTaskName());
        if (!NodeModel.canRejected(current, parent)) {
            throw new FlowLongException("无法驳回至上一步处理，请确认上一步骤并非fork、join、suprocess以及会签任务");
        }

        Task task = history.undoTask();
        task.setId(StringUtils.getPrimaryKey());
        task.setCreateTime(DateUtils.getTime());
        task.setOperator(history.getOperator());
        taskMapper.insert(task);
        assignTask(task.getId(), task.getOperator());
        return task;
    }

    /**
     * 对指定的任务分配参与者。参与者可以为用户、部门、角色
     *
     * @param taskId   任务id
     * @param actorIds 参与者id集合
     */
    private void assignTask(String taskId, String... actorIds) {
        if (actorIds == null || actorIds.length == 0) {
            return;
        }
        for (String actorId : actorIds) {
            //修复当actorId为null的bug
            if (StringUtils.isEmpty(actorId)) {
                continue;
            }
            TaskActor taskActor = new TaskActor();
            taskActor.setId(StringUtils.getPrimaryKey());
            taskActor.setTaskId(taskId);
            taskActor.setActorId(actorId);
            taskActorMapper.insert(taskActor);
        }
    }

    /**
     * 根据已有任务、任务类型、参与者创建新的任务
     * 适用于转派，动态协办处理
     */
    @Override
    public List<Task> createNewTask(String taskId, int taskType, String... actors) {
        Task task = taskMapper.selectById(taskId);
        Assert.notNull(task, "指定的任务[id=" + taskId + "]不存在");
        List<Task> tasks = new ArrayList<Task>();
        try {
            Task newTask = (Task) task.clone();
            newTask.setTaskType(taskType);
            newTask.setCreateTime(DateUtils.getTime());
            newTask.setParentTaskId(taskId);
            tasks.add(saveTask(newTask, actors));
        } catch (CloneNotSupportedException e) {
            throw new FlowLongException("任务对象不支持复制", e.getCause());
        }
        return tasks;
    }

    /**
     * 获取任务模型
     *
     * @param taskId 任务id
     * @return TaskModel
     */
    @Override
    public TaskModel getTaskModel(String taskId) {
        Task task = taskMapper.selectById(taskId);
        Assert.notNull(task);
        Instance instance = instanceMapper.selectById(task.getInstanceId());
        Assert.notNull(instance);
        Process process = processMapper.selectById(instance.getProcessId());
        ProcessModel model = process.getModel();
        NodeModel nodeModel = model.getNode(task.getTaskName());
        Assert.notNull(nodeModel, "任务id无法找到节点模型.");
        if (nodeModel instanceof TaskModel) {
            return (TaskModel) nodeModel;
        } else {
            throw new IllegalArgumentException("任务id找到的节点模型不匹配");
        }
    }

    /**
     * 由DBAccess实现类创建task，并根据model类型决定是否分配参与者
     *
     * @param taskModel 模型
     * @param execution 执行对象
     * @return List<Task> 任务列表
     */
    @Override
    public List<Task> createTask(TaskModel taskModel, Execution execution) {
        List<Task> tasks = new ArrayList<>();

        Map<String, Object> args = execution.getArgs();
        if (args == null) {
            args = new HashMap<>();
        }
        Date expireDate = DateUtils.processTime(args, taskModel.getExpireTime());
        Date remindDate = DateUtils.processTime(args, taskModel.getReminderTime());
        String form = (String) args.get(taskModel.getForm());
        String actionUrl = StringUtils.isEmpty(form) ? taskModel.getForm() : form;

        String[] actors = getTaskActors(taskModel, execution);
        args.put(Task.KEY_ACTOR, StringUtils.getStringByArray(actors));
        Task task = createTaskBase(taskModel, execution);
        task.setActionUrl(actionUrl);
        task.setExpireDate(expireDate);
        task.setExpireTime(DateUtils.parseTime(expireDate));
        task.setVariable(JsonUtils.toJson(args));

        if (taskModel.isPerformAny()) {
            //任务执行方式为参与者中任何一个执行即可驱动流程继续流转，该方法只产生一个task
            task = saveTask(task, actors);
            task.setRemindDate(remindDate);
            tasks.add(task);
        } else if (taskModel.isPerformAll()) {
            //任务执行方式为参与者中每个都要执行完才可驱动流程继续流转，该方法根据参与者个数产生对应的task数量
            for (String actor : actors) {
                Task singleTask;
                try {
                    singleTask = (Task) task.clone();
                } catch (CloneNotSupportedException e) {
                    singleTask = task;
                }
                singleTask = saveTask(singleTask, actor);
                singleTask.setRemindDate(remindDate);
                tasks.add(singleTask);
            }
        }
        return tasks;
    }

    /**
     * 根据模型、执行对象、任务类型构建基本的task对象
     *
     * @param model     模型
     * @param execution 执行对象
     * @return Task任务对象
     */
    private Task createTaskBase(TaskModel model, Execution execution) {
        Task task = new Task();
        task.setInstanceId(execution.getInstance().getId());
        task.setTaskName(model.getName());
        task.setDisplayName(model.getDisplayName());
        task.setCreateTime(DateUtils.getTime());
        if (model.isMajor()) {
            task.setTaskType(TaskModel.TaskType.Major.ordinal());
        } else {
            task.setTaskType(TaskModel.TaskType.Aidant.ordinal());
        }
        task.setParentTaskId(execution.getTask() == null ?
                START : execution.getTask().getId());
        task.setModel(model);
        return task;
    }

    /**
     * 由DBAccess实现类持久化task对象
     */
    private Task saveTask(Task task, String... actors) {
        task.setId(StringUtils.getPrimaryKey());
        task.setPerformType(TaskModel.PerformType.ANY.ordinal());
        taskMapper.insert(task);
        assignTask(task.getId(), actors);
        task.setActorIds(actors);
        return task;
    }

    /**
     * 根据Task模型的assignee、assignmentHandler属性以及运行时数据，确定参与者
     *
     * @param model     模型
     * @param execution 执行对象
     * @return 参与者数组
     */
    private String[] getTaskActors(TaskModel model, Execution execution) {
        Object assigneeObject = null;
        Assignment handler = model.getAssignmentHandlerObject();
        if (StringUtils.isNotEmpty(model.getAssignee())) {
            assigneeObject = execution.getArgs().get(model.getAssignee());
        } else if (handler != null) {
            if (handler instanceof Assignment) {
                assigneeObject = handler.assign(model, execution);
            } else {
                assigneeObject = handler.assign(execution);
            }
        }
        return getTaskActors(assigneeObject == null ? model.getAssignee() : assigneeObject);
    }

    /**
     * 根据taskmodel指定的assignee属性，从args中取值
     * 将取到的值处理为String[]类型。
     *
     * @param actors 参与者对象
     * @return 参与者数组
     */
    private String[] getTaskActors(Object actors) {
        if (actors == null) return null;
        String[] results;
        if (actors instanceof String) {
            //如果值为字符串类型，则使用逗号,分隔
            return ((String) actors).split(",");
        } else if (actors instanceof List) {
            //jackson会把stirng[]转成arraylist，此处增加arraylist的逻辑判断,by 红豆冰沙2014.11.21
            List<?> list = (List) actors;
            results = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                results[i] = (String) list.get(i);
            }
            return results;
        } else if (actors instanceof Long) {
            //如果为Long类型，则返回1个元素的String[]
            results = new String[1];
            results[0] = String.valueOf(actors);
            return results;
        } else if (actors instanceof Integer) {
            //如果为Integer类型，则返回1个元素的String[]
            results = new String[1];
            results[0] = String.valueOf(actors);
            return results;
        } else if (actors instanceof String[]) {
            //如果为String[]类型，则直接返回
            return (String[]) actors;
        } else {
            //其它类型，抛出不支持的类型异常
            throw new FlowLongException("任务参与者对象[" + actors + "]类型不支持."
                    + "合法参数示例:Long,Integer,new String[]{},'10000,20000',List<String>");
        }
    }

    /**
     * 判断当前操作人operator是否允许执行taskId指定的任务
     */
    @Override
    public boolean isAllowed(Task task, String operator) {
        // 如果当前操作人不为空
        if (StringUtils.isNotEmpty(operator)) {
            // 如果是admin或者auto，直接返回true
            if (FlowLongEngine.ADMIN.equalsIgnoreCase(operator)
                    || FlowLongEngine.AUTO.equalsIgnoreCase(operator)) {
                return true;
            }
            // 如果为其他，当前做错人和任务执行人对比
            if (StringUtils.isNotEmpty(task.getOperator())) {
                return operator.equals(task.getOperator());
            }
        }

        List<TaskActor> actors = taskActorMapper.selectList(Wrappers.<TaskActor>lambdaQuery().eq(TaskActor::getTaskId, task.getId()));
        if (actors == null || actors.isEmpty()) {
            return true;
        }
        return !StringUtils.isEmpty(operator) && taskAccessStrategy.isAllowed(operator, actors);
    }

}