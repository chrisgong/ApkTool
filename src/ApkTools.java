import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ApkTools {
	private HashMap<String, String> channels = new HashMap<String, String>();// 渠道号，渠道名
	private String apkName;
	private String keyFile;
	private String keyPasswd;

	public ApkTools(String apkName, String keyFile, String keyPasswd) {
		// TODO Auto-generated constructor stub
		this.apkName = apkName;
		this.keyFile = keyFile;
		this.keyPasswd = keyPasswd;
	}

	/**
	 * 获得渠道号
	 */
	public void getCannelFile() {
		File f = new File("channel.txt");// 读取当前文件夹下的channel.txt
		if (f.exists() && f.isFile()) {
			BufferedReader br = null;
			FileReader fr = null;
			try {
				fr = new FileReader(f);
				br = new BufferedReader(fr);
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] array = line.split(" ");// 这里是Tab分割
					if (array.length == 2) {
						channels.put(array[0].trim(), array[1].trim());// 讲渠道号和渠道名存入HashMap中
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (fr != null) {
						fr.close();
					}
					if (br != null) {
						br.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			System.out.println("==INFO 1.==获取渠道成功，一共有" + channels.size()
					+ "个渠道======");
		} else {
			System.out.println("==ERROR==channel.txt文件不存在，请添加渠道文件======");
		}
	}

	/**
	 * apktool解压apk，替换渠道值
	 * 
	 * @throws Exception
	 */
	public void replaceChannel() {
		getCannelFile();

		// 解压 /C 执行字符串指定的命令然后终断
		String cmdUnpack = "cmd.exe /C java -jar apktool.jar d -f -s "
				+ "old_apk/" + apkName + " " + apkName;
		System.out.println(cmdUnpack);
		runCmd(cmdUnpack);
		System.out.println("==INFO 2.==解压apk成功======");

		String f_mani = apkName + "/AndroidManifest.xml";
		File manifest = new File(f_mani);

		/*
		 * 遍历map，复制manifese进来，修改后打包，签名，存储在对应文件夹中
		 */
		for (Map.Entry<String, String> entry : channels.entrySet()) {
			String id = entry.getKey();
			System.out.println("==INFO 4.1. == 正在生成包: " + entry.getValue()
					+ " ======");
			BufferedReader br = null;
			FileReader fr = null;
			FileWriter fw = null;
			try {
				fr = new FileReader(manifest);
				br = new BufferedReader(fr);
				String line = null;
				StringBuffer sb = new StringBuffer();
				while ((line = br.readLine()) != null) {
					if (line.contains("UMENG_CHANNEL")) {
						int start = line.lastIndexOf("=");
						int end = line.lastIndexOf("\"");
						String old_c = line.substring(start + 2, end);
						String new_s = line.replace(old_c, id);
						System.out.println(new_s);
					}
					sb.append(line + "\n");
				}

				// 写回文件
				fw = new FileWriter(f_mani);
				fw.write(sb.toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (fr != null) {
						fr.close();
					}
					if (br != null) {
						br.close();
					}
					if (fw != null) {
						fw.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println("==INFO 4.2. == 准备打包: " + entry.getValue()
					+ " ======");

			String short_name = apkName.substring(0, apkName.lastIndexOf("."));

			// 打包 - 生成未签名的包
			String unsignApk = "new_apk/" + short_name + "_" + id + "_un.apk";
			String cmdPack = String.format(
					"cmd.exe /C java -jar apktool.jar b %s %s", apkName,
					unsignApk);
			runCmd(cmdPack);

			System.out.println("==INFO 4.3. == 开始签名: " + entry.getValue()
					+ " ======");
			// 签名
			String signApk = "new_apk/" + short_name + "_" + id + ".apk";
			String cmdKey = String
					.format("cmd.exe /C jarsigner -verbose -keystore %s -storepass %s -signedjar %s %s %s",
							keyFile, keyPasswd, signApk, unsignApk,
							keyFile.subSequence(0, keyFile.indexOf(".")));
			runCmd(cmdKey);

			System.out.println("==INFO 4.4. == 签名成功: " + entry.getValue()
					+ " ======");
			// // 删除未签名的包
			File unApk = new File(unsignApk);
			unApk.delete();
		}

		// // 删除解压文件
		deleteDir(new File(apkName));
		System.out.println("==INFO 5 == 完成 ======");
	}

	/**
	 * 递归删除目录下的所有文件及子目录下所有文件
	 * 
	 * @param dir
	 *            将要删除的文件目录
	 * @return boolean Returns "true" if all deletions were successful. If a
	 *         deletion fails, the method stops attempting to delete and returns
	 *         "false".
	 */
	private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

	/**
	 * 执行指令
	 * 
	 * @param cmd
	 */
	public void runCmd(String cmd) {
		Runtime rt = Runtime.getRuntime();
		BufferedReader br = null;
		InputStreamReader isr = null;
		try {
			Process p = rt.exec(cmd);
			// p.waitFor();
			isr = new InputStreamReader(p.getInputStream());
			br = new BufferedReader(isr);
			String msg = null;
			while ((msg = br.readLine()) != null) {
				System.out.println(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (isr != null) {
					isr.close();
				}
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
