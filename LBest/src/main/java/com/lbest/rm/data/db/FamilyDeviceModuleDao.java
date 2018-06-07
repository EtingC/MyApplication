package com.lbest.rm.data.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by dell on 2017/11/28.
 */

public class FamilyDeviceModuleDao extends BaseDaoImpl<FamilyDeviceModuleData, Integer> {
    public FamilyDeviceModuleDao(DatabaseHelper helper) throws SQLException {
        super(helper.getConnectionSource(), FamilyDeviceModuleData.class);
    }

    public FamilyDeviceModuleDao(ConnectionSource connectionSource,
                            Class<FamilyDeviceModuleData> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

}
