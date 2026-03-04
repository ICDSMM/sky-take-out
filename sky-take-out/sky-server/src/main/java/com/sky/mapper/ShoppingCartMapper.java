package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 判断当前加入到购物车中的商品是否已经存在
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 存在，数量 + 1
     * @param shoppingCart
     */
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 当前加入到购物车中的商品不存在，插入一条购物车数据
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据id删除购物车中一个商品
     * @param id
     */
    void deleteById(Long id);

    /**
     * 根据用户id删除购物车中所有商品
     * @param userId
     */
    void deleteByUserId(Long userId);
}
