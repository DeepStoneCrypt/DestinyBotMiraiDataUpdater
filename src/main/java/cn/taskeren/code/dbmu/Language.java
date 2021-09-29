package cn.taskeren.code.dbmu;

public enum Language {
	ENGLISH("en", "eng"),
	CHINESE_SIMPLIFIED("zh-chs", "chs"),
	CHINESE_TRADITIONAL("zh-cht", "cht") // 尚未被机器人使用！
	;

	public final String bungieCode; // en, zh-chs, zh-cht
	public final String collectionSuffix; // eng, chs

	Language(String bungieCode, String collectionSuffix) {
		this.bungieCode = bungieCode;
		this.collectionSuffix = collectionSuffix;
	}
}
