package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImp implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 根据分页查询订单VO列表
     * @param page
     * @return
     */
    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }


    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional  // 事务注解在事务成功提交后才会更改状态
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 处理业务异常：地址簿为空、购物车为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            // 抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if(list == null || list.size() == 0){
            // 抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 向订单表插入 1 条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orders.setAddress(addressBook.getDetail());

        orderMapper.insert(orders);
        List<OrderDetail> orderDetailList = new ArrayList<>();

        // 向订单明细表插入n条购物车数据
        for(ShoppingCart cart : list){
            OrderDetail orderDetail = new OrderDetail(); // 订单明细
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId()); // 设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        // 清空当前用户购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        // 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        /*
        // 微信支付需要用户的 openid 来标识支付主体（微信用户），通过查询用户id找到用户对象也就算获取到用户的 openid
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单，使用 JSONObject 接收微信支付API的返回结果，便于后续解析
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号，唯一标识订单
                new BigDecimal(0.01), //支付金额，单位 元，测试环境固定为0.01元（实际环境应从订单获取）
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid，微信用户的唯一标识
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        */

        log.info("跳过微信支付，支付成功");
        paySuccess(ordersPaymentDTO.getOrderNumber());

        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通过websocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap<>();
        map.put("type",1); // 1表示来单提醒，2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content","订单号："+ outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            // 每次循环获取一个 Orders 对象，赋值给 orders 变量
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        // total ：总记录数   records ：当前页的数据列表
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO showOrderDetail(Long id) {
        Orders orders = orderMapper.getById(id);
        Long orderId = orders.getId();
        // 查询订单明细
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancelOrders(Long id) {
        Orders orders = orderMapper.getById(id);
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    @Transactional
    public void repeatOrder(Long id) {

        Orders orders = orderMapper.getById(id);
        orders.setId(null);
        orders.setOrderTime(LocalDateTime.now());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setCheckoutTime(null);
        orders.setCancelTime(null);
        orderMapper.insert(orders);
        Long orderId = orders.getId();

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        List<OrderDetail> newOrderDetailList = new ArrayList<>();

        for (OrderDetail oldOrderDetail : orderDetailList){
            OrderDetail newOrderDetail = new OrderDetail();
            BeanUtils.copyProperties(oldOrderDetail, newOrderDetail);

            newOrderDetail.setOrderId(orderId);
            newOrderDetail.setId(null);

            newOrderDetailList.add(newOrderDetail);
        }
        if(!newOrderDetailList.isEmpty()){
            orderDetailMapper.insertBatch(newOrderDetailList);
        }


    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statisticsOrders() {
        Integer toBeConfirmed = orderMapper.getByStatus(Orders.TO_BE_CONFIRMED);
        Integer deliveryInProgress = orderMapper.getByStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer confirmed = orderMapper.getByStatus(Orders.CONFIRMED);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    /**
     * 商家查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);

        orderVO.setOrderDetailList(orderDetailMapper.getByOrderId(id));
        orderVO.setOrderDishes(getOrderDishesStr(orders));

        return orderVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     * @throws Exception
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception{
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        if(ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
            orders.setCancelTime(LocalDateTime.now());
            orders.setStatus(Orders.CANCELLED);
            if (ordersDB.getPayStatus().equals(Orders.PAID)) {
                orders.setPayStatus(Orders.REFUND);
            }
            orderMapper.update(orders);
        }else {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     * @throws Exception
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception{
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        if(ordersDB.getPayStatus().equals(Orders.PAID)){
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        Orders orders = new Orders();
        if(ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 客户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map map = new HashMap<>();
        map.put("Type",2);
        map.put("orderId", ordersDB.getId());
        map.put("content","订单号："+ ordersDB.getNumber());
        String json = JSON.toJSONString(map);
        // 通过websocket向客户端浏览器推送消息
        webSocketServer.sendToAllClient(json);
    }


}
