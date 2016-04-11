package com.fzb.blog.controller;

import com.fzb.blog.model.Link;
import com.fzb.blog.model.WebSite;
import com.fzb.common.util.IOUtil;
import com.fzb.common.util.ZipUtil;
import com.fzb.common.util.http.HttpUtil;
import com.fzb.common.util.http.handle.HttpFileHandle;
import com.jfinal.kit.PathKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class TemplateController extends ManageController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateController.class);

    public void delete() {
        Link.dao.deleteById(getPara(0));
    }

    public void apply() {
        String template = getPara("template");
        new WebSite().updateByKV("template", template);
        if (getPara("resultType") != null
                && "html".equals(getPara("resultType"))) {
            setAttr("message", "变更完成");
        } else {
            getData().put("success", true);
            renderJson(getData());
        }
        Cookie cookie = new Cookie("template", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        getResponse().addCookie(cookie);
        // 更新缓存数据
        BaseController.refreshCache();
    }

    public void index() {
        queryAll();
    }

    public void loadFile() {
        String file = getRequest().getRealPath("/") + getPara("file");
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            String fileContent = IOUtil.getStringInputStream(new FileInputStream(file));
            map.put("fileContent", fileContent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        renderJson(map);
    }

    public void saveFile() {
        String file = getRequest().getRealPath("/") + getPara("file");
        IOUtil.writeBytesToFile(getPara("content").getBytes(), new File(file));
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("status", 200);
        renderJson(map);
    }

    public void queryAll() {
        String webPath = PathKit.getWebRootPath();
        File[] templatesFile = new File(webPath + "/include/templates/")
                .listFiles();
        List<Map<String, Object>> templates = new ArrayList<Map<String, Object>>();
        if (templatesFile != null) {
            for (File file : templatesFile) {
                if (!file.isFile()) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    File templateInfo = new File(file.toString() + "/template.properties");
                    if (templateInfo.exists()) {
                        Properties properties = new Properties();
                        try {
                            properties.load(new FileInputStream(templateInfo));
                            map.put("author", properties.get("author"));
                            map.put("name", properties.get("name"));
                            map.put("digest", properties.get("digest"));
                            map.put("version", properties.get("version"));
                            map.put("url", properties.get("url"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        map.put("author", "");
                        map.put("name", "");
                        map.put("digest", "");
                        map.put("version", "");
                        map.put("url", "");
                    }
                    map.put("template", file.toString().substring(webPath.length()).replace("\\", "/"));
                    templates.add(map);
                }
            }
        }

        setAttr("templates", templates);
        render("/admin/template.jsp");
    }

    @Override
    public void add() {

    }

    @Override
    public void update() {

    }

    public void download() {
        try {
            String fileName = getRequest().getParameter("templateName");
            String templatePath = fileName.substring(0, fileName.indexOf("."));
            File path = new File(PathKit.getWebRootPath() + "/include/templates/" + templatePath + "/");

            if (!path.exists()) {
                HttpFileHandle fileHandle = (HttpFileHandle) HttpUtil.sendGetRequest(getPara("host") + "/template/download?id=" + getParaToInt("id"),
                        new HttpFileHandle(PathKit.getWebRootPath() + "/include/templates/"), new HashMap<String, String>());
                String target = fileHandle.getT().getParent() + "/" + fileName;
                IOUtil.moveOrCopyFile(fileHandle.getT().toString(), target, true);
                ZipUtil.unZip(target, path.toString() + "/");
                setAttr("message", "下载模板成功");
            } else {
                setAttr("message", "模板已经存在了");
            }
        } catch (Exception e) {
            setAttr("message", "发生一些错误");
            LOGGER.error("download error ", e);
        }
        setAttr("suburl", "template.jsp");
    }

    public void preview() {
        String template = getPara("template");
        if (template != null) {
            Cookie cookie = new Cookie("template", template);
            cookie.setPath("/");
            getResponse().addCookie(cookie);
            String path = getRequest().getContextPath();
            String basePath = getRequest().getScheme() + "://" + getRequest().getHeader("host") + path + "/";
            redirect(basePath);
        } else {
            setAttr("message", "模板路径不能为空");
        }
    }

}