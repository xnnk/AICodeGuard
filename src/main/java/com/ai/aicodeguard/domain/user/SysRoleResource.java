package com.ai.aicodeguard.domain.user;


import lombok.Data;

import jakarta.persistence.*;

/**
 * 角色资源关系
 */
@Data
@Entity
@Table(name = "sys_role_resource")
public class SysRoleResource {

    /**
     * 主键id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 角色id
     */
    private Integer roleId;

    /**
     * 资源id
     */
    private Integer resourceId;
}
