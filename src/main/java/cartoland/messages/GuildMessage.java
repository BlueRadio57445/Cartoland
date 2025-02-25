package cartoland.messages;

import cartoland.utilities.Algorithm;
import cartoland.utilities.CommandBlocksHandle;
import cartoland.utilities.IDs;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.util.HashSet;
import java.util.Set;

/**
 * {@code GuildMessage} is a listener that triggers when a user types anything in any channel that the bot can access.
 * This class is in an array in {@link cartoland.events.MessageEvent}.
 *
 * @since 1.0
 * @author Alex Cai
 */
public class GuildMessage implements IMessage
{
	private final Emoji learned = Emoji.fromCustom("learned", 892406442622083143L, false);
	private final Emoji wow = Emoji.fromCustom("wow", 893499112228519996L, false);
	private final Emoji worship_a = Emoji.fromCustom("worship_a", 935135593527128104L, true);
	private final Set<Long> commandBlockCategories = new HashSet<>(5);

	public GuildMessage()
	{
		commandBlockCategories.add(IDs.GENERAL_CATEGORY_ID);
		commandBlockCategories.add(IDs.TECH_TALK_CATEGORY_ID);
		commandBlockCategories.add(IDs.FORUM_CATEGORY_ID);
		commandBlockCategories.add(IDs.SHOWCASE_CATEGORY_ID);
		commandBlockCategories.add(IDs.VOICE_CATEGORY_ID);
	}

	/**
	 * The method that implements from {@link IMessage}, check if the message event need to process.
	 *
	 * @param event Information about the message and its channel and author.
	 * @return If the message need to process.
	 * @since 2.0
	 * @author Alex Cai
	 */
	@Override
	public boolean messageCondition(MessageReceivedEvent event)
	{
		return event.isFromGuild();
	}

	/**
	 * The method that implements from {@link IMessage}, triggers when receive a message from any channel that
	 * the bot has permission to read, but only response when the channel is a text channel and the user isn't
	 * a bot.
	 *
	 * @param event Information about the message and its channel and author.
	 * @throws InsufficientPermissionException When the bot doesn't have permission to react.
	 * @since 1.0
	 * @author Alex Cai
	 */
	@Override
	public void messageProcess(MessageReceivedEvent event)
	{
		Message message = event.getMessage(); //獲取訊息
		String rawMessage = message.getContentRaw(); //獲取訊息字串

		if (Algorithm.chance(20) && rawMessage.contains("learned")) //20%
			message.addReaction(learned).queue();
		if (Algorithm.chance(20) && rawMessage.contains("wow")) //20%
			message.addReaction(wow).queue();
		if (rawMessage.contains("貓們"))
		{
			message.addReaction(learned).queue();
			message.addReaction(worship_a).queue();
		}

		Category category = message.getCategory();
		if (category == null) //獲取類別失敗
			return; //不用執行
		//在一般、技術討論區或公眾區域類別 且不是在機器人專區
		if (message.getChannel().getIdLong() != IDs.BOT_CHANNEL_ID && commandBlockCategories.contains(category.getIdLong()))
			CommandBlocksHandle.getLotteryData(event.getAuthor().getIdLong())
					.addBlocks(rawMessage.length() + 1 + message.getAttachments().size() + message.getStickers().size()); //說話加等級 +1當作加上\0 附加一個檔案或貼圖算1個
	}
}