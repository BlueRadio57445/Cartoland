package cartoland.commands;

import cartoland.Cartoland;
import cartoland.utilities.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * {@code LotteryCommand} is an execution when a user uses /lottery command. This class implements {@link ICommand}
 * interface, which is for the commands HashMap in {@link cartoland.events.CommandUsage}. This class doesn't
 * have a backend class since all the works can be done here.
 *
 * @since 1.4
 * @author Alex Cai
 */
public class LotteryCommand implements ICommand
{
	private final Map<String, ICommand> subCommands = new HashMap<>(4);

	public LotteryCommand()
	{
		subCommands.put("get", new GetSubCommand());
		subCommands.put("bet", new BetSubCommand());
		subCommands.put("ranking", new RankingSubCommand());
		subCommands.put("daily", new DailySubCommand());
	}

	@Override
	public void commandProcess(SlashCommandInteractionEvent event)
	{
		subCommands.get(event.getSubcommandName()).commandProcess(event);
	}

	/**
	 * {@code GetSubCommand} is a class that handles one of the sub commands of {@code /lottery} command, which is
	 * {@code /lottery get}.
	 *
	 * @since 1.6
	 * @author Alex Cai
	 */
	private static class GetSubCommand implements ICommand
	{
		@Override
		public void commandProcess(SlashCommandInteractionEvent event)
		{
			User user = event.getUser();

			User target = event.getOption("target", CommonFunctions.getAsUser);
			if (target == null) //沒有填 預設是自己
				target = user;
			else if (target.isBot() || target.isSystem())
			{
				event.reply(JsonHandle.getStringFromJsonKey(user.getIdLong(), "lottery.get.invalid_get")).queue();
				return;
			}

			CommandBlocksHandle.LotteryData targetLotteryData = CommandBlocksHandle.getLotteryData(target.getIdLong());
			if (!Boolean.TRUE.equals(event.getOption("display_detail", CommonFunctions.getAsBoolean))) //不顯示細節 null代表false 所以不使用Boolean.FALSE.equals
			{
				event.reply(JsonHandle.getStringFromJsonKey(user.getIdLong(), "lottery.get.query")
									.formatted(targetLotteryData.getName(), targetLotteryData.getBlocks())).queue();
				return;
			}
			int won = targetLotteryData.getWon();
			int lost = targetLotteryData.getLost();
			int showHandWon = targetLotteryData.getShowHandWon();
			int showHandLost = targetLotteryData.getShowHandLost();
			event.reply(JsonHandle.getStringFromJsonKey(user.getIdLong(), "lottery.get.query_detail")
								.formatted(
										targetLotteryData.getName(), targetLotteryData.getBlocks(),
										won + lost, won, lost,
										showHandWon + showHandLost, showHandWon, showHandLost)).queue();
		}
	}

	/**
	 * {@code BetSubCommand} is a class that handles one of the sub commands of {@code /lottery} command, which is
	 * {@code /lottery bet}.
	 *
	 * @since 1.6
	 * @author Alex Cai
	 */
	private static class BetSubCommand implements ICommand
	{
		private final Random random = new Random(); //不使用Algorithm.chance
		private final Pattern numberRegex = Pattern.compile("\\d{1,18}"); //防止輸入超過Long.MAX_VALUE
		private final Pattern percentRegex = Pattern.compile("\\d{1,4}%"); //防止輸入超過Short.MAX_VALUE
		private static final long MAXIMUM = 1000000L;

