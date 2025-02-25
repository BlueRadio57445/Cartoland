package cartoland;

import cartoland.events.*;
import cartoland.utilities.AddCommands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

/**
 * {@code Cartoland} is the class that has the {@link #main} method, which is the entry point of the entire program. All the
 * settings of JDA was done here.
 *
 * @since 1.0
 * @author Alex Cai
 */
public class Cartoland
{
	private static JDA jda;
	public static JDA getJDA()
	{
		return jda;
	}

	/**
	 * The entry point of the entire program. JDA was built here. Commands was register at here also, but
	 * source code of commands are not in here. Instead, they are in {@link AddCommands}.
	 *
	 * @param args Command line arguments. The first argument is the token of the bot.
	 * @throws InterruptedException If this thread is interrupted while waiting.
	 * @throws IllegalStateException If JDA is shutdown during the wait period.
	 * @since 1.0
	 * @author Alex Cai
	 */
	public static void main(String[] args) throws InterruptedException
	{
		if (args.length < 1)
			return;

		jda = JDABuilder.createDefault(args[0])
				.addEventListeners(
						new BotOnlineOffline(), //當機器人上下線的時候
						new MessageEvent(), //當有任何訊息
						new AddReaction(), //當有人為訊息新增反應
						new CommandUsage(), //當有人使用指令
						new AutoComplete(), //當指令需要自動補完
						new ContextMenu(), //當有人使用右鍵功能
						new NewMember(), //當有人加入、離開，或獲得會員身分組
						new ThreadEvent(), //和討論串有關
						new UserChangeName(), //當有人改名(不是暱稱)
						new ClickedButton(), //當有人按按鈕
						new ReceiveModal()) //當有人使用Modal時
				.enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.setActivity(Activity.playing("Do /help for more information")) //正在玩
				.build();

		jda.updateCommands().addCommands(AddCommands.commands).queue(); //添加指令 裡面的程式簡直是一團亂 能跑就行

		jda.awaitReady();
	}
}