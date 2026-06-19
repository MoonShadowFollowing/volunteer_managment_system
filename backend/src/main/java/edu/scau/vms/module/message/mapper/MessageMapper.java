package edu.scau.vms.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.vms.module.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT COUNT(*) FROM messages WHERE receiver_id = #{userId} AND is_read = 0 " +
            "AND (target_scope = #{scope} OR target_scope IS NULL)")
    int countUnread(@Param("userId") Long userId, @Param("scope") String scope);

    @Update("UPDATE messages SET is_read = 1 WHERE receiver_id = #{userId} AND is_read = 0 " +
            "AND (target_scope = #{scope} OR target_scope IS NULL)")
    int markAllRead(@Param("userId") Long userId, @Param("scope") String scope);
}
