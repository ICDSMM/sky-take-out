package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.exception.BaseException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "B端-套餐相关接口")
@RestController("adminSetmealController")
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    // admin需要实时完整的数据，查询不需要缓存，有修改的时候需要把之前user端的缓存删掉

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("查询id：{}",id);
        if(id == null || id <= 0){
            throw new BaseException("套餐不存在");
        }
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    // #setmealDTO.categoryId精确删除
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐内容：{}",setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    /**
     * 批量删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    // 传入ids集合无法精确删除，所以全部删除
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result deleteById(List<Long> ids){
        log.info("删除套餐id：{}",ids);
        setmealService.deleteById(ids);
        return Result.success();
    }

     /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    @PostMapping("/status/{status}")
    @ApiOperation("套餐起售、停售")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result setStatus(@PathVariable Integer status, @RequestParam Long id){
        log.info("起售停售：{}，套餐id：{}",status,id);
        if (!status.equals(StatusConstant.DISABLE) && !status.equals(StatusConstant.ENABLE)) {
            return Result.error("状态参数不正确");
        }
        setmealService.updateStatus(status, id);
        return Result.success();
    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页需求：{}",setmealPageQueryDTO);
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

}
