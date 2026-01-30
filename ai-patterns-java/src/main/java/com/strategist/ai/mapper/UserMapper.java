package com.strategist.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.strategist.ai.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
