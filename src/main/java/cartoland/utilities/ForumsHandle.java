package cartoland.utilities;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code ForumsHandle} is a utility class that has functions which controls map-discuss forum and question forum
 * from create to archive. Can not be instantiated or inherited.
 * @since 2.0
 * @author Alex Cai
 */
public final class ForumsHandle
{
	private ForumsHandle()
	{
		throw new AssertionError(IDs.YOU_SHALL_NOT_ACCESS);
	}

	private static final Emoji resolved = Emoji.fromCustom("resolved", 1081082902785314921L, false);
	private static final String resolvedFormat = resolved.getFormatted();
	private static final Emoji reminder_ribbon = Emoji.fromUnicode("🎗️");
	private static final int CARTOLAND_GREEN = -8009369; //new java.awt.Color(133, 201, 103, 255).getRGB();
	private static final int MAX_TAG = 5;
	private static final long LAST_MESSAGE_HOUR = 48L;
	private static final MessageEmbed startEmbed = new EmbedBuilder()
			.setTitle("**-=發問指南=-**", "https://discord.com/channels/886936474723950603/1079081061658673253/1079081061658673253")
			.setDescription("""
							-=發問指南=-
														
							• 請清楚說明你想做什麼，並想要什麼結果。
							• 請提及你正在使用的Minecraft版本，以及是否正在使用任何模組。
							• 討論完成後，使用 `:resolved:` %s 表情符號關閉貼文。
														
							-=Guidelines=-
							       
							• Ask your question straight and clearly, tell us what you are trying to do.
							• Mention which Minecraft version you are using and any mods.
							• Remember to use `:resolved:` %s to close the post after resolved.
							""".formatted(resolvedFormat, resolvedFormat))
			.setColor(CARTOLAND_GREEN) //創聯的綠色
			.build();
	private static final String remindMessage =
    		"""
			%%s，你的問題解決了嗎？如果已經解決了，記得使用`:resolved:` %s 表情符號關閉貼文。
			如果還沒解決，可以嘗試在問題中加入更多資訊。
			%%s, did your question got a solution? If it did, remember to close this post using `:resolved:` %s emoji.
			If it didn't, try offer more information of question.
			""".formatted(resolvedFormat, resolvedFormat);

	private static final String IDLED_QUESTIONS_SET_FILE_NAME = "serialize/idled_questions.ser";
	private static final String HAS_START_MESSAGE_FILE_NAME = "serialize/has_start_message.ser";
	//https://stackoverflow.com/questions/41778276/casting-from-object-to-arraylist
	private static final Set<Long> idledQuestionForumPosts = FileHandle.deserialize(IDLED_QUESTIONS_SET_FILE_NAME) instanceof HashSet<?> set ?
			set.stream().map(element -> (Long)element).collect(Collectors.toSet()) : new HashSet<>();
	private static final Set<Long> hasStartMessageForumPosts = FileHandle.deserialize(HAS_START_MESSAGE_FILE_NAME) instanceof HashSet<?> set ?
			set.stream().map(element -> (Long)element).collect(Collectors.toSet()) : new HashSet<>();

	static
	{
		FileHandle.registerSerialize(IDLED_QUESTIONS_SET_FILE_NAME, idledQuestionForumPosts);
		FileHandle.registerSerialize(HAS_START_MESSAGE_FILE_NAME, hasStartMessageForumPosts);
	}

	/**
	 * This method is being used in {@link cartoland.messages.ForumMessage} in order to check if the message event is the first message in a forum post.
	 *
	 * @param forumPost The forum post that needs check.
	 * @return true if this is the first time this forum post received a message
	 * @since 2.0
	 * @author Alex Cai
	 */
	public static boolean isFirstMessage(ThreadChannel forumPost)
	{
		return !hasStartMessageForumPosts.contains(forumPost.getIdLong());
	}

	public static void startStuff(ThreadChannel forumPost)
	{
		long parentChannelID = forumPost.getParentChannel().getIdLong(); //貼文所在的論壇頻道
		if (parentChannelID == IDs.MAP_DISCUSS_CHANNEL_ID) //是地圖專版
			forumPost.retrieveStartMessage().queue(message -> message.pin().queue()); //釘選第一則訊息
		else if (parentChannelID == IDs.QUESTIONS_CHANNEL_ID) //是問題論壇
			forumPost.sendMessageEmbeds(startEmbed).queue(); //傳送發問指南
		hasStartMessageForumPosts.add(forumPost.getIdLong());
	}

