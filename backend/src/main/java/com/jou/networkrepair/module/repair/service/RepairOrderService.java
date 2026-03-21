package com.jou.networkrepair.module.repair.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderActionDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.vo.DispatchResultVO;

import java.util.List;
import java.util.Map;

public interface RepairOrderService {
    Page<RepairOrder> page(Long current, Long size, String status, String title, String orderNo, String priority);
    Page<RepairOrder> myPage(Long current, Long size, String status, String orderNo, String priority, Long userId, String role);
    RepairOrder detail(Long id, Long userId, String role);
    void create(RepairOrderCreateDTO dto, Long userId);
    void update(Long id, RepairOrder req);
    void delete(Long id);
    void assign(Long id, RepairOrderAssignDTO dto);
    void action(Long id, RepairOrderActionDTO dto, Long userId, String role);
    List<RepairOrderFlow> flows(Long id, Long userId, String role);
    void updateStatus(Long id, RepairOrderStatusDTO dto, Long userId, String role);
    Map<String, Object> stats(Long userId, String role);
    List<DispatchResultVO> autoDispatch();
    String exportCsv(String status, String priority, String orderNo);
}
