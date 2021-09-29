package cn.taskeren.code.dbmu;

import com.google.common.base.Charsets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {

	public static void writeToTemp(String text, String file) {
		try {
			File f = new File(file);
			if(!f.exists()) {
				if(!f.getParentFile().exists()) {
					if(!f.getParentFile().mkdirs()) {
						throw new IOException("无法建立文件夹："+f.getParentFile().getAbsolutePath());
					}
				}
				if(!f.createNewFile()) {
					throw new IOException("无法建立文件："+f.getAbsolutePath());
				}
			}

			try(FileWriter fw = new FileWriter(file, Charsets.UTF_8)) {
				fw.write(text);
			}
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

}
