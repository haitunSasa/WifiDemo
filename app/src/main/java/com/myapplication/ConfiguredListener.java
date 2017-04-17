package com.myapplication;

import java.util.HashMap;
import java.util.List;

/**
 * Created by liyan on 2017/3/31.
 */

public interface ConfiguredListener {
    void Success(List<HashMap<String, Object>> infoList);
    void Error(int errCode,String cause);
}
