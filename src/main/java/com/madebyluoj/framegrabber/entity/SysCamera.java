package com.madebyluoj.framegrabber.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * @author luoJ
 * @date 2023/12/28 9:24
 */
@TableName(value = "sys_camera")
public class SysCamera {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String rtsp;
    private String name;
    private Integer delFlag;

    public Integer getId() {
        return id;
    }

    public SysCamera setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getRtsp() {
        return rtsp;
    }

    public SysCamera setRtsp(String rtsp) {
        this.rtsp = rtsp;
        return this;
    }

    public String getName() {
        return name;
    }

    public SysCamera setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public SysCamera setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
        return this;
    }
}
