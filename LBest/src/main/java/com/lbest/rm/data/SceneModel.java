package com.lbest.rm.data;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by dell on 2017/10/31.
 */

public class SceneModel implements Serializable{
    private static final long serialVersionUID = 1L;
    public final static String SCENE_ACTIVE="1";
    public final static String SCENE_UNACTIVE="0";

    private String id;
    private String creator;
    private String deviceUuid;
    private String templateId;
    private String state;
    private String name;
    private String sceneGroup;

    public SceneModel(){}

    public SceneModel(String templateId) {
        this.templateId = templateId;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSceneGroup() {
        return sceneGroup;
    }

    public void setSceneGroup(String sceneGroup) {
        this.sceneGroup = sceneGroup;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}
