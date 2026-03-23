package com.jou.networkrepair.module.repair.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderAuditDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCloseDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderDelayApproveDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderFeedbackDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderReassignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RepairOrderService {
    Page<RepairOrder> page(Long current, Long size, String status, String title, String orderNo, String priority,
                           String deviceType, String faultType,
                           LocalDateTime reportTimeStart, LocalDateTime reportTimeEnd,
                           String sortField, String sortOrder,
                           Long userId, String role);

    RepairOrder detail(Long id, Long userId, String role);

    void create(RepairOrderCreateDTO dto, Long userId);

    void update(Long id, RepairOrder req, Long userId, String role);

    void delete(Long id, Long userId, String role);

    void assign(Long id, RepairOrderAssignDTO dto, Long userId, String role);

    void auditByAdmin(Long id, RepairOrderAuditDTO dto, Long userId, String role);

    void reassignByAdmin(Long id, RepairOrderReassignDTO dto, Long userId, String role);

    void approveDelayByAdmin(Long id, RepairOrderDelayApproveDTO dto, Long userId, String role);

    void closeByAdmin(Long id, RepairOrderCloseDTO dto, Long userId, String role);

    void updateStatus(Long id, RepairOrderStatusDTO dto, Long userId, String role);

    void cancelByUser(Long id, String remark, Long userId);

    void feedbackByUser(Long id, RepairOrderFeedbackDTO dto, Long userId);

    List<RepairOrderFlow> flows(Long id, Long userId, String role);

    Map<String, Object> stats(Long userId, String role);
}
