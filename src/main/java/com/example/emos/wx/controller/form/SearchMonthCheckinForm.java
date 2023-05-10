package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@ApiModel
public class SearchMonthCheckinForm {
    @NotNull
    @Range(min=2000,max = 3000)
    private Integer year;

    @NotNull
    @Range(min=1,max = 12)
    private Integer month;


    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    @Override
    public String toString() {
        return "SearchMonthCheckinForm{" +
                "year=" + year +
                ", month=" + month +
                '}';
    }
}