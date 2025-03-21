package com.xin.xinpicturebackend;

import com.xin.xinpicturebackend.manager.CosManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;

@SpringBootTest
@ActiveProfiles("prod")
class XinPictureBackendApplicationTests {
    @Resource
    CosManager cosManager;
    @Test
    void contextLoads() {

    }

}
