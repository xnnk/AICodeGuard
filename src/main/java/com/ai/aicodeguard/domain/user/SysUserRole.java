package com.ai.aicodeguard.domain.user;

import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity
@Table(name = "sys_user_role")
public class SysUserRole {

    /**
     * 主键id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    /**
     * 用户Id
     */
    private Integer userId;


    /**
     * 角色Id
     */
    private Integer roleId;
}