package com.ai.aicodeguard.infrastructure.persistence;

import com.ai.aicodeguard.domain.user.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @InterfaceName: SysUserRoleRepository
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/9 11:16
 */
@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Integer> {
    List<SysUserRole> findByUserId(Integer userId);

    void deleteByUserId(Integer id);

}
