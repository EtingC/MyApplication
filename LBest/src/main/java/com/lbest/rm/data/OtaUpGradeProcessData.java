package com.lbest.rm.data;

/**
 * Created by dell on 2017/11/25.
 */

public class OtaUpGradeProcessData {
    /*
     0	为初始状态，表示未开始升级
     1	代表固件开始下载
     2	代表固件开始解压
     3	代表固件开始验证
     4	代表固件开始刷flash
     5	代表固系统重启
     6	代表升级成功重启成功
     -1	代表初始化失败
     -2	代表下载失败
     -3	代表解压失败
     -4	代表校验失败
     -5	代表烧写失败
     -6	代表重启失败
     -7	代表升级失败重启
     */
    private String percent;
    private String step	;	//	当前步骤
    private String stepPercent;	//	升级各阶段(0-5）的百分比均为1-100(总体进度由平台或APP合成)

    public String getPercent() {
        return percent;
    }

    public void setPercent(String percent) {
        this.percent = percent;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStepPercent() {
        return stepPercent;
    }

    public void setStepPercent(String stepPercent) {
        this.stepPercent = stepPercent;
    }
}
