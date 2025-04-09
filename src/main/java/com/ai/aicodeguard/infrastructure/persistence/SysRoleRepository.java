package com.ai.aicodeguard.infrastructure.persistence;

import com.ai.aicodeguard.domain.user.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @InterfaceName: SysRoleRepository
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/9 11:09
 */
@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Integer> {

    @Query("""
        SELECT r FROM SysRole r
        JOIN SysUserRole ur ON r.id = ur.roleId
        WHERE ur.userId = :userId
    """)
    List<SysRole> findRolesByUserId(@Param("userId") Integer userId);

}
