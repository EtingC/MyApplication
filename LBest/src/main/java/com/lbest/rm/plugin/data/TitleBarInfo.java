package com.lbest.rm.plugin.data;

/**
 * Created by YeJin on 2016/8/24.
 */
public class TitleBarInfo {
    /**标题名称**/
    private String title;

    /**背景颜色**/
    private String backgroundColor;

    /**字体颜色**/
    private String color;

    private boolean visibility = true;

    private boolean padding = false;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public boolean isPadding() {
        return padding;
    }

    public void setPadding(boolean padding) {
        this.padding = padding;
    }
}
