package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        // 当前集合用于存放从begin到end范围内的每日日期
        List<LocalDate> list = new ArrayList<>();

        list.add(begin);
        while (!begin.equals(end)){
            // 日期计算，计算指定日期后一天对应的日期
            begin = begin.plusDays(1);
            list.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : list){
            //查询date日期对应的营业额数据，营业额指状态为"已完成"的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // select sum(amount) from orders where order_time > beginTime and order_time < endTime and status = 5
            Map map = new HashMap<>();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);

            // 一天没出单查出来为空，改为0
            turnover = turnover == null ?0.0 : turnover;

            turnoverList.add(turnover);
        }

        String dataList = StringUtils.join(list,",");
        String turnOverList = StringUtils.join(turnoverList,",");

        // 返回封装结果
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dataList)
                .turnoverList(turnOverList)
                .build();

        return turnoverReportVO;
    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end之间每天对应的日期
         List<LocalDate> dateList = new ArrayList<>();
         dateList.add(begin);
         while(!begin.equals(end)){
             begin = begin.plusDays(1);
             dateList.add(begin);
         }


         // 每天新增用户数量    select count(id) from user where create_time < ? and creat_time >?
         List<Integer> newUserList = new ArrayList<>();
         // 每天总用户量  select count(id) from user where create_time < ?
         List<Integer> totalUserList = new ArrayList<>();

         for (LocalDate date : dateList){
             LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
             LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

             Map map = new HashMap();
             map.put("end",endTime);

             // 总用户数量
             Integer totalUser = userMapper.countByMap(map);

             map.put("begin",beginTime);
             userMapper.countByMap(map);

             // 新增用户数量
             Integer newUser = userMapper.countByMap(map);

             totalUserList.add(totalUser);
             newUserList.add(newUser);
         }

         UserReportVO userReportVO = UserReportVO.builder()
                 .dateList(StringUtils.join(dateList,",0"))
                 .newUserList(StringUtils.join(newUserList,",0"))
                 .totalUserList(StringUtils.join(totalUserList,",0"))
                 .build();

        return userReportVO;
    }
}
