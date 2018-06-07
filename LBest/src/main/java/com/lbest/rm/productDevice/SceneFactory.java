package com.lbest.rm.productDevice;

import com.lbest.rm.data.SceneModel;
import com.lbest.rm.data.TimeScene;

/**
 * Created by dell on 2017/10/31.
 */

public class SceneFactory {
    public static final String SCENE_APPOINT_ON="1000201";
    public static final String SCENE_APPOINT_OFF="1000202";
    public static final String SCENE_APPOINT_ON_OFF="1000203";

    public static SceneModel createScene(String type){
        if(type.equals(SCENE_APPOINT_ON)){
            return new TimeScene(type);
        }else if(type.equals(SCENE_APPOINT_OFF)){
            return new TimeScene(type);
        }else if(type.equals(SCENE_APPOINT_ON_OFF)){
            return new TimeScene(type);
        }else{
            return null;
        }
    }
}
