package com.dai.community;

import com.dai.community.filter.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/27 - 17:14
 */
@SpringBootTest
public class SensitiveTest {
    @Autowired
    SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitive() {
        //String txt = "&֏你这是装逼@$草泥马 傻逼 法克鱿 ⩐⒀丢你老母";
        // String txt = "你这是装逼@$草泥马 ";
        // String s = sensitiveFilter.filter(txt);
        //System.out.println(s);
    }
}
