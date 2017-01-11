package com.example.lenovo.qweather.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovo.qweather.R;
import com.example.lenovo.qweather.model.City;
import com.example.lenovo.qweather.model.County;
import com.example.lenovo.qweather.model.Province;
import com.example.lenovo.qweather.model.QWeatherDB;
import com.example.lenovo.qweather.util.HttpCallbackListener;
import com.example.lenovo.qweather.util.analysis;
import com.example.lenovo.qweather.util.httpUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lenovo on 2017/1/11.
 */

public class ChooseArea extends AppCompatActivity{
    public static final int level_province=0;
    public static final int level_city=1;
    public static final int level_county=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private QWeatherDB qWeatherDB;
    private List<String> dataList=new ArrayList<String>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectedProvince;
    private City selectCity;
    private int currentLevel;

    private boolean isFromWeatherActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFromWeatherActivity=getIntent().getBooleanExtra("from_weather_activity",false);
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("city_selected",false)){
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.area);
        listView=(ListView) findViewById(R.id.list_view);
        titleText=(TextView) findViewById(R.id.title_text);
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        qWeatherDB=QWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
                if (currentLevel ==level_province){
                    selectedProvince=provinceList.get(index);
                    queryCities();
                }else if (currentLevel ==level_city){
                    selectCity=cityList.get(index);
                    queryCounties();
                }else if (currentLevel ==level_county){
                    String countyCode=countyList.get(index).getCountyCode();
                    Intent intent=new Intent(ChooseArea.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();
    }
    private void queryProvinces(){
        provinceList=qWeatherDB.loadProvinces();
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel =level_province;
        }else {
            queryFromServer(null,"province");
        }
    }
    private void queryCities(){
        cityList=qWeatherDB.loadCitys(selectedProvince.getId());
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel =level_city;
        }else {
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
    }
    private void queryCounties(){
        countyList=qWeatherDB.loadCounties(selectCity.getId());
        if (countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectCity.getCityName());
            currentLevel =level_county;
        }else {
            queryFromServer(selectCity.getCityCode(),"county");
        }
    }
    private void queryFromServer(final String code,final String type){
        String address;
        if (!TextUtils.isEmpty(code)){
            address="http://www.weather.com.cn/data/list3/city"+code+".xml";
        }else {
            address="http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        httpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result=false;
                if ("province".equals(type)){
                    result= analysis.handleProvinceResponse(qWeatherDB,response);
                }else if ("city".equals(type)){
                    result=analysis.handleCitiesResponse(qWeatherDB,response,selectedProvince.getId());
                }else if ("county".equals(type)){
                    result=analysis.handleCountiesResponse(qWeatherDB,response,selectCity.getId());

                    if (result){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeProgressDialog();
                                if ("province".equals(type)){
                                    queryProvinces();
                                }else if ("city".equals(type)){
                                    queryCities();
                                }else if ("county".equals(type)){
                                    queryCounties();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseArea.this,"啊偶，失败了...",Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }
    private void showProgressDialog(){
        if (progressDialog==null){
            progressDialog=new ProgressDialog(this);
            progressDialog.setMessage("给我点时间...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    private void closeProgressDialog(){
        if (progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (currentLevel ==level_county){
            queryCities();
        }else if (currentLevel ==level_city){
            queryProvinces();
        }else {
            if (isFromWeatherActivity){
                Intent intent=new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }
}
