package edu.scau.vms.module.certificate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.vms.module.certificate.entity.Certificate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CertificateMapper extends BaseMapper<Certificate> {
}
