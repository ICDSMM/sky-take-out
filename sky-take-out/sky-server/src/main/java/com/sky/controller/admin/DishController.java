package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController("adminDishController")
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "B端-菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    // 删除菜品缓存数据
    private void clearCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);


    }

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        // 清除缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        clearCache(key);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页需求：{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result deleteById(@RequestParam List<Long> ids){
        log.info("要删除的菜品id：{}",ids);
        dishService.deleteBatch(ids);
        // 将所有的菜品缓存数据删掉，所有以dish_开头的key
        clearCache("dish_*");

        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result getByCategoryId(Long categoryId){
        log.info("分类id：{}",categoryId);
        // categoryId验证
        if(categoryId == null){
            return Result.error("分类ID为空");
        }
        List<Dish> dishes = dishService.getByCategoryId(categoryId);
        return Result.success(dishes);

    }

    /**
     * 菜品起售停售
     * @param status
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result setStatus(@PathVariable Integer status, Long id){
        log.info("起售停售：{}，菜品id：{}",status,id);
         if (id == null) {
            return Result.error("菜品ID不能为空");
        }
        if (!status.equals(StatusConstant.DISABLE) && !status.equals(StatusConstant.ENABLE)) {
            return Result.error("状态参数不正确");
        }
        dishService.updateStatus(status, id);
        // 将所有的菜品缓存数据删掉，所有以dish_开头的key
        clearCache("dish_*");

        return Result.success();
    }

    /**
     * 修改菜品及口味
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result updateDish(@RequestBody DishDTO dishDTO){
        log.info("修改内容: {}", dishDTO);
        dishService.updateDish(dishDTO);
        // 将所有的菜品缓存数据删掉，所有以dish_开头的key
        clearCache("dish_*");
        return Result.success();
    }

}
