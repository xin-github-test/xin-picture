package com.xin.xinpicturebackend.CrawStrategy.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.xin.xinpicturebackend.CrawStrategy.Constants.StrategyTypeConstants;
import com.xin.xinpicturebackend.CrawStrategy.Strategy;
import com.xin.xinpicturebackend.CrawStrategy.annotation.StrategyType;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过bing来抓取图片
 */
@StrategyType(StrategyTypeConstants.BING)
@Slf4j
public class CrawingByBingStrategy implements Strategy {

    //爬取的源url
    private final String URL = "https://cn.bing.com/images/async";

    @Override
    public List<String> crawing(String searchText, Integer count) {
        //存储抓取的图片的url
        List<String> pictureUrlList = new ArrayList<>();

        //1.初始化抓取url
        String fetchUrl = String.format(URL + "?q=%s&mmasync=1", searchText);

        //2.获取网页doc
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败,",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败！");
        }

        //3.解析doc内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjectUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取元素失败！");
        }

        //4.获取所有的图片元素
        assert div != null;
        Elements imgElementList = div.select("img.mimg");

        //5.遍历所有图片元素并解析出其中的url
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                //获取失败，跳过，处理下一条
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            //处理图像地址，因为地址可能存在一些歧义字符（&）
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0,questionMarkIndex);
            }
            pictureUrlList.add(fileUrl);
            uploadCount++;
            //5.控制数量
            if (uploadCount >= count) {
                break;
            }
        }

        return pictureUrlList;
    }
}
