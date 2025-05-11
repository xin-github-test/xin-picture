package com.xin.xinpicturebackend;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.xin.xinpicturebackend.CrawStrategy.StrategyContext;
import com.xin.xinpicturebackend.manager.CosManager;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("prod")
class XinPictureBackendApplicationTests {
    @Resource
    CosManager cosManager;
    @Resource
    StrategyContext strategyContext;
    @Test
    void contextLoads() {

    }

    @Test
    void testTranslate() {
        String url = "https://dict.youdao.com/suggest?num=5&ver=3.0&doctype=json&cache=false&le=en&q=";
        String word = "动漫";
        String res = HttpUtil.get(url + word);
        Map bean = JSONUtil.toBean(res, Map.class);
        //bean.get("data").get("entries").get(0).get("explain")
        Map data = (Map)bean.get("data");
        List<Map> entries = JSONUtil.toList((JSONArray) data.get("entries"), Map.class);
        String explain = ((String) entries.get(0).get("explain")).trim().split(";")[0];
        System.out.println(explain);

    }
    @Test
    void testCrawingByFreeJpg() {
        List<String> list = strategyContext.crawing("壁纸", 5);
        System.out.println(list);
    }


    class Parent {
        private MyDao myDao;

        public Parent() {
            this.myDao = new MyDao(); // 初始化 myDao
        }

        public final void callP() {
            System.out.println("父类final方法");
        }

        public MyDao getMyDao() {
            return myDao;  // 用于测试代理时的 myDao
        }
    }

    class MyDao {

    }
    class child extends Parent {
        private MyDao myDao;
    }

    @Test
    void testFinalMethod() {
        // 创建目标对象
        Parent target = new Parent();

        // 创建 CGLIB 代理对象
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true); // 使用 CGLIB 代理

        // 获取代理对象
        Parent proxy = (Parent) proxyFactory.getProxy();

        // 获取原始目标对象
        Class<?> aClass = AopProxyUtils.ultimateTargetClass(proxy);

    }

}
