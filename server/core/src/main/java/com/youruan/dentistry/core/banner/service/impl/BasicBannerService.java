package com.youruan.dentistry.core.banner.service.impl;

import com.youruan.dentistry.core.banner.domain.Banner;
import com.youruan.dentistry.core.banner.mapper.BannerMapper;
import com.youruan.dentistry.core.banner.query.BannerQuery;
import com.youruan.dentistry.core.banner.service.BannerService;
import com.youruan.dentistry.core.banner.vo.ExtendedBanner;
import com.youruan.dentistry.core.base.exception.OptimismLockingException;
import com.youruan.dentistry.core.base.query.Pagination;
import com.youruan.dentistry.core.base.storage.DiskFileStorage;
import com.youruan.dentistry.core.base.storage.UploadFile;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BasicBannerService implements BannerService {

    private final BannerMapper bannerMapper;
    private final DiskFileStorage diskFileStorage;

    public BasicBannerService(BannerMapper bannerMapper, DiskFileStorage diskFileStorage) {
        this.bannerMapper = bannerMapper;
        this.diskFileStorage = diskFileStorage;
    }

    @Override
    public Pagination<ExtendedBanner> query(BannerQuery qo) {
        int rows = bannerMapper.count(qo);
        List<ExtendedBanner> datas = ( (rows == 0) ? new ArrayList<>() : bannerMapper.query(qo) );
        return new Pagination<>(rows,datas);
    }

    @Override
    public Banner get(Long id) {
        return bannerMapper.get(id);
    }

    @Override
    public Banner create(String name, String imageUrl, String linkUrl, Integer status) {
        this.checkAddBanner(name,imageUrl,linkUrl,status);
        Banner banner = new Banner();
        banner.setName(name);
        banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl);
        banner.setStatus(status);
        return add(banner);
    }

    /**
     * 检查添加轮播图
     */
    private void checkAddBanner(String name, String imageUrl, String linkUrl, Integer status) {
        Assert.notNull(name,"轮播图名称不能为空");
        Assert.notNull(imageUrl,"图片不能为空");
        Assert.notNull(linkUrl,"链接地址不能为空");
        Assert.notNull(status,"轮播图状态不能为空");
        BannerQuery qo = new BannerQuery();
        qo.setName(name);
        int count = bannerMapper.count(qo);
        Assert.isTrue(count == 0,"轮播图名称重复");
    }

    private Banner add(Banner banner) {
        banner.setCreatedDate(new Date());
        bannerMapper.add(banner);
        return banner;
    }

    @Override
    public void update(Banner banner, String name, String imageUrl, String linkUrl, Integer status) {
        this.checkUpdateBanner(banner,name);
        banner.setName(name);
        banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl);
        banner.setStatus(status);
        update(banner);
    }

    /**
     * 检查修改轮播图
     */
    private void checkUpdateBanner(Banner banner, String name) {
        if(name == null) {
            // 可能是修改状态的操作
            return;
        }
        Assert.notNull(banner, "必须提供轮播图");
        BannerQuery qo = new BannerQuery();
        qo.setName(name);
        int count = bannerMapper.count(qo);
        Assert.isTrue(count==0 || banner.getName().equals(name),"轮播图名称重复");
    }

    @Override
    public String upload(UploadFile uploadFile) {
        try {
            return diskFileStorage.store(uploadFile,"banner");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void update(Banner banner) {
        int affected = bannerMapper.update(banner);
        if (affected == 0) {
            throw new OptimismLockingException("version!!");
        }
        banner.setVersion((banner.getVersion()+ 1));
    }
}
