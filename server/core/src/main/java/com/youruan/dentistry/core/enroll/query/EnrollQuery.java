package com.youruan.dentistry.core.enroll.query;

import com.youruan.dentistry.core.base.query.QueryCondition;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 报名条件查询
 */
@Getter
@Setter
public class EnrollQuery extends QueryCondition {


    protected Long[] ids;
    protected Date startCreatedDate;
    protected Date endCreatedDate;
    protected String orderNo;
    protected String likeOrderNo;
    protected Integer type;
    protected Integer orderStatus;
    protected Long userId;
    protected Long activityId;

    public void setOrderById(int keyword) {
        setOrderBy("id", keyword);
    }

    public int getOrderById() {
        return getOrderByKeyword("id");
    }

    public void setOrderByCreatedDate(int keyword) {
        setOrderBy("createdDate", keyword);
    }

    public int getOrderByCreatedDate() {
        return getOrderByKeyword("createdDate");
    }



}
