package com.example.emos.wx.controller;

public class Test {
    public static void main(String[] args) {
        int P000 = 200;//移动点
        int P001 = 200;//移动点
        int I000;
//        速度
        int  sap= 100;
        int I001 = 1;
//        喷涂开关
        Boolean fal = false;
        for (I000 = 0;I000 <= 10; I000++){
            if (I001 <= 6){
//               打开喷涂
                fal = true;
                int P002 = sap;
                int P003 = sap;
                int P004 = sap;
                int P005 = sap;
//              运行一次自增1
                I001++;
            }
        }
    }
}
