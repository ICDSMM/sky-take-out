package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间区间内的订单数
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status){
        Map map = new HashMap<>();
        map.put("begin",beginTime);
        map.put("end",endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

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

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
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
    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end之间每天对应的日期
         List<LocalDate> dateList = new ArrayList<>();
         dateList.add(begin);
         while(!begin.equals(end)){
             begin = begin.plusDays(1);
             dateList.add(begin);
         }

         List<Integer> orderCountList = new ArrayList<>();
         List<Integer> validOrderCountList = new ArrayList<>();
         //遍历dataList集合，查询每天的有效订单数和订单总数

        for (LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);
            // 查询订单总数
            Integer orderCount = getOrderCount(beginTime,endTime,null);
            orderCountList.add(orderCount);
            // 查询有效订单数
            Integer validOrderCount = getOrderCount(beginTime,endTime,Orders.COMPLETED);
            validOrderCountList.add(validOrderCount);
        }
        // 计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        // 计算时间区间内的有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        // 订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }


        OrderReportVO orderReportVO = OrderReportVO.builder()
                 .dateList(StringUtils.join(dateList,","))
                 .orderCountList(StringUtils.join(orderCountList,","))
                 .validOrderCountList(StringUtils.join(validOrderCountList,","))
                 .totalOrderCount(totalOrderCount)
                 .validOrderCount(validOrderCount)
                 .orderCompletionRate(orderCompletionRate)
                 .build();
        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO salesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.salesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        //封装返回结果数据
        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1. 查询数据库获取营业数据 -- 查询最近三十天运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        // 2. 通过POI将数据写入exel文件
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板");
        // 基于模板文件创建一个新的excel文件
        try {
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);

            // 填充数据
            XSSFSheet sheet = excel.getSheet("Sheet1");

            // 填充数据 -- 时间
            sheet.getRow(1).getCell(1).setCellValue("时间："+ dateBegin + "至" + dateEnd);

            // 获得第4行
            XSSFRow row = sheet.getRow(3);

            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row = sheet.getRow(4);

            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            // 填充明细数据
            for (int i = 0; i < 30;i++){
                LocalDate date = dateBegin.plusDays(i);
                // 查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                // 获得某一行
                row = sheet.getRow(i + 7);
                // 第一个单元格--日期
                row.getCell(1).setCellValue(date.toString());
                // 第二个单元格--营业额
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

            }

            // 3. 通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);

            // 关闭资源
            outputStream.close();
            excel.close();
        }catch(IOException e){
            e.printStackTrace();
        }


    }
}
