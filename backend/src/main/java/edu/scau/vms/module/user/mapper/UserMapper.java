package edu.scau.vms.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.vms.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
