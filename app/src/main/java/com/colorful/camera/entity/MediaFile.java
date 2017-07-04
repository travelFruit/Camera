package com.colorful.camera.entity;

/**
 * Created by sg on 2017/4/27.
 */

public class MediaFile {

    private String name;
    private String path;
    private long lastModify;

    public MediaFile(String name, String path, long lastModify) {
        this.name = name;
        this.path = path;
        this.lastModify = lastModify;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getLastModify() {
        return lastModify;
    }

    public void setLastModify(long lastModify) {
        this.lastModify = lastModify;
    }
}
