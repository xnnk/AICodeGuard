package com.ai.aicodeguard.infrastructure.persistence;

import com.ai.aicodeguard.domain.user.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * @InterfaceName: SysUserRepository
 * @Description: 
 * @Author: LZX
 * @Date: 2025/4/8 12:00
 */
public interface SysUserRepository extends JpaRepository<SysUser, Integer> {
    Optional<SysUser> findByAccount(String account);

    @Query("""
            SELECT u
            FROM SysUser u
            LEFT JOIN SysUserRole ur
                ON u.id = ur.userId
            LEFT JOIN SysRole r
                ON ur.roleId = r.id
            WHERE u.account = ?1
    """)
    Optional<SysUser> findByAccountWithRole(String account);
}
