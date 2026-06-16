package edu.scau.vms.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.vms.module.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
