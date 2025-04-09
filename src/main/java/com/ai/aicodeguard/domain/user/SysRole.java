package com.ai.aicodeguard.domain.user;

import lombok.Data;

import jakarta.persistence.*;

/**
 * 角色
 */
@Data
@Entity
@Table(name = "sys_role")
public class SysRole {

    /**
     * 主键id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 角色名
     */
    private String name;

    /**
     * 角色等级
     */
    private Integer grade;

    /**
     * 备注
     */
    private String remark;
}