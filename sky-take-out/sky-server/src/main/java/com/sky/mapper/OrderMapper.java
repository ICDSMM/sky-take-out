package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 插入订单数据
     * @param order
     */
    void insert(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据订单id查询订单详情
     * @param id
     * @return
     */
    Orders getById(Long id);

    /**
     * 根据订单状态查询订单数量
     * @param status
     * @return
     */
    Integer getByStatus(Integer status);

    /**
     * 处理超时订单，查询状态为待支付且下单时间早于指定时间的订单
     * @param pendingPayment
     * @param time
     * @return
     */
    List<Orders> getByStatusAndOrderTimeLT(Integer pendingPayment, LocalDateTime time);

    Integer countByMap(Map map);

    Double sumByMap(Map map);


}
