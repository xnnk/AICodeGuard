package com.ai.aicodeguard.infrastructure.persistence;

import com.ai.aicodeguard.domain.user.SysRoleResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @InterfaceName: SysRoleResourceRepository
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/9 11:15
 */
@Repository
public interface SysRoleResourceRepository extends JpaRepository<SysRoleResource, Integer> {
    List<SysRoleResource> findByRoleId(Integer roleId);
}
