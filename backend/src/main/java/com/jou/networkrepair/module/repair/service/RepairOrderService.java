package com.jou.networkrepair.module.repair.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderActionDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderAuditDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCloseDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderDelayApproveDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderReassignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.vo.AssignmentRecommendationVO;
import com.jou.networkrepair.module.system.entity.BusinessLog;
import com.jou.networkrepair.module.repair.vo.DispatchResultVO;

import java.util.List;
import java.util.Map;

public interface RepairOrderService {
    Page<RepairOrder> page(Long current, Long size, String status, String title, String orderNo, String priority, String sortField, String sortOrder);
    Page<RepairOrder> myPage(Long current, Long size, String status, String orderNo, String priority, Long userId, String role, String sortField, String sortOrder);
    RepairOrder detail(Long id, Long userId, String role);
    void create(RepairOrderCreateDTO dto, Long userId);
    void update(Long id, RepairOrder req);
    void delete(Long id);
    void assign(Long id, RepairOrderAssignDTO dto, Long assignBy);
    void action(Long id, RepairOrderActionDTO dto, Long userId, String role);
    void audit(Long id, RepairOrderAuditDTO dto, Long userId);
    List<RepairOrderFlow> flows(Long id, Long userId, String role);
    List<BusinessLog> businessLogs(Long id, Long userId, String role);
    void reassign(Long id, RepairOrderReassignDTO dto, Long userId);
    void approveDelay(Long id, RepairOrderDelayApproveDTO dto, Long userId);
    void close(Long id, RepairOrderCloseDTO dto, Long userId);
    void updateStatus(Long id, RepairOrderStatusDTO dto, Long userId, String role);
    Map<String, Object> stats(Long userId, String role);
    List<DispatchResultVO> autoDispatch();
    List<AssignmentRecommendationVO> recommendMaintainers(Long id, Long userId, String role);
}
