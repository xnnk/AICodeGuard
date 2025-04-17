package com.ai.aicodeguard.domain.user;

import com.ai.aicodeguard.infrastructure.common.enums.ForbiddenEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;


/**
 * @ClassName: SysUser
 * @Description: 用户
 * @Author: LZX
 * @Date: 2023/12/18 14:40
 */
@Data
@Entity
@Table(name = "sys_user")
public class SysUser {

    /**
     * 主键id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 姓名
     */
    private String name;

    /**
     * 账号
     */
    private String account;

    /**
     * 密码
     */
    @JsonIgnore
    private String password;

    /**
     * 盐
     */
    @JsonIgnore
    private String salt;

    /**
     * 是否禁用 0：否；1：是
     */
    private String forbidden;


    /**
     * 是否被禁用
     */
    public boolean checkEnabled() {
        return this.forbidden.equals(ForbiddenEnum.DISABLE.getCode().toString());
    }
}