		@Override
		public void commandProcess(SlashCommandInteractionEvent event)
		{
			long userID = event.getUser().getIdLong();
			CommandBlocksHandle.LotteryData lotteryData = CommandBlocksHandle.getLotteryData(userID);
			long nowHave = lotteryData.getBlocks();
			String betString = event.getOption("bet", CommonFunctions.getAsString);

			if (betString == null) //不帶參數
			{
				event.reply("Impossible, this is required!").queue();
				return;
			}

			long bet;

			if (numberRegex.matcher(betString).matches()) //賭數字
				bet = Long.parseLong(betString);
			else if (percentRegex.matcher(betString).matches()) //賭%數
			{
				short percentage = Short.parseShort(betString.substring(0, betString.length() - 1));
				if (percentage > 100) //超過100%
				{
					event.reply(JsonHandle.getStringFromJsonKey(userID, "lottery.bet.wrong_percent").formatted(percentage)).queue();
					return;
				}
				bet = nowHave * percentage / 100;
			}
			else //都不是
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "lottery.bet.wrong_argument")).queue();
				return;
			}

			if (bet == 0L) //不能賭0
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "lottery.bet.wrong_argument")).queue();
				return;
			}

			if (bet > MAXIMUM) //限紅
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "lottery.bet.too_much").formatted(bet, MAXIMUM)).queue();
				return;
			}

			if (nowHave < bet) //如果現有的比要賭的還少
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "lottery.bet.not_enough").formatted(bet, nowHave)).queue();
				return;
			}

			long afterBet;
			String result;
			boolean win = random.nextBoolean();

			if (win) //賭贏
			{
				afterBet = Algorithm.safeAdd(nowHave, bet);
				result = JsonHandle.getStringFromJsonKey(userID, "lottery.bet.win");
			}
			else //賭輸
			{
				afterBet = nowHave - bet;
				result = JsonHandle.getStringFromJsonKey(userID, "lottery.bet.lose");
			}

			String replyMessage = JsonHandle.getStringFromJsonKey(userID, "lottery.bet.result").formatted(bet, result, afterBet);
			boolean showHand = bet == nowHave; //梭哈
			if (showHand)
				replyMessage += "\n" + (win ? "https://www.youtube.com/watch?v=RbMjxQEZ1IQ" : JsonHandle.getStringFromJsonKey(userID, "lottery.bet.play_with_your_limit"));
			event.reply(replyMessage).queue(); //盡快回覆比較好

			lotteryData.addGame(win, showHand); //紀錄勝場和是否梭哈
			lotteryData.setBlocks(afterBet); //設定方塊
		}
	}

	/**
	 * {@code RankingSubCommand} is a class that handles one of the sub commands of {@code /lottery} command, which is
	 * {@code /lottery ranking}.
	 *
	 * @since 1.6
	 * @author Alex Cai
	 */
	private static class RankingSubCommand implements ICommand
	{
		private List<CommandBlocksHandle.LotteryData> forSort; //需要排序的list
		private String lastReply; //上一次回覆過的字串
		private int lastPage = -1; //上一次查看的頁面
		private long lastUser = -1L; //上一次使用指令的使用者

		@Override
		public void commandProcess(SlashCommandInteractionEvent event)
		{
			long userID = event.getUser().getIdLong();
			boolean sameUser = userID == lastUser;
			lastUser = userID;

			Integer pageBox = event.getOption("page", CommonFunctions.getAsInt);
			int page = pageBox != null ? pageBox : 1; //page從1開始

			//假設總共有27位使用者 (27 - 1) / 10 + 1 = 3 總共有3頁
			int maxPage = (CommandBlocksHandle.size() - 1) / 10 + 1;
			if (page > maxPage) //超出範圍
				page = maxPage; //同上例子 就改成顯示第3頁
			else if (page < 0) //-1 = 最後一頁, -2 = 倒數第二頁 負太多就變第一頁
				page = (-page < maxPage) ? maxPage + page + 1 : 1;
			else if (page == 0)
				page = 1;

			if (!CommandBlocksHandle.changed) //指令方塊 距離上一次排序 沒有任何變動
			{
				if (page != lastPage || !sameUser) //有換頁 或 不是同一位使用者
					lastReply = replyString(userID, page, maxPage); //重新建立字串
				event.reply(lastReply).queue();
				return; //省略排序
			}

			forSort = CommandBlocksHandle.toArrayList();

			//排序
			forSort.sort((user1, user2) -> Long.compare(user2.getBlocks(), user1.getBlocks())); //方塊較多的在前面 方塊較少的在後面

			event.reply(lastReply = replyString(userID, page, maxPage)).queue();
			CommandBlocksHandle.changed = false; //已經排序過了
			lastPage = page; //換過頁了
		}

		private final StringBuilder rankBuilder = new StringBuilder();

		/**
		 * Builds a page in the ranking list of command blocks.
		 *
		 * @param userID The ID of the user who used the command.
		 * @param page The page that the command user want to check.
		 * @param maxPage Maximum of pages that the ranking list has.
		 * @return A page of the ranking list into a single string.
		 * @since 1.6
		 * @author Alex Cai
		 */
		private String replyString(long userID, int page, int maxPage)
		{
			//page 從1開始
			int startElement = (page - 1) * 10; //開始的那個元素
			int endElement = startElement + 10; //結束的那個元素
			if (endElement > forSort.size()) //結束的那個元素比list總長還長
				endElement = forSort.size();

			List<CommandBlocksHandle.LotteryData> ranking = forSort.subList(startElement, endElement); //要查看的那一頁
			CommandBlocksHandle.LotteryData myData = CommandBlocksHandle.getLotteryData(userID);
			long blocks = myData.getBlocks(); //本使用者擁有的方塊數

			Guild cartoland = Cartoland.getJDA().getGuildById(IDs.CARTOLAND_SERVER_ID);
			rankBuilder.setLength(0);
			rankBuilder.append("```ansi\n")
					.append(JsonHandle.getStringFromJsonKey(userID, "lottery.ranking.title").formatted(cartoland != null ? cartoland.getName() : ""))
					.append("\n--------------------\n")
					.append(JsonHandle.getStringFromJsonKey(userID, "lottery.ranking.my_rank").formatted(forSortBinarySearch(blocks), blocks))
					.append("\n\n");

			for (int i = 0, add = page * 10 - 9, rankingSize = ranking.size(); i < rankingSize; i++) //add = (page - 1) * 10 + 1
			{
				CommandBlocksHandle.LotteryData rank = ranking.get(i);
				rankBuilder.append("[\u001B[36m")
						.append(String.format("%03d", add + i))
						.append("\u001B[0m]\t")
						.append(rank.getName())
						.append(": \u001B[36m")
						.append(String.format("%,d", rank.getBlocks()))
						.append("\u001B[0m\n");
			}

			return rankBuilder.append("\n--------------------\n")
					.append(page)
					.append(" / ")
					.append(maxPage)
					.append("\n```")
					.toString();
		}

		/**
		 * Use binary search to find the index of the user that has these blocks in the {@link #forSort} list, in order to find
		 * the ranking of a user. These code was stole... was <i>"borrowed"</i> from {@link java.util.Collections#binarySearch(List, Object)}
		 *
		 * @param blocks The number of blocks that are used to match in the {@link #forSort} list.
		 * @return The index of the user that has these blocks in the {@link #forSort} list and add 1, because though an
		 * array is 0-indexed, but the ranking that are going to display should be 1-indexed.
		 * @since 2.0
		 * @author Alex Cai
		 */
		private int forSortBinarySearch(long blocks)
		{
			long midValue;
			for (int low = 0, middle, high = forSort.size() - 1; low <= high;)
			{
				middle = (low + high) >>> 1;
				midValue = forSort.get(middle).getBlocks();

				if (midValue < blocks)
					high = middle - 1;
				else if (midValue > blocks)
					low = middle + 1;
				else
					return middle + 1;
			}
			return 0;
		}
	}

	/**
	 * {@code DailySubCommand} is a class that handles one of the sub commands of {@code /lottery} command, which is
	 * {@code /lottery daily}.
	 *
	 * @since 2.1
	 * @author Alex Cai
	 */
	private static class DailySubCommand implements ICommand
	{
		private final int[] until = { 0,0,0 };
		private final boolean[] bonus = { false,false,false };
		private final StringBuilder builder = new StringBuilder();

		@Override
		public void commandProcess(SlashCommandInteractionEvent event)
		{
			long userID = event.getUser().getIdLong();
			CommandBlocksHandle.LotteryData lotteryData = CommandBlocksHandle.getLotteryData(userID); //獲取指令方塊資料
			if (!lotteryData.tryClaimDaily(until)) //嘗試daily失敗了
			{
				event.reply(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.not_yet")
									.formatted(CommandBlocksHandle.LotteryData.DAILY, until[0], until[1], until[2])).setEphemeral(true).queue();
				return;
			}

			builder.setLength(0);
			builder.append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.claimed").formatted(CommandBlocksHandle.LotteryData.DAILY));

			int streak = lotteryData.getStreak(); //連續領取天數
			builder.append('\n').append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.streak").formatted(streak));
			if (lotteryData.tryClaimBonus(bonus)) //有額外
			{
				if (bonus[0]) //週
					builder.append('\n').append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.weekly").formatted(streak / 7))
							.append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.bonus").formatted(CommandBlocksHandle.LotteryData.WEEKLY));
				if (bonus[1]) //月
					builder.append('\n').append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.monthly").formatted(streak / 30))
							.append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.bonus").formatted(CommandBlocksHandle.LotteryData.MONTHLY));
				if (bonus[2]) //年
					builder.append('\n').append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.yearly").formatted(streak / 365))
							.append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.bonus").formatted(CommandBlocksHandle.LotteryData.YEARLY));
			}

			builder.append('\n').append(JsonHandle.getStringFromJsonKey(userID, "lottery.daily.now_have").formatted(lotteryData.getBlocks()));
			event.reply(builder.toString()).queue();
		}
	}
}