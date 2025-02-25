package cartoland.commands;

import cartoland.Cartoland;
import cartoland.utilities.CommonFunctions;
import cartoland.utilities.IDs;
import cartoland.utilities.IntroduceHandle;
import cartoland.utilities.JsonHandle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@code IntroduceCommand} is an execution when a user uses /introduce command. This class implements
 * {@link ICommand} interface, which is for the commands HashMap in {@link cartoland.events.CommandUsage}. This
 * class doesn't handle sub commands, but call other classes to deal with it.
 *
 * @since 2.0
 * @author Alex Cai
 */
public class IntroduceCommand implements ICommand
{
	private final Map<String, ICommand> subCommands = new HashMap<>(3);

	public IntroduceCommand()
	{
		subCommands.put("user", event ->
		{
			User user = event.getUser();
			User target = event.getOption("user", CommonFunctions.getAsUser);
			if (target == null) //沒有填 預設是自己
				target = user;

			String content = IntroduceHandle.getIntroduction(target.getIdLong());
			event.reply(content != null ? content : JsonHandle.getStringFromJsonKey(user.getIdLong(), "introduce.user.no_info")).queue();
		});
		subCommands.put("update", new UpdateSubCommand());
		subCommands.put("delete", event ->
		{
			long userID = event.getUser().getIdLong();
			event.reply(JsonHandle.getStringFromJsonKey(userID, "introduce.update.delete")).queue();
			IntroduceHandle.deleteIntroduction(userID); //刪除自我介紹
		});
	}

	/**
	 * The execution of a slash command. Unlike other commands that has sub commands, since this
	 * command only has 2 sub commands, it uses a single ternary operation instead of HashMap to call the
	 * class that handles the sub command.
	 *
	 * @param event The event that carries information of the user and the command.
	 * @since 2.0
	 * @author Alex Cai
	 */
	@Override
	public void commandProcess(SlashCommandInteractionEvent event)
	{
		subCommands.get(event.getSubcommandName()).commandProcess(event);
	}

	/**
	 * {@code UpdateSubCommand} is a class that handles one of the sub commands of {@code /introduce} command, which is
	 * {@code /introduce update}.
	 *
	 * @since 2.0
	 * @author Alex Cai
	 */
	private static class UpdateSubCommand implements ICommand
	{
		private final Pattern linkRegex = Pattern.compile("https://discord\\.com/channels/" + IDs.CARTOLAND_SERVER_ID + "/\\d+/\\d+");
		private static final int SUB_STRING_START = ("https://discord.com/channels/" + IDs.CARTOLAND_SERVER_ID + "/").length();

		@Override
		public void commandProcess(SlashCommandInteractionEvent event)
		{
			long userID = event.getUser().getIdLong();
			String content = event.getOption("content", CommonFunctions.getAsString);
			if (content == null)
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "introduce.update.delete")).queue();
				IntroduceHandle.deleteIntroduction(userID); //刪除自我介紹
				return;
			}

			if (!linkRegex.matcher(content).matches()) //如果內容不是創聯群組連結
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "introduce.update.update")).queue();
				IntroduceHandle.updateIntroduction(userID, content);
				return;
			}

			//以下就是處理創聯群組連結的部分
			String[] numbersInLink = content.substring(SUB_STRING_START).split("/");

			//從創聯中取得頻道
			Guild cartoland = event.getGuild(); //先假設指令在創聯中執行 這樣可以省去一次getGuildById
			if (cartoland == null || cartoland.getIdLong() != IDs.CARTOLAND_SERVER_ID) //結果不是在創聯
				cartoland = Cartoland.getJDA().getGuildById(IDs.CARTOLAND_SERVER_ID); //定位創聯
			if (cartoland == null) //找不到創聯
			{
				event.reply("Can't get Cartoland server").queue();
				return; //結束
			}

			MessageChannel linkChannel = cartoland.getChannelById(MessageChannel.class, Long.parseLong(numbersInLink[0]));
			if (linkChannel == null) //找不到訊息內的頻道
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "introduce.update.no_channel")).queue();
				return;
			}

			//從頻道中取得訊息 注意ID是String 與慣例的long不同
			linkChannel.retrieveMessageById(numbersInLink[1]).queue(linkMessage ->
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "introduce.update.update")).queue(); //越早回覆越好 以免超過三秒
				String rawMessage = linkMessage.getContentRaw(); //訊息內容
				List<Message.Attachment> attachments = linkMessage.getAttachments(); //副件
				if (!attachments.isEmpty())
					rawMessage += attachments.stream().map(CommonFunctions.getUrl).collect(Collectors.joining("\n", "\n", ""));
				IntroduceHandle.updateIntroduction(linkMessage.getAuthor().getIdLong(), rawMessage); //更新介紹
			}, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e ->
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "introduce.update.no_message")).queue();
				IntroduceHandle.updateIntroduction(userID, content); //更新介紹 直接把連結放進內容中
			}));

		}
	}
}