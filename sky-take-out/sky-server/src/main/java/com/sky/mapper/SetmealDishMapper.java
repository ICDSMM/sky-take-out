package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询关联的套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdByDishIds(List<Long> dishIds);

    /**
     * 根据套餐id查询
     * @param category
     * @return
     */
    List<SetmealDish> getBySetmealId(Long category);

    /**
     * 新增套餐与菜品关联
     * @param setmealDish
     */
    @AutoFill(OperationType.INSERT)
    void insert(SetmealDish setmealDish);

    /**
     * 根据套餐id删除关联
     * @param ids
     */
    void deleteBySetmealIds(List<Long> ids);
}
