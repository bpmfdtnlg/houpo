package com.youruan.dentistry.core.user.domain;

import com.youruan.dentistry.core.base.domain.BasicDomain;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户职业信息实体类
 */
@Getter
@Setter
public class UserProfessionInfo extends BasicDomain {

    /**
     * 毕业院校
     */
    private String graduatedCollege;
    /**
     * 所学专业
     */
    private String major;
    /**
     * 学历水平
     */
    private String education;
    /**
     * 期望从事职业
     */
    private String expectedOccupation;
    /**
     * 期望就业地址
     */
    private String expectedAddress;

}
