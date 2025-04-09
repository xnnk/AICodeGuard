package com.ai.aicodeguard.domain.user;

import jakarta.persistence.*;
import lombok.Data;


/**
 * 资源
 */
@Data
@Entity
@Table(name = "sys_resource")
public class SysResource {

    /**
     * 主键id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 资源父id
     */
    private Integer parentId;

    /**
     * 资源名称
     */
    private String name;

    /**
     * 权限标识符
     */
    private String perms;

    /**
     * 类型：0：目录，1：菜单，2：按钮
     */
    private String type;
}
