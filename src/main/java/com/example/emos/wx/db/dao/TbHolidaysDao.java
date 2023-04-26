package com.example.emos.wx.db.dao;

import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

//@Mapper
public interface TbHolidaysDao {

    public Integer searchTodayIsHolidays();

}