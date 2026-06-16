package edu.scau.vms.module.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.vms.module.activity.entity.Activity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
}
