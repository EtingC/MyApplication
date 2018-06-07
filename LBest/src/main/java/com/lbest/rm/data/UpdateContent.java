package com.lbest.rm.data;

import java.io.Serializable;

/**
 * Created by dell on 2017/12/23.
 */

public class UpdateContent implements Serializable {
    private static final long serialVersionUID = -420882306833514658L;

    private String en;

    private String cn;

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getEn() {
        return en;
    }

    public void setEn(String en) {
        this.en = en;
    }
}
