public class Main {
	/**
	 * 批量打包命令
	 * 
	 * channel.txt填入渠道信息
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ApkTools tools = new ApkTools("cim120_v1.0.8_stable.apk", "cim120.keystore", "cim120");
		tools.replaceChannel();
	}
}
