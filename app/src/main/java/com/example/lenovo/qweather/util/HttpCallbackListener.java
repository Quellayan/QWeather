package com.example.lenovo.qweather.util;

/**
 * Created by lenovo on 2016/12/23.
 */

public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
