package com.xin.xinpicturebackend.CrawStrategy.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.xin.xinpicturebackend.CrawStrategy.Constants.StrategyTypeConstants;
import com.xin.xinpicturebackend.CrawStrategy.Strategy;
import com.xin.xinpicturebackend.CrawStrategy.annotation.StrategyType;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.utils.TranslateUtil;
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
 * 使用freejpg源，不太准确
 */
@StrategyType(StrategyTypeConstants.FREEJPG)
@Slf4j
public class CrawingByFreeJpgStrategy implements Strategy {
    //爬取的源url
    private final String URL = "https://en.freejpg.com.ar/";

    @Override
    public List<String> crawing(String searchText, Integer count) {
        //拼接搜索词
        String enText = TranslateUtil.ChToEn(searchText);
        String fetchUrl = String.format(URL + "free/images/?criterio=%s", enText);
        //存储抓取的图片的url
        List<String> pictureUrlList = new ArrayList<>();

        //2.获取网页doc
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败,",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败！");
        }

        //3.解析doc内容
        Element div = document.getElementsByClass("pagefull").first().getElementsByClass("rownew33").first();
        if (ObjectUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未搜索到，请换一个搜索词！");
        }

        //4.获取所有的图片元素
        assert div != null;
        int uploadCount = 0;
        Elements divElementList = div.getElementsByClass("item");
        for (Element divElement : divElementList) {
            Element a = divElement.select("a").first();
            String href = a.attr("href");
            if (StrUtil.isBlank(href)) {
                //获取失败，跳过，处理下一条
                log.info("当前链接为空，已跳过：{}", href);
                continue;
            }
            //处理href https://en.freejpg.com.ar/free/info/100038400/cat-draw-illustration-vector-feline-animal-mammal-pets
            String id = href.split("/")[href.split("/").length - 2];
            //原图下载地址
            String downloadUrl = String.format(URL + "descargar/" + id);
            pictureUrlList.add(downloadUrl);
            uploadCount++;
            //5.控制数量
            if (uploadCount >= count) {
                break;
            }
        }
        return pictureUrlList;
    }
}
