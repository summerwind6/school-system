package com.school.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class ScoreStatisticsVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long courseId;
    private String courseName;
    
    // 统计数据
    private Integer totalCount;        // 总人数
    private Integer evaluatedCount;    // 已评定人数
    private Integer unevaluatedCount;  // 未评定人数
    
    private BigDecimal averageScore;   // 平均分（总评）
    private BigDecimal maxScore;       // 最高分
    private BigDecimal minScore;       // 最低分
    
    private BigDecimal averageUsualScore;  // 平均平时分
    private BigDecimal averageFinalScore;  // 平均期末分
    
    private Double excellentRate;      // 优秀率（>=90）
    private Double goodRate;           // 良好率（>=80且<90）
    private Double passRate;           // 及格率（>=60）
    private Double failRate;           // 不及格率（<60）
    
    private Integer excellentCount;    // 优秀人数
    private Integer goodCount;         // 良好人数
    private Integer passCount;         // 及格人数
    private Integer failCount;         // 不及格人数
    
    public void calculateRates() {
        if (evaluatedCount != null && evaluatedCount > 0) {
            excellentRate = round((double) excellentCount / evaluatedCount * 100);
            goodRate = round((double) goodCount / evaluatedCount * 100);
            passRate = round((double) passCount / evaluatedCount * 100);
            failRate = round((double) failCount / evaluatedCount * 100);
        }
    }
    
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
