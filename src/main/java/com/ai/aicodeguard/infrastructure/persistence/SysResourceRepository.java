package com.ai.aicodeguard.infrastructure.persistence;

import com.ai.aicodeguard.domain.user.SysResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @ClassName: SysResourceRepository
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/9 10:54
 */
@Repository
public interface SysResourceRepository extends JpaRepository<SysResource, Integer> {

    @Query("""
        SELECT DISTINCT r FROM SysResource r
        JOIN SysRoleResource rr ON r.id = rr.resourceId
        JOIN SysRole role ON role.id = rr.roleId
        JOIN SysUserRole ur ON ur.roleId = role.id
        WHERE ur.userId = :roleId
    """)
    List<SysResource> findByRoleId(@Param("roleId") Integer roleId);
}
