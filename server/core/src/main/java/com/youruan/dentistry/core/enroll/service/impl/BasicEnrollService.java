package com.youruan.dentistry.core.enroll.service.impl;

import com.youruan.dentistry.core.activity.domain.Activity;
import com.youruan.dentistry.core.activity.service.ActivityService;
import com.youruan.dentistry.core.base.exception.OptimismLockingException;
import com.youruan.dentistry.core.base.query.Pagination;
import com.youruan.dentistry.core.base.utils.HttpClientUtils;
import com.youruan.dentistry.core.base.utils.SnowflakeIdWorker;
import com.youruan.dentistry.core.base.wxpay.sdk.WXPayConstants;
import com.youruan.dentistry.core.base.wxpay.sdk.WXPayUtil;
import com.youruan.dentistry.core.enroll.WxPayProperties;
import com.youruan.dentistry.core.enroll.domain.Enroll;
import com.youruan.dentistry.core.enroll.mapper.EnrollMapper;
import com.youruan.dentistry.core.enroll.query.EnrollQuery;
import com.youruan.dentistry.core.enroll.service.EnrollService;
import com.youruan.dentistry.core.enroll.vo.EnrollActivityVo;
import com.youruan.dentistry.core.enroll.vo.ExtendedEnroll;
import com.youruan.dentistry.core.user.domain.RegisteredUser;
import com.youruan.dentistry.core.user.service.RegisteredUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BasicEnrollService implements EnrollService {

    private final EnrollMapper enrollMapper;
    private final ActivityService activityService;
    private final RegisteredUserService userService;
    private final WxPayProperties wxPayProperties;

    public BasicEnrollService(EnrollMapper enrollMapper, ActivityService activityService, RegisteredUserService userService, WxPayProperties wxPayProperties) {
        this.enrollMapper = enrollMapper;
        this.activityService = activityService;
        this.userService = userService;
        this.wxPayProperties = wxPayProperties;
    }

    @Override
    public Pagination<ExtendedEnroll> query(EnrollQuery qo) {
        int rows = enrollMapper.count(qo);
        List<ExtendedEnroll> datas = ( (rows == 0) ? new ArrayList<>() : enrollMapper.query(qo) );
        return new Pagination<>(rows,datas);
    }

    @Override
    public Enroll baseCreate(RegisteredUser user, Long activityId, Integer type, Integer state) {
        Assert.isTrue(userService.checkCompleteInfo(user),"??????????????????");
        Assert.isTrue(this.checkEnrollStatus(activityId),"???????????????????????????");
        Enroll enroll = new Enroll();
        // ????????????????????? ??????
        if(type==Enroll.TYPE_WORKPLACE) {
            enroll.setOrderNo(SnowflakeIdWorker.getIdWorker());
            enroll.setPrice(new BigDecimal(1));
        }
        enroll.setType(type);
        enroll.setOrderStatus(state);
        enroll.setUserId(user.getId());
        enroll.setActivityId(activityId);
        return this.add(enroll);
    }

    /**
     * ??????????????????????????????
     */
    private boolean checkEnrollStatus(Long activityId) {
        if(activityId==null) {
            // ??????????????????????????????????????????????????????
            return true;
        }
        Activity activity = activityService.get(activityId);
        return activity.getEnrollStatus()==1;
    }

    /**
     * ????????????????????????????????????????????????????????????
     * @param userId
     * @param type
     * @return
     */
    private boolean checkEnroll(Long userId, Integer type) {
        int count = enrollMapper.countByUserIdAndType(userId,type);
        return count > 0;
    }

    @Override
    public Enroll queryOne(Long userId, Integer type) {
        Assert.notNull(type,"??????????????????");
        return enrollMapper.queryOne(userId,type);
    }

    @Override
    public String placeOrder(RegisteredUser user, Enroll enroll, String ip) {
        Assert.notNull(user,"??????????????????");
        Assert.notNull(enroll,"????????????????????????");
        Assert.notNull(ip,"??????????????????ip");
        if(enroll.getPrepayId()!=null) return enroll.getPrepayId();
        try {
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("appid",wxPayProperties.getAppId());
            paramMap.put("mch_id",wxPayProperties.getMchid());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paramMap.put("sign",WXPayUtil.generateSignature(paramMap,wxPayProperties.getPrivateKey()));
            paramMap.put("body","?????????");
            String idWorker = SnowflakeIdWorker.getIdWorker();
            paramMap.put("out_trade_no", idWorker);
            paramMap.put("total_fee",enroll.getPrice().toString());
            paramMap.put("spbill_create_ip",ip);
            paramMap.put("notify_url",wxPayProperties.getNotifyUrl());
            paramMap.put("trade_type","JSAPI");
            paramMap.put("openid",user.getOpenid());
            String xml = HttpClientUtils.doPostXml(WXPayConstants.UNIFIED_ORDER_URL,
                    WXPayUtil.generateSignedXml(paramMap, wxPayProperties.getPrivateKey()));
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            System.out.println("????????????????????????:" + resultMap);
            //????????????id??????????????????
            enroll.setPrepayId(resultMap.get("prepay_id"));
            enroll.setOrderNo(idWorker);
            this.update(enroll);
            return enroll.getPrepayId();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void update(Enroll enroll) {
        int affected = enrollMapper.update(enroll);
        if (affected == 0) {
            throw new OptimismLockingException("version!!");
        }
        enroll.setVersion((enroll.getVersion()+ 1));
    }


    @Override
    public Map<String,String> payHandle(String prepayId) {
        try {
            Map<String,String> resultMap = new HashMap<>();
            resultMap.put("appId",wxPayProperties.getAppId());
            resultMap.put("timeStamp",String.valueOf(System.currentTimeMillis() / 1000));
            resultMap.put("signType","MD5");
            resultMap.put("nonceStr",WXPayUtil.generateNonceStr());
            resultMap.put("package","prepay_id="+prepayId);
            resultMap.put("paySign",WXPayUtil.generateSignature(resultMap,wxPayProperties.getPrivateKey()));
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int countByUserIdAndActivityId(Long userId, Long activityId) {
        return enrollMapper.countByUserIdAndActivityId(userId,activityId);
    }

    @Override
    public Enroll get(Long id) {
        return enrollMapper.get(id);
    }

    @Override
    public void setOrderStatus(Enroll enroll) {
        Assert.notNull(enroll,"????????????????????????");
        enrollMapper.setOrderStatus(enroll);
    }

    @Override
    public Enroll getByOrderNo(String orderNo) {
        Assert.notNull(orderNo,"?????????????????????");
        return enrollMapper.getByOrderNo(orderNo);
    }

    @Override
    public List<ExtendedEnroll> list() {
        return enrollMapper.list();
    }

    @Override
    public List<Long> getActivityIdsByUserId(Long userId) {
        Assert.notNull(userId,"??????id??????");
        return enrollMapper.getActivityIdsByUserId(userId);
    }

    @Override
    public List<EnrollActivityVo> listByUser(Long userId) {
        Assert.notNull(userId,"??????????????????id");
        return enrollMapper.listByUser(userId);
    }

    @Override
    public Enroll workplaceEnroll(RegisteredUser user) {
        Assert.isTrue(!checkEnroll(user.getId(),Enroll.TYPE_WORKPLACE),"???????????????????????????????????????????????????");
        return baseCreate(user,null,Enroll.TYPE_WORKPLACE,Enroll.ORDER_STATUS_NOT);
    }

    @Override
    public void employmentEnroll(RegisteredUser user) {
        Assert.isTrue(!checkEnroll(user.getId(),Enroll.TYPE_EMPLOYMENT),"???????????????????????????????????????????????????");
        baseCreate(user,null,Enroll.TYPE_EMPLOYMENT,Enroll.ORDER_STATUS_OK);
    }

    @Override
    @Transactional
    public Enroll activeEnroll(RegisteredUser user, Long activityId) {
        Assert.isTrue(!checkEnroll(user.getId(), activityId),"??????????????????????????????");
        activityService.updateNumberOfEntries(activityId);
        return baseCreate(user,activityId,Enroll.TYPE_GENERAL,Enroll.ORDER_STATUS_OK);
    }

    @Override
    public void deleteExpiredPrepayId(Enroll enroll,Integer expiredHours) {
        Date lastModify = enroll.getLastModifiedDate();
        if (lastModify==null) return;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(lastModify);
        calendar.add(Calendar.HOUR,expiredHours);
        Date expiredTime = calendar.getTime();
        if (new Date().compareTo(expiredTime) < 0) return;
        // prepayId ????????????????????????????????????
        int effect = enrollMapper.deleteExpiredPrepayId(enroll);
        if(effect == 0) throw new OptimismLockingException("version!!");
        enroll.setVersion(enroll.getVersion() + 1);
    }

    /**
     * ????????????
     */
    private Enroll add(Enroll enroll) {
        enroll.setCreatedDate(new Date());
        enrollMapper.add(enroll);
        return enroll;
    }

    /**
     * ???????????????????????????????????????
     */
    private boolean checkEnroll(Long userId, Long activityId) {
        int count = enrollMapper.countByUserIdAndActivityId(userId, activityId);
        return count > 0;
    }

}
