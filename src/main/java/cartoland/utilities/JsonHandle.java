package cartoland.utilities;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code JsonHandle} is a utility class that handles all the need of JSON. It will load every JSON files that the bot need
 * at the beginning of process, and provide every information that the outer classes need. This is the only place
 * that imports {@link JSONArray} and {@link JSONObject}. Can not be instantiated or inherited.
 *
 * @since 1.0
 * @author Alex Cai
 */
public final class JsonHandle
{
	private JsonHandle()
	{
		throw new AssertionError(IDs.YOU_SHALL_NOT_ACCESS);
	}

	private static final String USERS_FILE_NAME = "serialize/users.ser";

	@SuppressWarnings("unchecked")
	private static final Map<Long, String> users = (FileHandle.deserialize(USERS_FILE_NAME) instanceof HashMap map) ? map : new HashMap<>(); //使用者的語言設定 id為key en, tw 等等的語言字串為value
	private static final Map<String, JSONObject> languageFileMap = new HashMap<>(7); //語言字串為key 語言檔案為value
	private static final Map<String, List<String>> commandListMap = new HashMap<>(); //cmd.list等等為key 語言檔案對應的JSONArray為value
	private static final StringBuilder builder = new StringBuilder();

	private static JSONObject file; //在lastUse中獲得這個ID對應的語言檔案 並在指令中使用
	private static JSONObject englishFile; //英文檔案

	static
	{
		reloadLanguageFiles();
		FileHandle.registerSerialize(USERS_FILE_NAME, users);
	}

	private static void lastUse(long userID)
	{
		//獲取使用者設定的語言
		//找不到設定的語言就放英文進去
		file = languageFileMap.get(users.computeIfAbsent(userID, k -> Languages.ENGLISH));
	}

	public static String command(long userID, String commandName)
	{
		lastUse(userID);
		builder.setLength(0);
		builder.append(file.getString(commandName + ".begin")); //開頭
		JSONArray dotListArray = englishFile.getJSONArray(commandName + ".list");
		int dotListLength = dotListArray.length();
		if (dotListLength != 0) //建立回覆字串
		{
			for (int i = 0; ; i++)
			{
				builder.append(dotListArray.getString(i));
				if (i + 1 == dotListLength) //已經是最後一個了
					break;
				builder.append(", ");
			}
		}
		return builder.append(file.getString(commandName + ".end")).toString(); //結尾
	}

	public static String command(long userID, String commandName, String argument)
	{
		String result = getStringFromJsonKey(userID, commandName + ".name." + argument);
		if (commandName.equals("lang"))
		{
			users.put(userID, argument);
			return result;
		}

		//空字串代表獲取失敗
		return result.isEmpty() ? file.getString(commandName + ".fail") : result;
	}

	public static List<String> commandList(String commandName)
	{
		return commandListMap.get(commandName + ".list");
	}

	private static List<String> buildStringListFromJsonArray(JSONArray jsonArray)
	{
		int length = jsonArray.length();
		List<String> builtList = new ArrayList<>(length);
		for (int i = 0; i < length; i++)
			builtList.add(jsonArray.getString(i));
		return builtList;
	}

	public static void reloadLanguageFiles()
	{
		languageFileMap.put(Languages.ENGLISH, englishFile = new JSONObject(FileHandle.buildJsonStringFromFile("lang/en.json")));
		languageFileMap.put(Languages.TW_MANDARIN, new JSONObject(FileHandle.buildJsonStringFromFile("lang/tw.json")));
		languageFileMap.put(Languages.TAIWANESE, new JSONObject(FileHandle.buildJsonStringFromFile("lang/ta.json")));
		languageFileMap.put(Languages.CANTONESE, new JSONObject(FileHandle.buildJsonStringFromFile("lang/hk.json")));
		languageFileMap.put(Languages.CHINESE, new JSONObject(FileHandle.buildJsonStringFromFile("lang/cn.json")));
		languageFileMap.put(Languages.ESPANOL, new JSONObject(FileHandle.buildJsonStringFromFile("lang/es.json")));
		languageFileMap.put(Languages.JAPANESE, new JSONObject(FileHandle.buildJsonStringFromFile("lang/jp.json")));

		commandListMap.put("help.list", buildStringListFromJsonArray(englishFile.getJSONArray("help.list")));
		commandListMap.put("cmd.list", buildStringListFromJsonArray(englishFile.getJSONArray("cmd.list")));
		commandListMap.put("faq.list", buildStringListFromJsonArray(englishFile.getJSONArray("faq.list")));
		commandListMap.put("dtp.list", buildStringListFromJsonArray(englishFile.getJSONArray("dtp.list")));
	}

	public static String getStringFromJsonKey(long userID, String key)
	{
		lastUse(userID);
		String result; //要獲得的字串
		if (file.has(key)) //如果有這個key
			result = file.getString(key);
		else if (englishFile.has(key)) //預設使用英文
			result = englishFile.getString(key);
		else
			return "";

		//以&開頭的json key 代表要去那個地方找
		return (result.charAt(0) == '&') ? getStringFromJsonKey(userID, result.substring(1)) : result;
	}
}