package com.spark.maven.sparksamplemaven.util;

import org.springframework.stereotype.Component;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileUtil {
    private Environment env;

    @Autowired
    public FileUtil(Environment env){
        this.env = env;
    }

    public String setGCPCredentialFile(String credential, String project){
        // ex : gcp_eh_kate.json
        String file_name = "gcp_" + project + ".json";
        String file_path = env.getProperty("file.gcp.path");
        String file_full_path = file_path + file_name;
        try {
            File f = new File(file_path + file_name);
            if (f.createNewFile()){
                BufferedWriter writer = new BufferedWriter(new FileWriter(file_full_path));
                writer.write(credential);
                writer.close();
            }
        } catch (Exception e){
            file_full_path = "";
            e.printStackTrace();
        }
        return file_full_path;
    }

    public void createQueryFolder(String chart_name){
        try {
            String folder_path = env.getProperty("file.temps.path") + chart_name;
            File folder = new File(folder_path);

            if (folder.exists()){
                FileUtils.deleteDirectory(folder);
            }

            Path path = Paths.get(folder_path);
            Files.createDirectories(path);
            System.out.println("create folder");
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public void delFile(String path){
        try {
            // json array file 쓰기
            File file = new File(path);
            if (file.exists()){
                if (file.delete())
                    System.out.println("## " + path + " :  file deleted !");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
