package cn.taskeren.code.dbmu;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import static cn.taskeren.code.dbmu.Utils.writeToTemp;

/**
 * TODO: 等机器人支持多语言之后再看看要不要搞多语言的方法
 *
 * @see Language
 */
public class Updater {

	public static final Logger log = LogManager.getLogger("Updater");

	public static String mongoUrl = "mongodb://localhost:27017";

	private static MongoClient db;
	private static JsonElement manifest;

	public static void main(String[] args) {
		System.out.println("Whoosh!");

		log.info("正在配置更新环境");
		initialSetup();

		log.info("正在连接到数据库（{}）", mongoUrl);
		tryConnectMongoDB();

		log.info("正在删除数据库中的旧数据");
		dropDestinyDatabase();

		log.info("开始更新数据库");
		executeUpdate();

		log.info("完成！");

		if(System.console() != null) {
			System.console().readLine("按任意键退出 $ ");
		}
	}

	static void initialSetup() {
		System.setProperty("java.net.useSystemProxies", "true");
		log.info("启用系统代理设置：\t{}", System.getProperty("java.net.useSystemProxies"));

		log.info("数据库地址：\t{}", mongoUrl);
	}

	static void tryConnectMongoDB() {
		db = MongoClients.create(mongoUrl);
		Objects.requireNonNull(db, "db");
	}

	static MongoDatabase getDatabase() {
		return db.getDatabase("destiny2");
	}

	static void dropDestinyDatabase() {
		getDatabase().drop();
	}

	/**
	 * 先获取棒鸡的 Manifest 再根据具体需要去新建线程跑
	 * <p>
	 * 目前有 中英文的物品定义（ItemDefinition）
	 * 看机器人里面还有关于 Activity 的东西，但是没有找到使用的地方就暂时先不搞。
	 */
	static void executeUpdate() {
		try {
			updateDestinyManifest();

			for(var lang : Arrays.asList(Language.ENGLISH, Language.CHINESE_SIMPLIFIED)) {
				new Thread(() -> {
					try {
						executeUpdateItemDefinition(lang);
					} catch(Exception ex) {
						log.error("无法为 " + lang.name() + " 正常更新数据", ex);
					}
				}, "Updater_DestinyInventoryItemDefinition_" + lang.collectionSuffix).start();
			}

		} catch(Throwable t) {
			log.error("发生致命错误！", t);
			throw new RuntimeException(t);
		}
	}

	/**
	 * 一个获取物品定义（ItemDefinition）的线程的方法
	 * @param lang 语言
	 */
	static void executeUpdateItemDefinition(Language lang) throws IOException {
		var log = LogManager.getLogger("Updater_ItemDefinition_" + lang.collectionSuffix);

		var itemDefinitions = getItemDefinitions(lang);

		var size = itemDefinitions.size();
		var curr = 0;
		var definitionsList = new ArrayList<Document>(size);
		log.info("正在处理物品定义（ItemDefinition）共{}个", size);
		for(var definition : itemDefinitions.values()) {
			curr++;
			if(definition instanceof Document definitionDoc) {
				var itemHash = definitionDoc.get("hash");
				var itemName = definitionDoc.get("displayProperties", Document.class).getString("name");
				if(itemName != null && !itemName.isBlank()) {
					log.trace("({}/{}) 添加物品定义：{}({})", curr, size, itemName, itemHash);
				} else {
					log.trace("({}/{}) 添加物品定义：#空白名称物品#({})", curr, size, itemHash);
				}
				definitionsList.add(definitionDoc);
			} else {
				log.warn("({}/{}) 跳过非法定义类型：{}", curr, size, definition.getClass());
			}
		}

		log.info("正在写入数据库");
		getDatabase().getCollection("DestinyInventoryItemDefinition_" + lang.collectionSuffix)
				.insertMany(definitionsList);
	}

	/**
	 * 获取棒鸡的 Manifest 然后存在 {@code manifest} 里
	 */
	static void updateDestinyManifest() throws IOException {
		final String manifestUrl = "https://www.bungie.net/Platform/Destiny2/Manifest/";

		log.info("正在下载 Manifest（{}）", manifestUrl);

		StringBuilder sb = new StringBuilder();
		Scanner scan = new Scanner(new URL(manifestUrl).openStream(), Charsets.UTF_8);
		while(scan.hasNext()) {
			sb.append(scan.next());
		}

		var content = sb.toString();

		log.info("正在存储 Manifest 缓存文件：{}",
				new File("./debug/manifest.json").getAbsolutePath());
		writeToTemp(content, "./debug/manifest.json");

		manifest = JsonParser.parseString(content);
	}

	/**
	 * 获取某语言的所有物品定义（ItemDefinition）然后再解析为 Document 返回
	 * @param lang 语言
	 * @return Document
	 */
	static Document getItemDefinitions(Language lang) throws IOException {
		var url = getItemDefinitionUrl(lang);

		log.info("正在下载 {} 物品定义（ItemDefinition）（{}）", lang.name(), url);

		var sb = new StringBuilder();
		var scan = new Scanner(new URL(url).openStream(), Charsets.UTF_8);
		while(scan.hasNext()) {
			sb.append(scan.next());
		}

		var content = sb.toString();

		log.info("正在存储 物品定义（ItemDefinition） 缓存文件：{}",
				new File("./debug/itemDefinition_" + lang.collectionSuffix + ".json").getAbsolutePath());
		writeToTemp(content, "./debug/itemDefinition_" + lang.collectionSuffix + ".json");

		return Document.parse(content);
	}

	/**
	 * @param lang 语言
	 * @return 物品定义数据地址
	 */
	static String getItemDefinitionUrl(Language lang) {
		return "https://www.bungie.net" + manifest.getAsJsonObject().getAsJsonObject("Response")
				.getAsJsonObject("jsonWorldComponentContentPaths").getAsJsonObject(lang.bungieCode)
				.getAsJsonPrimitive("DestinyInventoryItemDefinition").getAsString();
	}

}
