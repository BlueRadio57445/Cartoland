package cartoland.mini_games;

import cartoland.utilities.Algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * {@code OneATwoBGame} is the backend of the 1A2B game, it can process the entire game with all fields and methods.
 * This game will generate a {@link #ANSWER_LENGTH} digits long number. Leading 0 is allowed. Players need to guess
 * the number, if the place of a digit is right, that is an A; if the digit is right but the place is wrong, that is a B.
 *
 * @since 1.1
 * @see cartoland.commands.OneATwoBCommand The frontend of the 1A2B game.
 * @author Alex Cai
 */
public class OneATwoBGame implements IMiniGame
{
	public static final int ANSWER_LENGTH = 4;

	private final int[] answer = new int[ANSWER_LENGTH];
	private final boolean[] metBefore = new boolean[10];
	private final boolean[] inAnswer = { false,false,false,false,false,false,false,false,false,false };
	private final Instant begin;
	private int guesses = 0;

	public OneATwoBGame()
	{
		int[] zeroToNine = { 0,1,2,3,4,5,6,7,8,9 };
		Algorithm.shuffle(zeroToNine); //洗牌0 ~ 9

		for (int i = 0; i < ANSWER_LENGTH; i++) //產生答案
		{
			answer[i] = zeroToNine[i];
			inAnswer[answer[i]] = true;
		}

		begin = Instant.now();
	}

	@Override
	public String gameName()
	{
		return "1A2B";
	}

	private final int[] ab = new int[2];

	public int[] calculateAAndB(int input)
	{
		guesses++;
		ab[0] = ab[1] = 0;
		Arrays.fill(metBefore, false);

		for (int i = ANSWER_LENGTH - 1, digit; i >= 0; i--) //從後面檢測回來
		{
			digit = input % 10;
			if (metBefore[digit]) //遇過這個數字了
				return null;

			metBefore[digit] = true;

			if (answer[i] == digit)
				ab[0]++;
			else if (inAnswer[digit])
				ab[1]++;

			input /= 10;
		}

		return ab;
	}
	//計時結束
	public long getTimePassed()
	{
		return Duration.between(begin, Instant.now()).toSeconds();
	}

	public int getGuesses()
	{
		return guesses;
	}
}