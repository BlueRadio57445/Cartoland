package cartoland.events;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

/**
 * {@code ReceiveModal} is a listener that triggers when a user interact with a modal. This class was registered in
 * {@link cartoland.Cartoland#main}, with the build of JDA.
 *
 * @since 2.0
 * @author Alex Cai
 */
public class ReceiveModal extends ListenerAdapter
{
	public static final String NEW_TITLE_TEXT = "new_title";
	public static final String NEW_TITLE_MODAL_ID = "new_title";

	@Override
	public void onModalInteraction(ModalInteractionEvent event)
	{
		if (event.getModalId().equals(NEW_TITLE_MODAL_ID))
		{
			ModalMapping newTitle = event.getValue(NEW_TITLE_TEXT);
			if (newTitle == null)
			{
				event.reply("Impossible, this is required!").queue();
				return;
			}

			String newTitleString = newTitle.getAsString(); //新標題
			event.getChannel().asThreadChannel().getManager().setName(newTitleString).queue();
			event.reply(event.getUser().getEffectiveName() + " changed thread title to " + newTitleString + ".").queue();
		}
	}
}