package com.kefu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kefu.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
