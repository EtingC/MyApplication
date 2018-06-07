package com.lbest.rm.view.fragment.Account;

import android.support.v4.app.Fragment;

import com.lbest.rm.AccountMainActivity;

/**
 * Created by YeJin on 2017/4/20.
 */

public class AccountBaseFragemt extends Fragment {
    private AccountMainActivity.BottomBar bottomBar;

    public AccountMainActivity.BottomBar getBottomBar() {
        return bottomBar;
    }

    public void setBottomBar(AccountMainActivity.BottomBar bottomBar) {
        this.bottomBar = bottomBar;
    }
}