	public static void createForumPost(ThreadChannel forumPost)
	{
		if (forumPost.getLatestMessageIdLong() != 0) //有初始訊息
			startStuff(forumPost);//傳送發問指南

		ForumChannel questionsChannel = forumPost.getParentChannel().asForumChannel(); //問題論壇
		if (questionsChannel.getIdLong() != IDs.QUESTIONS_CHANNEL_ID) //不是問題論壇
			return;
		ForumTag resolvedForumTag = questionsChannel.getAvailableTagById(IDs.RESOLVED_FORUM_TAG_ID); //已解決
		ForumTag unresolvedForumTag = questionsChannel.getAvailableTagById(IDs.UNRESOLVED_FORUM_TAG_ID); //未解決

		List<ForumTag> tags = new ArrayList<>(forumPost.getAppliedTags());
		tags.remove(resolvedForumTag); //避免使用者自己加resolved
		if (tags.contains(unresolvedForumTag)) //如果使用者有加unresolved
		{
			forumPost.getManager().setAppliedTags(tags).queue(); //直接送出
			return;
		}

		if (tags.size() >= MAX_TAG) //不可以超過MAX_TAG個tag
			tags.remove(MAX_TAG - 1); //移除最後一個 空出位置給unresolved
		tags.add(unresolvedForumTag);
		forumPost.getManager().setAppliedTags(tags).queue();
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted") //IntelliJ IDEA 閉嘴
	public static boolean typedResolved(Object withReaction)
	{
		if (withReaction instanceof Message message)
			return message.getContentRaw().equals(resolvedFormat);
		else if (withReaction instanceof MessageReaction reaction)
			return reaction.getEmoji().equals(resolved);
		else
			return false;
	}

	public static void archiveForumPost(ThreadChannel forumPost, Message eventMessage)
	{
		eventMessage.addReaction(resolved).queue(); //機器人會在訊息上加:resolved:
		ForumChannel questionsChannel = forumPost.getParentChannel().asForumChannel(); //問題論壇
		ForumTag resolvedForumTag = questionsChannel.getAvailableTagById(IDs.RESOLVED_FORUM_TAG_ID); //已解決
		ForumTag unresolvedForumTag = questionsChannel.getAvailableTagById(IDs.UNRESOLVED_FORUM_TAG_ID); //未解決

		List<ForumTag> tags = new ArrayList<>(forumPost.getAppliedTags());
		tags.remove(unresolvedForumTag); //移除unresolved
		tags.add(resolvedForumTag); //新增resolved
		forumPost.getManager().setAppliedTags(tags).queue();
		idledQuestionForumPosts.remove(forumPost.getIdLong());

		//移除🎗️ 並關閉貼文
		unIdleQuestionForumPost(forumPost, true);
	}

	public static void tryIdleQuestionForumPost(ThreadChannel forumPost)
	{
		if (forumPost.isArchived() || forumPost.isLocked() || forumPost.getParentChannel().getIdLong() != IDs.QUESTIONS_CHANNEL_ID)
			return; //已經關閉 或已經鎖起來了 或不是問題論壇

		forumPost.retrieveMessageById(forumPost.getLatestMessageIdLong()).queue(lastMessage ->
		{
			User author = lastMessage.getAuthor();
			if (author.isBot() || author.isSystem()) //是機器人或系統
				return; //不用執行

			if (Duration.between(lastMessage.getTimeCreated(), OffsetDateTime.now()).toHours() < LAST_MESSAGE_HOUR) //LAST_MESSAGE_HOUR小時內有人發言
				return;

			String mentionOwner = "<@" + forumPost.getOwnerIdLong() + ">";
			forumPost.sendMessage(String.format(remindMessage, mentionOwner, mentionOwner)).queue(); //提醒開串者

			idledQuestionForumPosts.add(forumPost.getIdLong()); //記錄這個貼文正在idle

			//增加🎗️
			forumPost.retrieveStartMessage().queue(message -> message.addReaction(reminder_ribbon).queue());
		}, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e ->
		{
			String mentionOwner = "<@" + forumPost.getOwnerIdLong() + ">";
			forumPost.sendMessage(String.format(remindMessage, mentionOwner, mentionOwner)).queue();
		}));
	}

	public static void unIdleQuestionForumPost(ThreadChannel forumPost, boolean archive)
	{
		if (forumPost.isArchived() || forumPost.isLocked() || forumPost.getParentChannel().getIdLong() != IDs.QUESTIONS_CHANNEL_ID)
			return;

		forumPost.retrieveStartMessage().queue(message ->
		{
			//移除🎗️
			if (message.getReactions().stream().anyMatch(reaction -> reaction.getEmoji().equals(reminder_ribbon)))
				message.removeReaction(reminder_ribbon).queue();

			idledQuestionForumPosts.remove(forumPost.getIdLong());

			if (archive)
				forumPost.getManager().setArchived(true).queue(); //關閉貼文
		});
	}

	public static boolean questionForumPostIsIdled(ThreadChannel forumPost)
	{
		return forumPost.getParentChannel().getIdLong() == IDs.QUESTIONS_CHANNEL_ID && idledQuestionForumPosts.contains(forumPost.getIdLong());
	}
}